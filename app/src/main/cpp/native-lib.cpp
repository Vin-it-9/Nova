#include <jni.h>

#include <atomic>
#include <chrono>
#include <mutex>
#include <string>
#include <thread>

namespace {
constexpr char kResponseSeparator = '\x1F';

JavaVM* gJvm = nullptr;
std::mutex gWorkerMutex;
std::atomic<bool> gWorkerRunning{false};
std::thread gWorkerThread;

std::string BuildResponse(
        bool success,
        const std::string& payload,
        const std::string& error = "") {
    return std::string(success ? "ok" : "err") + kResponseSeparator + payload + kResponseSeparator + error;
}

std::string JStringToString(JNIEnv* env, const jstring value) {
    if (value == nullptr) {
        return "";
    }

    const char* chars = env->GetStringUTFChars(value, nullptr);
    std::string result(chars == nullptr ? "" : chars);
    if (chars != nullptr) {
        env->ReleaseStringUTFChars(value, chars);
    }
    return result;
}

void DispatchNativeCallback(const std::string& type, const std::string& payload) {
    if (gJvm == nullptr) {
        return;
    }

    JNIEnv* env = nullptr;
    bool didAttach = false;
    if (gJvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        if (gJvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        didAttach = true;
    }

    jclass bridgeClass = env->FindClass("com/nova/ai/assistant/nativecore/NativeBridge");
    if (bridgeClass != nullptr) {
        jmethodID callbackMethod = env->GetStaticMethodID(
                bridgeClass,
                "onNativeCallback",
                "(Ljava/lang/String;Ljava/lang/String;)V");
        if (callbackMethod != nullptr) {
            jstring jType = env->NewStringUTF(type.c_str());
            jstring jPayload = env->NewStringUTF(payload.c_str());
            env->CallStaticVoidMethod(bridgeClass, callbackMethod, jType, jPayload);
            env->DeleteLocalRef(jType);
            env->DeleteLocalRef(jPayload);
        }
        env->DeleteLocalRef(bridgeClass);
    }

    if (didAttach) {
        gJvm->DetachCurrentThread();
    }
}

void StartWorkerIfNeeded() {
    std::lock_guard<std::mutex> guard(gWorkerMutex);
    if (gWorkerRunning.load(std::memory_order_acquire)) {
        return;
    }

    gWorkerRunning.store(true, std::memory_order_release);
    gWorkerThread = std::thread([]() {
        while (gWorkerRunning.load(std::memory_order_acquire)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
    });
}

void StopWorkerIfRunning() {
    std::lock_guard<std::mutex> guard(gWorkerMutex);
    if (!gWorkerRunning.load(std::memory_order_acquire)) {
        return;
    }

    gWorkerRunning.store(false, std::memory_order_release);
    if (gWorkerThread.joinable()) {
        gWorkerThread.join();
    }
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nova_ai_assistant_nativecore_NativeBridge_nativeRuntimePingInternal(
        JNIEnv*,
        jobject) {
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nova_ai_assistant_nativecore_NativeBridge_nativeHandleRequest(
        JNIEnv* env,
        jobject,
        jstring action,
        jstring payload) {
    const std::string actionValue = JStringToString(env, action);
    const std::string payloadValue = JStringToString(env, payload);

    if (actionValue == "lifecycle:init") {
        StartWorkerIfNeeded();
        DispatchNativeCallback("native.lifecycle", "worker-started");
        return env->NewStringUTF(BuildResponse(true, "initialized").c_str());
    }

    if (actionValue == "lifecycle:shutdown") {
        StopWorkerIfRunning();
        DispatchNativeCallback("native.lifecycle", "worker-stopped");
        return env->NewStringUTF(BuildResponse(true, "stopped").c_str());
    }

    if (actionValue == "pipeline:audio") {
        return env->NewStringUTF(BuildResponse(true, "audio-frame-accepted").c_str());
    }

    if (actionValue == "pipeline:inference") {
        return env->NewStringUTF(BuildResponse(true, "inference-request-queued").c_str());
    }

    if (actionValue.empty()) {
        return env->NewStringUTF(BuildResponse(false, "", "empty-action").c_str());
    }

    return env->NewStringUTF(BuildResponse(true, payloadValue).c_str());
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void*) {
    gJvm = vm;
    return JNI_VERSION_1_6;
}

package com.nova.ai.assistant.nativecore

import com.nova.ai.assistant.core.runtime.NativeRuntimeContract
import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object NativeBridge : NativeRuntimeContract {
    private const val RESPONSE_SEPARATOR = '\u001F'
    private val requestMutex = Mutex()

    init {
        System.loadLibrary("assistant")
    }

    fun nativeRuntimePing(): Boolean = nativeRuntimePingInternal()

    override suspend fun ping(): Boolean = withContext(RuntimeThreadingModel.nativeDispatcher) {
        nativeRuntimePing()
    }

    override suspend fun request(request: NativeRuntimeRequest): NativeRuntimeResponse =
        withContext(RuntimeThreadingModel.nativeDispatcher) {
            requestMutex.withLock {
                val encoded = request.encode()
                val raw = nativeHandleRequest(encoded.first, encoded.second)
                decodeResponse(raw)
            }
        }

    @JvmStatic
    fun onNativeCallback(type: String, payload: String) {
        NativeCallbackChannel.tryPublish(
            NativeCallbackChannel.NativeCallbackEvent(
                type = type,
                payload = payload
            )
        )
    }

    private fun NativeRuntimeRequest.encode(): Pair<String, String> = when (this) {
        is NativeRuntimeRequest.Lifecycle -> "lifecycle:$operation" to payload
        is NativeRuntimeRequest.AudioPipeline -> "pipeline:audio" to payload
        is NativeRuntimeRequest.Inference -> "pipeline:inference" to payload
        is NativeRuntimeRequest.Custom -> action to payload
    }

    private fun decodeResponse(raw: String): NativeRuntimeResponse {
        val parts = raw.split(RESPONSE_SEPARATOR, limit = 3)
        val success = parts.getOrNull(0) == "ok"
        return NativeRuntimeResponse(
            success = success,
            payload = parts.getOrNull(1).orEmpty(),
            error = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
        )
    }

    private external fun nativeRuntimePingInternal(): Boolean
    private external fun nativeHandleRequest(action: String, payload: String): String

    sealed interface NativeRuntimeRequest {
        data class Lifecycle(
            val operation: String,
            val payload: String = ""
        ) : NativeRuntimeRequest

        data class AudioPipeline(
            val payload: String
        ) : NativeRuntimeRequest

        data class Inference(
            val payload: String
        ) : NativeRuntimeRequest

        data class Custom(
            val action: String,
            val payload: String
        ) : NativeRuntimeRequest
    }

    data class NativeRuntimeResponse(
        val success: Boolean,
        val payload: String,
        val error: String? = null,
        val timestampNanos: Long = System.nanoTime()
    )
}

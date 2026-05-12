package com.nova.ai.assistant.core.runtime

import com.nova.ai.assistant.core.AssistantState
import com.nova.ai.assistant.nativecore.NativeBridge
import com.nova.ai.assistant.voice.pipeline.SpeechFrame
import com.nova.ai.assistant.voice.pipeline.VoicePipelineState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AssistantRuntimeContract {
    val assistantState: StateFlow<AssistantState>
    val runtimeEvents: Flow<RuntimeEvent>

    suspend fun startRuntime()
    suspend fun stopRuntime()
}

interface ServiceRuntimeContract {
    val serviceState: StateFlow<RuntimeServiceState>

    suspend fun startForegroundRuntime()
    suspend fun bindRuntime()
    suspend fun unbindRuntime()
    suspend fun stopForegroundRuntime()
}

interface NativeRuntimeContract {
    suspend fun ping(): Boolean
    suspend fun request(request: NativeBridge.NativeRuntimeRequest): NativeBridge.NativeRuntimeResponse
}

interface VoiceRuntimeContract {
    val pipelineState: StateFlow<VoicePipelineState>

    suspend fun pushFrame(frame: SpeechFrame)
    suspend fun resetPipeline()
}

interface IntelligenceRuntimeContract {
    suspend fun onRuntimeEvent(event: RuntimeEvent)
}

interface UiRuntimeContract {
    val assistantState: StateFlow<AssistantState>

    fun observeRuntimeEvents(): Flow<RuntimeEvent>
    suspend fun onRuntimeEvent(event: RuntimeEvent)
}

enum class RuntimeServiceState {
    STOPPED,
    STARTING,
    RUNNING,
    BINDING,
    BOUND,
    STOPPING,
    FAILED
}

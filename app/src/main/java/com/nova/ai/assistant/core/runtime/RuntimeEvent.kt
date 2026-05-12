package com.nova.ai.assistant.core.runtime

import com.nova.ai.assistant.core.AssistantState

sealed interface RuntimeEvent {
    val createdAtNanos: Long

    data class Lifecycle(
        val state: LifecycleState,
        val details: String? = null,
        override val createdAtNanos: Long = System.nanoTime()
    ) : RuntimeEvent

    data class Audio(
        val frameCount: Int,
        val sampleRateHz: Int,
        val source: String,
        override val createdAtNanos: Long = System.nanoTime()
    ) : RuntimeEvent

    data class Wake(
        val activated: Boolean,
        val confidence: Float? = null,
        override val createdAtNanos: Long = System.nanoTime()
    ) : RuntimeEvent

    data class Inference(
        val phase: InferencePhase,
        val payload: String,
        override val createdAtNanos: Long = System.nanoTime()
    ) : RuntimeEvent

    data class Callback(
        val channel: String,
        val payload: String,
        override val createdAtNanos: Long = System.nanoTime()
    ) : RuntimeEvent

    data class UiSync(
        val state: AssistantState,
        override val createdAtNanos: Long = System.nanoTime()
    ) : RuntimeEvent
}

enum class LifecycleState {
    RUNTIME_STARTING,
    NATIVE_READY,
    SERVICE_STARTING,
    SERVICE_READY,
    SERVICE_RUNNING,
    SERVICE_BINDING,
    SERVICE_BOUND,
    SERVICE_UNBOUND,
    VOICE_READY,
    INTELLIGENCE_READY,
    RUNTIME_READY,
    SERVICE_STOPPING,
    SERVICE_STOPPED,
    SERVICE_DISCONNECTED,
    RUNTIME_STOPPED,
    ERROR
}

enum class InferencePhase {
    REQUESTED,
    STARTED,
    COMPLETED,
    FAILED
}

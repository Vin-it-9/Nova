package com.nova.ai.assistant.voice.pipeline

enum class VoicePipelineState {
    IDLE,
    CAPTURING,
    VAD_ACTIVE,
    WAKE_LISTENING,
    TRANSCRIBING,
    ERROR
}

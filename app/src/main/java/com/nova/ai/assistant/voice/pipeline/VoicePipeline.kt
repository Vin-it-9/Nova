package com.nova.ai.assistant.voice.pipeline

import kotlinx.coroutines.flow.StateFlow

interface VoicePipeline {
    val state: StateFlow<VoicePipelineState>

    suspend fun start()
    suspend fun stop()
    suspend fun submit(frame: SpeechFrame)
    suspend fun reset()
}

interface VoiceActivityDetector {
    suspend fun isSpeech(frame: SpeechFrame): Boolean
}

interface WakeWordPipeline {
    suspend fun detect(frame: SpeechFrame): Boolean
    suspend fun reset()
}

interface TranscriptionPipeline {
    suspend fun transcribe(frame: SpeechFrame): String?
    suspend fun reset()
}

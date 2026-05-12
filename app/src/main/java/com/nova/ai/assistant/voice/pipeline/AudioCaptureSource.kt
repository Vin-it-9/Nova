package com.nova.ai.assistant.voice.pipeline

interface AudioCaptureSource {
    val sampleRateHz: Int
    val channelCount: Int

    suspend fun start(onFrame: suspend (SpeechFrame) -> Unit)
    suspend fun stop()
}

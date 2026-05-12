package com.nova.ai.assistant.voice.pipeline

data class SpeechFrame(
    val samples: ShortArray,
    val sampleRateHz: Int,
    val channelCount: Int,
    val timestampNanos: Long = System.nanoTime()
) {
    val frameCount: Int
        get() = samples.size / channelCount.coerceAtLeast(1)
}

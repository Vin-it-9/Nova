package com.nova.ai.assistant.voice.pipeline

interface AudioFrameBuffer {
    val capacityFrames: Int
    val sizeFrames: Int

    fun push(frame: SpeechFrame): Boolean
    fun pop(): SpeechFrame?
    fun clear()
}

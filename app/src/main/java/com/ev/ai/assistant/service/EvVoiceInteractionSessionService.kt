package com.ev.ai.assistant.service

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * File B: EvVoiceInteractionSessionService
 *
 * System service invoked by the Android framework to instantiate voice interaction sessions.
 */
class EvVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return EvVoiceInteractionSession(this)
    }
}

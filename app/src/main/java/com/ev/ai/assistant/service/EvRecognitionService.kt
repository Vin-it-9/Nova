package com.ev.ai.assistant.service

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService

/**
 * EvRecognitionService
 *
 * Mandatory RecognitionService required by Android's VoiceInteractionManagerService.
 * Without this service registered and linked in voice_interaction_service.xml, Android's
 * system parser marks the assistant as invalid and will NOT list it under
 * "Default digital assistant app" in Android Settings.
 */
class EvRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // System speech recognition delegate callback
        listener?.readyForSpeech(Bundle())
    }

    override fun onCancel(listener: Callback?) {
        // Handle cancel signal
    }

    override fun onStopListening(listener: Callback?) {
        // Handle stop signal
        listener?.endOfSpeech()
    }
}

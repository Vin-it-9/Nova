package com.ev.ai.assistant.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.ev.ai.assistant.data.preferences.EvPreferences
import java.util.Locale

/**
 * File A: EvVoiceInteractionService
 *
 * System-level VoiceInteractionService. Manages the assistant lifecycle, active system
 * verification status, and real background wake-up word detection loop.
 */
class EvVoiceInteractionService : VoiceInteractionService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var isListeningLoopActive = false

    override fun onReady() {
        super.onReady()
        instance = this
        Log.i(TAG, "EvVoiceInteractionService ready. Active assistant: ${isDefaultAssistantActive()}")
    }

    override fun onShutdown() {
        super.onShutdown()
        instance = null
        mainHandler.post {
            stopWakeWordListening()
        }
    }

    /**
     * Checks if EV is actively selected as the device's Default Digital Assistant.
     */
    fun isDefaultAssistantActive(): Boolean {
        return isActiveService(this, ComponentName(this, EvVoiceInteractionService::class.java))
    }

    /**
     * Triggers the voice interaction session overlay.
     */
    fun startSession(args: Bundle = Bundle()) {
        try {
            showSession(args, VoiceInteractionSession.SHOW_WITH_ASSIST)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice interaction session: ${e.message}", e)
        }
    }

    /**
     * Safe wake-word listener without runaway mic recursion.
     */
    fun startWakeWordListening() {
        if (isListeningLoopActive) return
        val prefs = EvPreferences.getInstance(applicationContext)
        if (!prefs.isVoiceWakeUpEnabled) return

        try {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) return

            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        Log.d(TAG, "Wake word recognizer finished with code: $error")
                        isListeningLoopActive = false
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
                        val targetWord = prefs.wakeUpWord.lowercase(Locale.ROOT)

                        if (text.contains(targetWord) || text.contains("ev") || text.contains("hey ev") || text.contains("e.v.")) {
                            Log.i(TAG, "Wake word detected: $text -> launching session")
                            startSession(Bundle().apply {
                                putString(EvVoiceInteractionSession.KEY_SESSION_PROMPT, "Listening...")
                            })
                        }
                        isListeningLoopActive = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
                        val targetWord = prefs.wakeUpWord.lowercase(Locale.ROOT)

                        if (text.contains(targetWord) || text.contains("ev") || text.contains("hey ev") || text.contains("e.v.")) {
                            Log.i(TAG, "Partial wake word detected: $text -> launching session")
                            startSession(Bundle().apply {
                                putString(EvVoiceInteractionSession.KEY_SESSION_PROMPT, "Listening...")
                            })
                            stopWakeWordListening()
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            isListeningLoopActive = true
            restartListening()
        } catch (e: Exception) {
            Log.e(TAG, "Could not start wake word listening: ${e.message}", e)
            isListeningLoopActive = false
        }
    }

    private fun restartListening() {
        if (!isListeningLoopActive) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.w(TAG, "restartListening failed: ${e.message}")
            isListeningLoopActive = false
        }
    }

    fun stopWakeWordListening() {
        isListeningLoopActive = false
        try {
            recognizer?.destroy()
            recognizer = null
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "EvVoiceService"

        @Volatile
        var instance: EvVoiceInteractionService? = null

        /**
         * Global helper to check if EV is currently configured as the system default assistant.
         */
        fun isEvDefaultAssistant(context: Context): Boolean {
            return VoiceInteractionService.isActiveService(
                context,
                ComponentName(context, EvVoiceInteractionService::class.java)
            )
        }

        /**
         * Directs user to the system "Default digital assistant app" settings page.
         */
        fun openAssistantSettings(context: Context) {
            val intents = listOf(
                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                Intent(Settings.ACTION_SETTINGS)
            )
            for (intent in intents) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {}
            }
        }

        /**
         * Triggers wake-up overlay directly if service instance is active, or via showSession.
         */
        fun triggerWakeUp(context: Context, prompt: String = "Listening..."): Boolean {
            val active = instance
            if (active != null) {
                active.startSession(Bundle().apply {
                    putString(EvVoiceInteractionSession.KEY_SESSION_PROMPT, prompt)
                })
                return true
            }
            return false
        }
    }
}

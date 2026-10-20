package com.ev.ai.assistant.nlu

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Text-to-Speech (TTS) synthesizer for EV voice responses.
 *
 * Picks the highest-quality voice available instead of whatever the engine defaults
 * to. Many devices ship a low-quality offline voice as the default, which sounds
 * robotic and "cheap"; preferring a network voice (and a natural speech rate) makes
 * replies sound far more professional. Falls back gracefully to the default voice
 * when no better one exists or the device is offline.
 */
class EvTtsSpeaker(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureVoice()
                isInitialized = true
                pendingText?.let { text ->
                    speak(text)
                    pendingText = null
                }
            } else {
                Log.w(TAG, "TTS Initialization failed: $status")
            }
        }
    }

    /**
     * Chooses the best available voice and tunes rate/pitch for a natural, human
     * delivery. Preference order: network (highest fidelity) Indian-English, then
     * any high-quality Indian-English, then US/GB English, then the default voice.
     */
    private fun configureVoice() {
        val engine = tts ?: return

        // A slightly slower rate and a neutral pitch read as calmer and more
        // professional than the default, often-rushed system voice.
        runCatching {
            engine.setSpeechRate(DEFAULT_SPEECH_RATE)
            engine.setPitch(DEFAULT_PITCH)
        }

        val voices = runCatching { engine.voices }.getOrNull()
        if (voices.isNullOrEmpty()) return

        val chosen = pickBestVoice(voices, engine.defaultVoice)
        if (chosen != null) {
            val result = runCatching { engine.voice = chosen }.getOrDefault(TextToSpeech.ERROR)
            if (result != TextToSpeech.SUCCESS) {
                Log.w(TAG, "Failed to set voice ${chosen.name}; keeping default")
            } else {
                Log.i(TAG, "TTS voice selected: ${chosen.name} (quality=${chosen.quality})")
            }
        }
    }

    /** Ranks candidate voices so the most natural-sounding one wins. */
    private fun pickBestVoice(voices: Set<Voice>, default: Voice?): Voice? {
        val preferredLocales = listOf(
            Locale.forLanguageTag("en-IN"),
            Locale.forLanguageTag("en-GB"),
            Locale.US
        )
        val usable = voices.filter { !it.isNetworkConnectionRequired || hasNetwork() }

        fun score(v: Voice): Int {
            var s = 0
            // Locale match, in priority order.
            val idx = preferredLocales.indexOfFirst {
                v.locale != null && v.locale.language == it.language && v.locale.country == it.country
            }
            if (idx >= 0) s += (preferredLocales.size - idx) * 100
            else if (v.locale != null && v.locale.language == "en") s += 10
            // Network voices are dramatically higher fidelity than bundled ones.
            if (!v.isNetworkConnectionRequired) s += 40
            // Higher quality number = better.
            s += v.quality.coerceIn(0, 500) / 10
            return s
        }

        return usable.maxByOrNull(::score) ?: default
    }

    private fun hasNetwork(): Boolean = try {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        false
    }

    /**
     * Speaks the given text using the selected voice.
     */
    fun speak(text: String) {
        if (text.isBlank()) return

        if (!isInitialized) {
            pendingText = text
            return
        }

        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ev_speech_id_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}", e)
        }
    }

    /**
     * Stops current speech output.
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    /**
     * Releases system TTS resources.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "EvTtsSpeaker"

        /** Slightly below 1.0 reads as calmer and less "robotic". */
        private const val DEFAULT_SPEECH_RATE = 0.95f
        private const val DEFAULT_PITCH = 1.0f

        @Volatile
        private var instance: EvTtsSpeaker? = null

        fun getInstance(context: Context): EvTtsSpeaker {
            return instance ?: synchronized(this) {
                instance ?: EvTtsSpeaker(context).also { instance = it }
            }
        }
    }
}

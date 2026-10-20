package com.ev.ai.assistant.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.MainThread
import java.util.Locale

/**
 * Lifecycle-safe wrapper around Android's speech recognizer.
 *
 * Create it from the UI/main thread when possible. Public methods are dispatched to the
 * main thread if called elsewhere. Always call release() when its owner leaves the UI.
 *
 * EV phrase-biasing is only available from API 33 and is best-effort; the recognition
 * service is allowed to ignore it.
 */
internal class SpeechRecognizerController(
    context: Context,
    private val onTextChanged: (String) -> Unit,
    private val onFinalText: (String) -> Unit = {},
    private val onListeningChanged: (Boolean) -> Unit,
    private val languageTag: String = Locale.getDefault().toLanguageTag(),
    private val biasingStrings: List<String> = DEFAULT_BIASING_STRINGS,
    private val onError: (SpeechRecognizerFailure) -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val transcriptCorrection: (String) -> String = ::correctEvWakePhrase
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var released = false
    private var listening = false
    private var sessionActive = false
    private var sessionId = 0L
    private var initializationFailure: SpeechRecognizerFailure? = null

    private var sessionTimeoutRunnable: Runnable? = null
    private var lastRmsUpdateAt = 0L

    init {
        runOnMain(::initializeRecognizer)
    }

    /** Starts one session. Repeated taps during an active session are ignored. */
    fun start() = runOnMain(::startOnMain)

    /**
     * Primes the speech service so the *first* real tap starts instantly.
     *
     * Android binds the recognition service lazily on the first startListening(),
     * which costs a few hundred milliseconds of cold-start on the very first use.
     * This performs one throwaway listen and cancels it shortly after, warming the
     * service without emitting any text (sessionActive stays false, so every
     * listener callback is ignored by isCurrent()).
     */
    fun warmUp() = runOnMain(::warmUpOnMain)

    /** Stops audio capture; Android may still deliver the final result afterwards. */
    fun stop() = runOnMain(::stopOnMain)

    /** Cancels an active session and destroys the recognizer. Safe to call repeatedly. */
    fun release() = runOnMain(::releaseOnMain)

    private fun initializeRecognizer() {
        if (released || recognizer != null) return

        try {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                initializationFailure = SpeechRecognizerFailure.NOT_AVAILABLE
                return
            }

            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { created ->
                created.setRecognitionListener(createListener(sessionId))
            }
        } catch (_: SecurityException) {
            initializationFailure = SpeechRecognizerFailure.PERMISSION_DENIED
        } catch (_: RuntimeException) {
            initializationFailure = SpeechRecognizerFailure.NOT_AVAILABLE
        }
    }

    @MainThread
    private fun warmUpOnMain() {
        if (released || sessionActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (recognizer == null) {
            if (initializationFailure != SpeechRecognizerFailure.PERMISSION_DENIED) {
                initializationFailure = null
            }
            initializeRecognizer()
        }
        val speechRecognizer = recognizer ?: return

        try {
            // A throwaway listen; sessionActive stays false so callbacks are ignored.
            speechRecognizer.setRecognitionListener(createListener(sessionId))
            speechRecognizer.startListening(createRecognizerIntent())
            mainHandler.postDelayed({
                if (!sessionActive && !released) {
                    try {
                        speechRecognizer.cancel()
                    } catch (_: RuntimeException) {
                    }
                }
            }, WARM_UP_LISTEN_MS)
        } catch (_: SecurityException) {
        } catch (_: RuntimeException) {
        }
    }

    @MainThread
    private fun startOnMain() {
        if (released) {
            notifyErrorSafely(SpeechRecognizerFailure.CLIENT)
            return
        }
        if (sessionActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            updateListening(false)
            notifyErrorSafely(SpeechRecognizerFailure.PERMISSION_DENIED)
            return
        }

        if (recognizer == null) {
            if (initializationFailure != SpeechRecognizerFailure.PERMISSION_DENIED) {
                initializationFailure = null
            }
            initializeRecognizer()
        }

        val speechRecognizer = recognizer
        if (speechRecognizer == null) {
            updateListening(false)
            notifyErrorSafely(initializationFailure ?: SpeechRecognizerFailure.NOT_AVAILABLE)
            return
        }

        sessionId += 1
        val currentSession = sessionId
        sessionActive = true

        scheduleSessionTimeout(currentSession)

        try {
            speechRecognizer.setRecognitionListener(createListener(currentSession))
            speechRecognizer.startListening(createRecognizerIntent())
            updateListening(true)
        } catch (_: SecurityException) {
            finishSession(currentSession)
            notifyErrorSafely(SpeechRecognizerFailure.PERMISSION_DENIED)
        } catch (_: RuntimeException) {
            finishSession(currentSession)
            notifyErrorSafely(SpeechRecognizerFailure.CLIENT)
        }
    }

    @MainThread
    private fun stopOnMain() {
        if (released || !sessionActive) return

        try {
            recognizer?.stopListening()
            updateListening(false)
        } catch (_: RuntimeException) {
            finishSession(sessionId)
            notifyErrorSafely(SpeechRecognizerFailure.CLIENT)
        }
    }

    @MainThread
    private fun releaseOnMain() {
        if (released) return

        released = true
        sessionActive = false
        sessionId++

        cancelSessionTimeout()

        val currentRecognizer = recognizer
        recognizer = null

        try {
            currentRecognizer?.cancel()
        } catch (_: RuntimeException) {
        } finally {
            try {
                currentRecognizer?.destroy()
            } catch (_: RuntimeException) {
            }
        }

        updateListening(false)
    }

    private fun createRecognizerIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            // Prefer an Indian-English online model when available: it is far more
            // accurate for names/numbers spoken with an Indian accent.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Return the top hypothesis quickly; don't wait for a long trailing silence.
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val terms = biasingStrings
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinct()

                if (terms.isNotEmpty()) {
                    putStringArrayListExtra(
                        RecognizerIntent.EXTRA_BIASING_STRINGS,
                        ArrayList(terms)
                    )
                }
            }
        }

    private fun createListener(forSession: Long): RecognitionListener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (isCurrent(forSession)) updateListening(true)
            }

            override fun onBeginningOfSpeech() {
                if (isCurrent(forSession)) updateListening(true)
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!isCurrent(forSession) || !listening) return
                val now = System.currentTimeMillis()
                if (now - lastRmsUpdateAt < RMS_UPDATE_INTERVAL_MS) return

                lastRmsUpdateAt = now
                runCatching { onRmsChanged(rmsdB) }
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                if (isCurrent(forSession)) updateListening(false)
            }

            override fun onError(error: Int) {
                if (!isCurrent(forSession)) return

                finishSession(forSession)
                notifyErrorSafely(mapError(error))
            }

            override fun onResults(results: Bundle?) {
                if (!isCurrent(forSession)) return

                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull(String::isNotBlank)

                finishSession(forSession)

                if (text == null) {
                    notifyErrorSafely(SpeechRecognizerFailure.NO_MATCH)
                } else {
                    emitFinalText(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isCurrent(forSession)) return

                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull(String::isNotBlank)
                    ?: return

                emitText(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

    private fun emitText(rawText: String) {
        val corrected = runCatching { transcriptCorrection(rawText).trim() }
            .getOrDefault(rawText.trim())

        if (corrected.isNotEmpty()) {
            runCatching { onTextChanged(corrected) }
        }
    }

    private fun emitFinalText(rawText: String) {
        val corrected = runCatching { transcriptCorrection(rawText).trim() }
            .getOrDefault(rawText.trim())
        if (corrected.isNotEmpty()) {
            runCatching { onFinalText(corrected) }
        }
    }

    private fun isCurrent(forSession: Long): Boolean =
        !released && sessionActive && forSession == sessionId

    private fun finishSession(forSession: Long) {
        if (forSession != sessionId) return

        cancelSessionTimeout()
        sessionActive = false
        updateListening(false)
    }

    private fun scheduleSessionTimeout(session: Long) {
        cancelSessionTimeout()

        sessionTimeoutRunnable = Runnable {
            if (isCurrent(session)) {
                finishSession(session)
                notifyErrorSafely(SpeechRecognizerFailure.SPEECH_TIMEOUT)
            }
        }.also {
            mainHandler.postDelayed(it, SESSION_TIMEOUT_MS)
        }
    }

    private fun cancelSessionTimeout() {
        sessionTimeoutRunnable?.let(mainHandler::removeCallbacks)
        sessionTimeoutRunnable = null
    }

    private fun updateListening(value: Boolean) {
        if (listening == value) return

        listening = value
        runCatching { onListeningChanged(value) }
    }

    private fun notifyErrorSafely(error: SpeechRecognizerFailure) {
        runCatching { onError(error) }
    }

    private fun mapError(error: Int): SpeechRecognizerFailure = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> SpeechRecognizerFailure.AUDIO
        SpeechRecognizer.ERROR_CLIENT -> SpeechRecognizerFailure.CLIENT
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            SpeechRecognizerFailure.PERMISSION_DENIED

        SpeechRecognizer.ERROR_NETWORK -> SpeechRecognizerFailure.NETWORK
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechRecognizerFailure.NETWORK_TIMEOUT
        SpeechRecognizer.ERROR_NO_MATCH -> SpeechRecognizerFailure.NO_MATCH
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognizerFailure.BUSY
        SpeechRecognizer.ERROR_SERVER -> SpeechRecognizerFailure.SERVER
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechRecognizerFailure.SPEECH_TIMEOUT

        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> SpeechRecognizerFailure.BUSY
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> SpeechRecognizerFailure.SERVER
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> SpeechRecognizerFailure.NOT_AVAILABLE
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechRecognizerFailure.NOT_AVAILABLE

        else -> SpeechRecognizerFailure.UNKNOWN
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }

    private companion object {
        val DEFAULT_BIASING_STRINGS = listOf(
            "EV",
            "E V",
            "E.V.",
            "Hey EV",
            "Hey E V",
            "EV assistant",
            "EV AI assistant"
        )

        /**
         * Safety net for a recognition service that never sends onResults() or onError().
         * This prevents the controller from becoming permanently stuck in an active session.
         */
        private const val SESSION_TIMEOUT_MS = 15_000L

        /** Limits UI updates from onRmsChanged(). */
        private const val RMS_UPDATE_INTERVAL_MS = 80L

        /** How long the throwaway warm-up listen runs before being cancelled. */
        private const val WARM_UP_LISTEN_MS = 700L
    }
}

/** Correct only a likely opening wake phrase; don't rewrite legitimate TV requests. */
private fun correctEvWakePhrase(text: String): String {
    val value = text.trim()

    val wakePhrase = Regex(
        pattern = "^(hey|hi)\\s+(?:e\\s*v|e\\.\\s*v\\.?)(?=\\s|$|[!?.,])",
        option = RegexOption.IGNORE_CASE
    )

    val match = wakePhrase.find(value) ?: return value

    val greeting = match.groupValues[1].replaceFirstChar { it.uppercase(Locale.ROOT) }
    val suffix = value.substring(match.range.last + 1)

    return "$greeting EV$suffix"
}

internal enum class SpeechRecognizerFailure {
    NOT_AVAILABLE,
    PERMISSION_DENIED,
    AUDIO,
    CLIENT,
    NETWORK,
    NETWORK_TIMEOUT,
    NO_MATCH,
    BUSY,
    SERVER,
    SPEECH_TIMEOUT,
    UNKNOWN
}
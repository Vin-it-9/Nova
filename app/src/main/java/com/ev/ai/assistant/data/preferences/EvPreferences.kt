package com.ev.ai.assistant.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Lightweight, thread-safe, and secure global preferences store for EV Assistant.
 * Uses Android's sandboxed private storage (MODE_PRIVATE) with in-memory caching
 * and reactive Compose states for seamless UI synchronization across all screens.
 */
class EvPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Observable states for reactive Jetpack Compose integration
    val voiceWakeUpEnabledState = mutableStateOf(
        getBoolean(KEY_VOICE_WAKE_UP_ENABLED, DEFAULT_VOICE_WAKE_UP_ENABLED)
    )
    val wakeUpWordState = mutableStateOf(
        getString(KEY_WAKE_UP_WORD, DEFAULT_WAKE_UP_WORD)
    )
    val fullVoiceResponsesState = mutableStateOf(
        getBoolean(KEY_FULL_VOICE_RESPONSES, DEFAULT_FULL_VOICE_RESPONSES)
    )
    val soundFeedbackState = mutableStateOf(
        getBoolean(KEY_SOUND_FEEDBACK, DEFAULT_SOUND_FEEDBACK)
    )
    val useWhileLockedState = mutableStateOf(
        getBoolean(KEY_USE_WHILE_LOCKED, DEFAULT_USE_WHILE_LOCKED)
    )
    val newsAndOffersState = mutableStateOf(
        getBoolean(KEY_NEWS_AND_OFFERS, DEFAULT_NEWS_AND_OFFERS)
    )

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_VOICE_WAKE_UP_ENABLED ->
                voiceWakeUpEnabledState.value = getBoolean(KEY_VOICE_WAKE_UP_ENABLED, DEFAULT_VOICE_WAKE_UP_ENABLED)
            KEY_WAKE_UP_WORD ->
                wakeUpWordState.value = getString(KEY_WAKE_UP_WORD, DEFAULT_WAKE_UP_WORD)
            KEY_FULL_VOICE_RESPONSES ->
                fullVoiceResponsesState.value = getBoolean(KEY_FULL_VOICE_RESPONSES, DEFAULT_FULL_VOICE_RESPONSES)
            KEY_SOUND_FEEDBACK ->
                soundFeedbackState.value = getBoolean(KEY_SOUND_FEEDBACK, DEFAULT_SOUND_FEEDBACK)
            KEY_USE_WHILE_LOCKED ->
                useWhileLockedState.value = getBoolean(KEY_USE_WHILE_LOCKED, DEFAULT_USE_WHILE_LOCKED)
            KEY_NEWS_AND_OFFERS ->
                newsAndOffersState.value = getBoolean(KEY_NEWS_AND_OFFERS, DEFAULT_NEWS_AND_OFFERS)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    // --- GENERIC TYPE-SAFE STORAGE (ALL DATA TYPES) ---

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        when (key) {
            KEY_VOICE_WAKE_UP_ENABLED -> voiceWakeUpEnabledState.value = value
            KEY_FULL_VOICE_RESPONSES -> fullVoiceResponsesState.value = value
            KEY_SOUND_FEEDBACK -> soundFeedbackState.value = value
            KEY_USE_WHILE_LOCKED -> useWhileLockedState.value = value
            KEY_NEWS_AND_OFFERS -> newsAndOffersState.value = value
        }
    }

    fun getString(key: String, defaultValue: String): String =
        prefs.getString(key, defaultValue) ?: defaultValue

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
        when (key) {
            KEY_WAKE_UP_WORD -> wakeUpWordState.value = value
        }
    }

    fun getInt(key: String, defaultValue: Int): Int =
        prefs.getInt(key, defaultValue)

    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long): Long =
        prefs.getLong(key, defaultValue)

    fun setLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float =
        prefs.getFloat(key, defaultValue)

    fun setFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getStringSet(key: String, defaultValue: Set<String>): Set<String> =
        prefs.getStringSet(key, defaultValue) ?: defaultValue

    fun setStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value).apply()
    }

    fun contains(key: String): Boolean =
        prefs.contains(key)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // --- CONVENIENCE PROPERTIES FOR EV ASSISTANT ---

    var isVoiceWakeUpEnabled: Boolean
        get() = voiceWakeUpEnabledState.value
        set(value) = setBoolean(KEY_VOICE_WAKE_UP_ENABLED, value)

    var wakeUpWord: String
        get() = wakeUpWordState.value
        set(value) = setString(KEY_WAKE_UP_WORD, value)

    var fullVoiceResponses: Boolean
        get() = fullVoiceResponsesState.value
        set(value) = setBoolean(KEY_FULL_VOICE_RESPONSES, value)

    var soundFeedback: Boolean
        get() = soundFeedbackState.value
        set(value) = setBoolean(KEY_SOUND_FEEDBACK, value)

    var useWhileLocked: Boolean
        get() = useWhileLockedState.value
        set(value) = setBoolean(KEY_USE_WHILE_LOCKED, value)

    var newsAndOffers: Boolean
        get() = newsAndOffersState.value
        set(value) = setBoolean(KEY_NEWS_AND_OFFERS, value)

    companion object {
        private const val PREFS_NAME = "ev_assistant_secure_prefs"

        const val KEY_VOICE_WAKE_UP_ENABLED = "pref_voice_wake_up_enabled"
        const val KEY_WAKE_UP_WORD = "pref_wake_up_word"
        const val KEY_FULL_VOICE_RESPONSES = "pref_full_voice_responses"
        const val KEY_SOUND_FEEDBACK = "pref_sound_feedback"
        const val KEY_USE_WHILE_LOCKED = "pref_use_while_locked"
        const val KEY_NEWS_AND_OFFERS = "pref_news_and_offers"

        const val DEFAULT_VOICE_WAKE_UP_ENABLED = true
        const val DEFAULT_WAKE_UP_WORD = "EV"
        const val DEFAULT_FULL_VOICE_RESPONSES = true
        const val DEFAULT_SOUND_FEEDBACK = true
        const val DEFAULT_USE_WHILE_LOCKED = true
        const val DEFAULT_NEWS_AND_OFFERS = false

        @Volatile
        private var INSTANCE: EvPreferences? = null

        fun getInstance(context: Context): EvPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EvPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Convenient Compose hook to observe and update global EV preferences.
 */
@Composable
fun rememberEvPreferences(): EvPreferences {
    val context = LocalContext.current
    return remember(context) {
        EvPreferences.getInstance(context)
    }
}

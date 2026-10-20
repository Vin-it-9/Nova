package com.ev.ai.assistant.nlu

/**
 * Represents structured intents understood by the EV assistant NLU engine.
 *
 * This is a comprehensive, extensible set of intents covering device control,
 * information, communication, productivity, media, and conversation.
 */
sealed interface EvIntent {

    // ── Device / App Control ────────────────────────────────────────────────

    data class OpenApp(
        val appName: String,
        val packageName: String? = null
    ) : EvIntent

    data class CloseApp(
        val appName: String,
        val packageName: String? = null
    ) : EvIntent

    data class SwitchApp(
        val appName: String
    ) : EvIntent

    // ── Alarms & Timers ─────────────────────────────────────────────────────

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String = "EV Alarm",
        val days: List<String> = emptyList(),
        val enabled: Boolean = true
    ) : EvIntent

    data class DeleteAlarm(
        val label: String? = null,
        val hour: Int? = null,
        val minute: Int? = null,
        val all: Boolean = false
    ) : EvIntent

    data class EditAlarm(
        val oldHour: Int,
        val oldMinute: Int,
        val newHour: Int,
        val newMinute: Int,
        val label: String? = null
    ) : EvIntent

    data class SnoozeAlarm(
        val minutes: Int = 9
    ) : EvIntent

    data class SetTimer(
        val durationSeconds: Int,
        val label: String = "EV Timer"
    ) : EvIntent

    data class CancelTimer(
        val all: Boolean = false
    ) : EvIntent

    object PauseTimer : EvIntent

    object ResumeTimer : EvIntent

    object CheckTimer : EvIntent

    object CheckAlarm : EvIntent

    // ── Time & Date ─────────────────────────────────────────────────────────

    object GetTime : EvIntent

    object GetDate : EvIntent

    data class GetDayOfWeek(
        val date: String? = null
    ) : EvIntent

    // ── Calculations ────────────────────────────────────────────────────────

    data class Calculate(
        val expression: String,
        val result: Double
    ) : EvIntent

    data class ConvertUnits(
        val value: Double,
        val fromUnit: String,
        val toUnit: String,
        val result: Double? = null
    ) : EvIntent

    // ── Connectivity ────────────────────────────────────────────────────────

    data class ToggleConnectivity(
        val type: ConnectivityType,
        val enabled: Boolean
    ) : EvIntent

    data class GetConnectivityStatus(
        val type: ConnectivityType
    ) : EvIntent

    /** Full network details (type, SSID, IP, signal) rather than a yes/no status. */
    object GetNetworkInfo : EvIntent

    data class SetAirplaneMode(
        val enabled: Boolean
    ) : EvIntent

    data class SetDoNotDisturb(
        val enabled: Boolean,
        val durationMinutes: Int? = null
    ) : EvIntent

    // ── Settings ────────────────────────────────────────────────────────────

    data class OpenSettings(
        val settingType: SettingType,
        val label: String
    ) : EvIntent

    data class SetBrightness(
        val level: Int
    ) : EvIntent

    data class SetVolume(
        val stream: VolumeStream,
        val level: Int,
        val relative: Boolean = false
    ) : EvIntent

    data class ToggleFlashlight(
        val enabled: Boolean? = null
    ) : EvIntent

    object GetBatteryStatus : EvIntent

    object GetStorageStatus : EvIntent

    // ── Communication ───────────────────────────────────────────────────────

    data class MakeCall(
        val target: String,
        val displayName: String? = null
    ) : EvIntent

    data class SendSms(
        val recipient: String,
        val message: String
    ) : EvIntent

    data class SendWhatsAppMessage(
        val recipient: String? = null,
        val message: String? = null
    ) : EvIntent

    data class SendEmail(
        val recipient: String?,
        val subject: String?,
        val body: String?
    ) : EvIntent

    data class ReadMessages(
        val limit: Int = 5,
        val unreadOnly: Boolean = false
    ) : EvIntent

    // ── Information & Web ───────────────────────────────────────────────────

    data class SearchWeb(
        val query: String
    ) : EvIntent

    /**
     * General-knowledge question answered live from the web ("who is the president").
     *
     * Separate from [SearchWeb] so the answer is read aloud instead of only opening
     * a browser, and separate from the hardcoded conversation replies so knowledge
     * questions never fall back to a canned identity line.
     */
    data class Answer(
        val query: String
    ) : EvIntent

    data class GetWeather(
        val location: String? = null,
        val forecast: Boolean = false
    ) : EvIntent

    data class GetNews(
        val category: String? = null,
        val limit: Int = 5
    ) : EvIntent

    data class GetSportsScore(
        val team: String? = null,
        val league: String? = null
    ) : EvIntent

    data class GetStockPrice(
        val symbol: String
    ) : EvIntent

    data class GetDefinition(
        val word: String
    ) : EvIntent

    data class Translate(
        val text: String,
        val targetLanguage: String?,
        val sourceLanguage: String? = null
    ) : EvIntent

    // ── Media ───────────────────────────────────────────────────────────────

    data class PlayMusic(
        val query: String? = null,
        val resume: Boolean = false
    ) : EvIntent

    object PauseMusic : EvIntent

    object ResumeMusic : EvIntent

    object NextTrack : EvIntent

    object PreviousTrack : EvIntent

    object StopMusic : EvIntent

    data class SetMusicVolume(
        val level: Int
    ) : EvIntent

    // ── Productivity ────────────────────────────────────────────────────────

    data class CreateReminder(
        val title: String,
        val dueTime: String? = null,
        val dueDate: String? = null,
        val repeat: String? = null
    ) : EvIntent

    data class ListReminders(
        val pending: Boolean = true,
        val all: Boolean = false
    ) : EvIntent

    data class DeleteReminder(
        val title: String? = null,
        val all: Boolean = false
    ) : EvIntent

    data class CreateNote(
        val title: String?,
        val content: String
    ) : EvIntent

    object ListNotes : EvIntent

    data class SearchNotes(
        val query: String
    ) : EvIntent

    /**
     * Deletes stored notes. Kept separate from [DeleteReminder] because notes and
     * reminders are different stores — asking to remove a note must never report
     * "you have no reminders to delete".
     */
    data class DeleteNote(
        val title: String? = null,
        val all: Boolean = false
    ) : EvIntent

    data class AddCalendarEvent(
        val title: String,
        val startTime: String? = null,
        val endTime: String? = null,
        val date: String? = null,
        val description: String? = null
    ) : EvIntent

    data class GetCalendarEvents(
        val date: String? = null,
        val limit: Int = 5
    ) : EvIntent

    // ── Navigation ──────────────────────────────────────────────────────────

    data class OpenMaps(
        val destination: String? = null,
        val mode: String? = null
    ) : EvIntent

    data class GetDirections(
        val origin: String?,
        val destination: String
    ) : EvIntent

    // ── Conversation & Assistant ────────────────────────────────────────────

    data class Conversation(
        val responseText: String,
        val followUp: Boolean = false,
        val suggestedActions: List<String> = emptyList()
    ) : EvIntent

    data class Unknown(
        val rawQuery: String,
        val confidence: Double = 0.0,
        val suggestions: List<String> = emptyList()
    ) : EvIntent

    data class Help(
        val category: String? = null
    ) : EvIntent

    object Cancel : EvIntent

    object Repeat : EvIntent

    // ── System ──────────────────────────────────────────────────────────────

    data class Shutdown(
        val confirm: Boolean = false
    ) : EvIntent

    object Restart : EvIntent

    object LockScreen : EvIntent

    object TakeScreenshot : EvIntent

    /**
     * Opens the camera. [facing] is "front", "back" or "default"; [mode] is
     * "still" (photo) or "video" (recording).
     */
    data class OpenCamera(
        val facing: String = "back",
        val mode: String = "still"
    ) : EvIntent

    /**
     * Nearby place search, e.g. "hospital near me" → opens Google Maps with the
     * query "hospital near me" so the user sees live results around them.
     */
    data class NearbySearch(val query: String) : EvIntent
}

/** Result produced by executing an intent. */
data class EvExecutionResult(
    val success: Boolean,
    val spokenFeedback: String,
    val userDisplayMessage: String,
    val intent: EvIntent,
    val confidence: Double = 1.0,
    val suggestions: List<String> = emptyList(),
    val data: Map<String, Any?> = emptyMap()
)

enum class SettingType {
    WIFI,
    BLUETOOTH,
    DISPLAY,
    SOUND,
    BATTERY,
    DATE_TIME,
    APPS,
    GENERAL,
    LOCATION,
    SECURITY,
    ACCOUNTS,
    ACCESSIBILITY
}

enum class ConnectivityType {
    WIFI,
    BLUETOOTH,
    HOTSPOT,
    MOBILE_DATA,
    NFC
}

enum class VolumeStream {
    RING,
    MEDIA,
    ALARM,
    NOTIFICATION,
    CALL,
    SYSTEM
}
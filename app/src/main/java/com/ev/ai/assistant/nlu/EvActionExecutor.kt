package com.ev.ai.assistant.nlu

import android.app.SearchManager
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.BatteryManager
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.KeyEvent
import com.ev.ai.assistant.nlu.nlp.Normalizer
import com.ev.ai.assistant.data.preferences.EvPreferences
import com.ev.ai.assistant.service.EvDeviceAdminReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Executes parsed EvIntents by directly interacting with Android framework APIs,
 * device hardware services, and installed applications.
 *
 * Handles the full expanded intent set: device control, alarms/timers, settings,
 * communication, media, productivity, info, navigation, and system actions.
 */
object EvActionExecutor {

    private const val TAG = "EvActionExecutor"

    /**
     * Executes the given intent, triggers voice feedback via TTS, and returns a structured result.
     */
    fun execute(context: Context, intent: EvIntent, speakResponse: Boolean = true): EvExecutionResult {
        val result = when (intent) {
            // ── Device / App Control ──
            is EvIntent.OpenApp -> executeOpenApp(context, intent)
            is EvIntent.CloseApp -> executeCloseApp(context, intent)
            is EvIntent.SwitchApp -> executeSwitchApp(context, intent)

            // ── Alarms & Timers ──
            is EvIntent.SetAlarm -> executeSetAlarm(context, intent)
            is EvIntent.DeleteAlarm -> executeDeleteAlarm(context, intent)
            is EvIntent.EditAlarm -> executeEditAlarm(context, intent)
            is EvIntent.SnoozeAlarm -> executeSnoozeAlarm(context, intent)
            is EvIntent.SetTimer -> executeSetTimer(context, intent)
            is EvIntent.CancelTimer -> executeCancelTimer(context, intent)
            is EvIntent.PauseTimer -> executePauseTimer(context, intent)
            is EvIntent.ResumeTimer -> executeResumeTimer(context, intent)
            is EvIntent.CheckTimer -> executeCheckTimer(context)
            is EvIntent.CheckAlarm -> executeCheckAlarm(context)

            // ── Time & Date ──
            is EvIntent.GetTime -> executeGetTime()
            is EvIntent.GetDate -> executeGetDate()
            is EvIntent.GetDayOfWeek -> executeGetDayOfWeek(intent)

            // ── Calculations ──
            is EvIntent.Calculate -> executeCalculate(intent)
            is EvIntent.ConvertUnits -> executeConvertUnits(intent)

            // ── Connectivity ──
            is EvIntent.ToggleConnectivity -> executeToggleConnectivity(context, intent)
            is EvIntent.GetConnectivityStatus -> executeGetConnectivityStatus(context, intent)
            is EvIntent.GetNetworkInfo -> executeGetNetworkInfo(context, intent)
            is EvIntent.SetAirplaneMode -> executeSetAirplaneMode(context, intent)
            is EvIntent.SetDoNotDisturb -> executeSetDoNotDisturb(context, intent)

            // ── Settings ──
            is EvIntent.OpenSettings -> executeOpenSettings(context, intent)
            is EvIntent.SetBrightness -> executeSetBrightness(context, intent)
            is EvIntent.SetVolume -> executeSetVolume(context, intent)
            is EvIntent.ToggleFlashlight -> executeToggleFlashlight(context, intent)
            is EvIntent.GetBatteryStatus -> executeGetBatteryStatus(context)
            is EvIntent.GetStorageStatus -> executeGetStorageStatus(context)

            // ── Device Control ──
            is EvIntent.LockScreen -> executeLockScreen(context)
            is EvIntent.TakeScreenshot -> executeTakeScreenshot(context)
            is EvIntent.OpenCamera -> executeOpenCamera(context)

            // ── Communication ──
            is EvIntent.MakeCall -> executeMakeCall(context, intent)
            is EvIntent.SendSms -> executeSendSms(context, intent)
            is EvIntent.SendWhatsAppMessage -> executeWhatsApp(context, intent)
            is EvIntent.SendEmail -> executeSendEmail(context, intent)
            is EvIntent.ReadMessages -> executeReadMessages(context, intent)

            // ── Information & Web ──
            is EvIntent.SearchWeb -> executeSearchWeb(context, intent)
            is EvIntent.Answer -> executeAnswer(context, intent)
            is EvIntent.GetWeather -> executeGetWeather(context, intent)
            is EvIntent.GetNews -> executeGetNews(context, intent)
            is EvIntent.GetSportsScore -> executeGetSportsScore(context, intent)
            is EvIntent.GetStockPrice -> executeGetStockPrice(context, intent)
            is EvIntent.GetDefinition -> executeGetDefinition(context, intent)
            is EvIntent.Translate -> executeTranslate(context, intent)

            // ── Media ──
            is EvIntent.PlayMusic -> executePlayMusic(context, intent)
            is EvIntent.PauseMusic -> executePauseMusic(context)
            is EvIntent.ResumeMusic -> executeResumeMusic(context)
            is EvIntent.NextTrack -> executeNextTrack(context)
            is EvIntent.PreviousTrack -> executePreviousTrack(context)
            is EvIntent.StopMusic -> executeStopMusic(context)
            is EvIntent.SetMusicVolume -> executeSetMusicVolume(context, intent)

            // ── Productivity ──
            is EvIntent.CreateReminder -> executeCreateReminder(context, intent)
            is EvIntent.ListReminders -> executeListReminders(context, intent)
            is EvIntent.DeleteReminder -> executeDeleteReminder(context, intent)
            is EvIntent.CreateNote -> executeCreateNote(context, intent)
            is EvIntent.ListNotes -> executeListNotes(context, intent)
            is EvIntent.SearchNotes -> executeSearchNotes(context, intent)
            is EvIntent.DeleteNote -> executeDeleteNote(context, intent)
            is EvIntent.AddCalendarEvent -> executeAddCalendarEvent(context, intent)
            is EvIntent.GetCalendarEvents -> executeGetCalendarEvents(context, intent)

            // ── Navigation ──
            is EvIntent.OpenMaps -> executeOpenMaps(context, intent)
            is EvIntent.GetDirections -> executeGetDirections(context, intent)

            // ── Conversation & Assistant ──
            is EvIntent.Conversation -> EvExecutionResult(
                success = true,
                spokenFeedback = intent.responseText,
                userDisplayMessage = intent.responseText,
                intent = intent,
                suggestions = intent.suggestedActions
            )
            is EvIntent.Unknown -> executeUnknown(intent)
            is EvIntent.Help -> executeHelp(intent)
            is EvIntent.Cancel -> EvExecutionResult(true, "Cancelled.", "Cancelled.", intent)
            is EvIntent.Repeat -> EvExecutionResult(true, "Repeating last action.", "Repeating last action.", intent)

            // ── System ──
            is EvIntent.Shutdown -> executeShutdown(context, intent)
            is EvIntent.Restart -> executeRestart(context)
        }

        if (speakResponse && result.spokenFeedback.isNotBlank()) {
            speakIfEnabled(context, result.spokenFeedback)
        }

        return result
    }

    // ── Async privileged execution ──────────────────────────────────────────────
    // Privileged actions run through `su`, which can block for seconds while the
    // root prompt is answered. They must never run on the main thread, so these
    // helpers run the work in the background and speak the outcome when it's ready.

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /** Runs [block] on the main thread, immediately if we're already there. */
    private fun postToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { try { block() } catch (_: Exception) { } }
        }
    }

    /**
     * Whether any audio output is permitted by Settings.
     *
     * Honors both the "Sound feedback" and "Full voice responses" toggles. When both
     * are off the assistant must run completely silently — and, critically, must never
     * construct the [EvTtsSpeaker] (which spins up the whole TTS engine), so there is
     * no engine-startup latency. We therefore check this before every `getInstance`.
     */
    private fun isAudioEnabled(context: Context): Boolean = try {
        val prefs = EvPreferences.getInstance(context.applicationContext)
        prefs.soundFeedback || prefs.fullVoiceResponses
    } catch (_: Exception) {
        true
    }

    /** Speaks [text] only when the audio toggles allow it; never starts TTS otherwise. */
    private fun speakIfEnabled(context: Context, text: String) {
        if (text.isBlank()) return
        if (!isAudioEnabled(context)) return
        EvTtsSpeaker.getInstance(context).speak(text)
    }

    /**
     * Runs a privileged action off the main thread. Returns immediately with [ack] as
     * the on-screen message; the real result produced by [work] is spoken when ready.
     */
    private fun executeRootAction(
        context: Context,
        intent: EvIntent,
        ack: String,
        work: () -> String
    ): EvExecutionResult {
        val appContext = context.applicationContext
        Thread {
            val message = try {
                work()
            } catch (e: Exception) {
                "Sorry, that didn't work: ${e.localizedMessage ?: "unknown error"}"
            }
            // Speak on the main thread: TextToSpeech must be built with a Looper, and
            // this work runs on a background thread.
            postToMain { speakIfEnabled(appContext, message) }
        }.apply { isDaemon = true }.start()
        // spokenFeedback is blank so execute() doesn't also speak the acknowledgement.
        return EvExecutionResult(true, "", ack, intent)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Device / App Control
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeOpenApp(context: Context, intent: EvIntent.OpenApp): EvExecutionResult {
        val pm = context.packageManager
        val targetPackage = intent.packageName ?: EvNluEngine.resolvePackageName(context, intent.appName)

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val msg = "Opening ${intent.appName}..."
                return EvExecutionResult(true, msg, msg, intent)
            }
        }

        return try {
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(intent.appName)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(storeIntent)
            val msg = "Couldn't find ${intent.appName} installed. Searching in store..."
            EvExecutionResult(true, msg, msg, intent)
        } catch (_: Exception) {
            val msg = "Application ${intent.appName} is not installed on this device."
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeCloseApp(context: Context, intent: EvIntent.CloseApp): EvExecutionResult {
        val pm = context.packageManager
        val targetPackage = intent.packageName ?: EvNluEngine.resolvePackageName(context, intent.appName)
        if (targetPackage == null) {
            val msg = "Couldn't find ${intent.appName}."
            return EvExecutionResult(false, msg, msg, intent)
        }
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(targetPackage)
            val msg = "Stopped background activity for ${intent.appName}. Android does not allow ordinary apps to force-close another app."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not close ${intent.appName}: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeSwitchApp(context: Context, intent: EvIntent.SwitchApp): EvExecutionResult {
        val pkg = EvNluEngine.resolvePackageName(context, intent.appName)
        if (pkg == null) {
            val msg = "Couldn't find ${intent.appName}."
            return EvExecutionResult(false, msg, msg, intent)
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            val msg = "Couldn't launch ${intent.appName}."
            return EvExecutionResult(false, msg, msg, intent)
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(launchIntent)
        val msg = "Switching to ${intent.appName}."
        return EvExecutionResult(true, msg, msg, intent)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Alarms & Timers
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeSetAlarm(context: Context, intent: EvIntent.SetAlarm): EvExecutionResult {
        return try {
            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, intent.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, intent.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, intent.label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(alarmIntent)
            val displayTime = formatTime(intent.hour, intent.minute)
            val whenText = describeNextOccurrence(intent.hour, intent.minute)
            val msg = "Alarm set for $displayTime$whenText."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm: ${e.message}", e)
            val msg = "Could not set alarm: ${e.localizedMessage ?: "Unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    /** " today" / " tomorrow" / " on Friday" for the alarm's next fire time. */
    private fun describeNextOccurrence(hour: Int, minute: Int): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return when {
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> " today"
            else -> {
                now.add(Calendar.DAY_OF_YEAR, 1)
                if (target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) " tomorrow"
                else " " + SimpleDateFormat("EEEE", Locale.getDefault()).format(target.time)
            }
        }
    }

    private fun executeDeleteAlarm(context: Context, intent: EvIntent.DeleteAlarm): EvExecutionResult {
        // Android doesn't provide a public API to delete specific alarms directly.
        // Open the clock app so the user can manage alarms.
        return try {
            val pm = context.packageManager
            val clockIntent = pm.getLaunchIntentForPackage("com.sec.android.app.clock")
                    ?: pm.getLaunchIntentForPackage("com.google.android.deskclock")
            if (clockIntent != null) {
                clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(clockIntent)
                val msg = "Opening Clock app to manage alarms."
                EvExecutionResult(true, msg, msg, intent)
            } else {
                val msg = "Clock app not found."
                EvExecutionResult(false, msg, msg, intent)
            }
        } catch (e: Exception) {
            val msg = "Could not open clock app: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeEditAlarm(context: Context, intent: EvIntent.EditAlarm): EvExecutionResult {
        // Android can't edit an existing alarm, so create the new time and tell the
        // user the old one may still need removing in the Clock app.
        val hadOld = intent.oldHour != 0 || intent.oldMinute != 0
        val setResult = executeSetAlarm(
            context,
            EvIntent.SetAlarm(
                hour = intent.newHour,
                minute = intent.newMinute,
                label = intent.label ?: "EV Alarm",
                days = emptyList(),
                enabled = true
            )
        )
        if (!setResult.success) return setResult
        val displayTime = formatTime(intent.newHour, intent.newMinute)
        val whenText = describeNextOccurrence(intent.newHour, intent.newMinute)
        val msg = if (hadOld) {
            "Moved the alarm to $displayTime$whenText. The old alarm may still be in the Clock app."
        } else {
            "Alarm set for $displayTime$whenText."
        }
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeSnoozeAlarm(context: Context, intent: EvIntent.SnoozeAlarm): EvExecutionResult {
        val msg = "Snoozed for ${intent.minutes} minutes."
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeSetTimer(context: Context, intent: EvIntent.SetTimer): EvExecutionResult {
        return try {
            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, intent.durationSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, intent.label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(timerIntent)
            val displayDuration = formatDuration(intent.durationSeconds)
            val msg = "Timer set for $displayDuration."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer: ${e.message}", e)
            val msg = "Could not set timer: ${e.localizedMessage ?: "Unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeCancelTimer(context: Context, intent: EvIntent.CancelTimer): EvExecutionResult {
        return openClockApp(
            context,
            intent,
            "Android doesn't let apps cancel a running timer. Opening the Clock app so you can stop it."
        )
    }

    private fun executePauseTimer(context: Context, intent: EvIntent.PauseTimer): EvExecutionResult {
        return openClockApp(
            context,
            intent,
            "Android doesn't let apps pause a running timer. Opening the Clock app so you can stop it."
        )
    }

    private fun executeResumeTimer(context: Context, intent: EvIntent.ResumeTimer): EvExecutionResult {
        return openClockApp(
            context,
            intent,
            "Android doesn't let apps resume a timer. Opening the Clock app so you can start it again."
        )
    }

    private fun executeCheckTimer(context: Context): EvExecutionResult {
        return openClockApp(
            context,
            EvIntent.CheckTimer,
            "I can't read the timers in your Clock app. Opening it so you can see them."
        )
    }

    /** Opens the device's Clock app (Google/Samsung) as an honest fallback. */
    private fun openClockApp(context: Context, intent: EvIntent, message: String): EvExecutionResult {
        return try {
            val pm = context.packageManager
            val clockIntent = pm.getLaunchIntentForPackage("com.google.android.deskclock")
                ?: pm.getLaunchIntentForPackage("com.sec.android.app.clock")
                ?: pm.getLaunchIntentForPackage("com.sony.smallapp.app.alarm")
            if (clockIntent != null) {
                clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(clockIntent)
            }
            EvExecutionResult(true, message, message, intent)
        } catch (e: Exception) {
            EvExecutionResult(false, message, message, intent)
        }
    }

    private fun executeCheckAlarm(context: Context): EvExecutionResult {
        // There is no public API to read the Clock app's alarms, so don't claim "none".
        val msg = "I can't read the alarms in your Clock app. Open the Clock app to see them."
        return EvExecutionResult(true, msg, msg, EvIntent.CheckAlarm)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Time & Date
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeGetTime(): EvExecutionResult {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentTime = sdf.format(Calendar.getInstance().time)
        val msg = "It is $currentTime."
        return EvExecutionResult(true, msg, msg, EvIntent.GetTime)
    }

    private fun executeGetDate(): EvExecutionResult {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val currentDate = sdf.format(Calendar.getInstance().time)
        val msg = "Today is $currentDate."
        return EvExecutionResult(true, msg, msg, EvIntent.GetDate)
    }

    private fun executeGetDayOfWeek(intent: EvIntent.GetDayOfWeek): EvExecutionResult {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        val day = sdf.format(Calendar.getInstance().time)
        val msg = "Today is $day."
        return EvExecutionResult(true, msg, msg, intent)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Calculations
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeCalculate(intent: EvIntent.Calculate): EvExecutionResult {
        val formatted = if (intent.result.isNaN() || intent.result.isInfinite()) {
            "Cannot divide by zero."
        } else if (intent.result % 1.0 == 0.0) {
            intent.result.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", intent.result)
        }
        val msg = "The answer is $formatted."
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeConvertUnits(intent: EvIntent.ConvertUnits): EvExecutionResult {
        val result = convertUnit(intent.value, intent.fromUnit, intent.toUnit)
        return if (result == null) {
            val msg = "I can't convert ${intent.fromUnit.ifBlank { "that" }} to " +
                "${intent.toUnit.ifBlank { "that" }}. Try km to miles, kg to pounds, litres to gallons, or celsius to fahrenheit."
            EvExecutionResult(false, msg, msg, intent)
        } else {
            val formatted = if (result % 1.0 == 0.0) result.toLong().toString()
            else String.format(Locale.getDefault(), "%.4f", result).trimEnd('0').trimEnd('.')
            val msg = "${trimNumber(intent.value)} ${intent.fromUnit} is $formatted ${intent.toUnit}."
            EvExecutionResult(true, msg, msg, intent)
        }
    }

    private fun trimNumber(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.getDefault(), "%.4f", v).trimEnd('0').trimEnd('.')

    private fun normalizeUnit(unit: String): String = unit.lowercase(Locale.ROOT).trim()
        .replace("°", "").replace(".", "").trim()

    internal fun convertUnit(value: Double, from: String, to: String): Double? {
        val f = normalizeUnit(from)
        val t = normalizeUnit(to)
        if (f.isEmpty() || t.isEmpty()) return null
        if (f == t) return value

        // Temperature is affine, not a simple scale factor.
        val tempUnits = setOf("c", "celsius", "centigrade", "f", "fahrenheit", "k", "kelvin")
        if (f in tempUnits && t in tempUnits) {
            val celsius = when (f) {
                "c", "celsius", "centigrade" -> value
                "f", "fahrenheit" -> (value - 32) * 5 / 9
                else -> value - 273.15
            }
            return when (t) {
                "c", "celsius", "centigrade" -> celsius
                "f", "fahrenheit" -> celsius * 9 / 5 + 32
                else -> celsius + 273.15
            }
        }

        val factor = factorToBase(f) ?: return null
        val target = factorToBase(t) ?: return null
        // Only convert within the same dimension.
        if (factor.first != target.first) return null
        return value * factor.second / target.second
    }

    /** (dimension, factor-to-base) where base is metres / grams / litres. */
    private fun factorToBase(unit: String): Pair<String, Double>? = when (unit) {
        // Length -> metres
        "mm", "millimeter", "millimetre", "millimeters", "millimetres" -> "length" to 0.001
        "cm", "centimeter", "centimetre", "centimeters", "centimetres" -> "length" to 0.01
        "m", "meter", "metre", "meters", "metres" -> "length" to 1.0
        "km", "kilometer", "kilometre", "kilometers", "kilometres" -> "length" to 1000.0
        "in", "inch", "inches" -> "length" to 0.0254
        "ft", "foot", "feet" -> "length" to 0.3048
        "yd", "yard", "yards" -> "length" to 0.9144
        "mi", "mile", "miles" -> "length" to 1609.344
        // Mass -> grams
        "g", "gram", "grams" -> "mass" to 1.0
        "kg", "kilogram", "kilograms", "kilo", "kilos" -> "mass" to 1000.0
        "mg", "milligram", "milligrams" -> "mass" to 0.001
        "lb", "lbs", "pound", "pounds" -> "mass" to 453.59237
        "oz", "ounce", "ounces" -> "mass" to 28.349523125
        "tonne", "tonnes", "ton", "tons" -> "mass" to 1_000_000.0
        // Volume -> litres
        "ml", "milliliter", "millilitre", "milliliters", "millilitres" -> "volume" to 0.001
        "l", "liter", "litre", "liters", "litres" -> "volume" to 1.0
        "gal", "gallon", "gallons" -> "volume" to 3.785411784
        "qt", "quart", "quarts" -> "volume" to 0.946352946
        "pt", "pint", "pints" -> "volume" to 0.473176473
        "cup", "cups" -> "volume" to 0.2365882365
        // Speed -> metres per second handled as special below (not needed here)
        else -> null
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Connectivity
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeToggleConnectivity(context: Context, intent: EvIntent.ToggleConnectivity): EvExecutionResult {
        val enable = intent.enabled
        val name = when (intent.type) {
            ConnectivityType.WIFI -> "Wi-Fi"
            ConnectivityType.BLUETOOTH -> "Bluetooth"
            ConnectivityType.HOTSPOT -> "mobile hotspot"
            ConnectivityType.MOBILE_DATA -> "mobile data"
            ConnectivityType.NFC -> "NFC"
        }
        val state = if (enable) "enable" else "disable"
        val rootCommands = when (intent.type) {
            ConnectivityType.WIFI -> listOf("svc wifi $state")
            ConnectivityType.BLUETOOTH -> listOf("svc bluetooth $state", "cmd bluetooth_manager $state")
            ConnectivityType.MOBILE_DATA -> listOf("svc data $state")
            ConnectivityType.NFC -> listOf("svc nfc $state")
            ConnectivityType.HOTSPOT -> emptyList()
        }
        // No root (or no shell command for this type): open the matching system screen.
        val settingsIntent = when (intent.type) {
            ConnectivityType.WIFI ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI)
                else Intent(Settings.ACTION_WIFI_SETTINGS)
            ConnectivityType.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            ConnectivityType.HOTSPOT -> Intent("android.settings.TETHER_SETTINGS")
            ConnectivityType.MOBILE_DATA -> Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            ConnectivityType.NFC -> Intent(Settings.ACTION_NFC_SETTINGS)
        }.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        val ack = "${if (enable) "Turning on" else "Turning off"} $name..."
        return executeRootAction(context, intent, ack) {
            val toggled = rootCommands.any { EvRoot.run(it) }
            if (toggled) {
                val reported = when (intent.type) {
                    ConnectivityType.WIFI -> awaitWifiState(context, enable)
                    ConnectivityType.BLUETOOTH -> awaitBluetoothState(enable)
                    else -> null
                }
                when {
                    reported == enable -> "$name is ${if (enable) "on" else "off"}."
                    reported != null -> "I tried, but $name is still ${if (reported) "on" else "off"}."
                    else -> "${if (enable) "Turned on" else "Turned off"} $name."
                }
            } else {
                postToMain {
                    try {
                        context.startActivity(settingsIntent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open ${intent.type} controls", e)
                    }
                }
                "I can't switch $name directly on this device. Opening its settings so you can toggle it."
            }
        }
    }

    private fun readWifiEnabled(context: Context): Boolean? = try {
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.isWifiEnabled
    } catch (_: Exception) {
        null
    }

    private fun readBluetoothEnabled(): Boolean? = try {
        BluetoothAdapter.getDefaultAdapter()?.isEnabled
    } catch (_: Exception) {
        null
    }

    /**
     * Reads Wi-Fi state, retrying briefly so we report the settled value rather than
     * the state cached before the toggle took effect. Runs on a background thread.
     */
    private fun awaitWifiState(context: Context, desired: Boolean, attempts: Int = 10): Boolean? {
        var last = readWifiEnabled(context)
        var i = 0
        while (i < attempts && last != null && last != desired) {
            try { Thread.sleep(150) } catch (_: InterruptedException) { break }
            last = readWifiEnabled(context)
            i++
        }
        return last
    }

    /** Same as [awaitWifiState] but for Bluetooth. */
    private fun awaitBluetoothState(desired: Boolean, attempts: Int = 10): Boolean? {
        var last = readBluetoothEnabled()
        var i = 0
        while (i < attempts && last != null && last != desired) {
            try { Thread.sleep(150) } catch (_: InterruptedException) { break }
            last = readBluetoothEnabled()
            i++
        }
        return last
    }

    private fun executeGetConnectivityStatus(context: Context, intent: EvIntent.GetConnectivityStatus): EvExecutionResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val active = cm.activeNetwork
        val msg = if (active != null) "${intent.type} is connected." else "${intent.type} is not connected."
        return EvExecutionResult(true, msg, msg, intent)
    }

    /**
     * Rich network report ("network info"): connection type, SSID, IP, DNS and signal.
     * Fetched off the main thread so the binder calls never jank the UI.
     */
    private fun executeGetNetworkInfo(context: Context, intent: EvIntent): EvExecutionResult {
        return executeNetworkInfo(context, intent, ack = "Checking network details...") {
            buildNetworkReport(context)
        }
    }

    private fun buildNetworkReport(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return null
        val active = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(active)
        if (caps == null) return "You're not connected to any network right now."

        val type = when {
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "another network"
        }

        val parts = mutableListOf("You're connected over $type.")

        // SSID (needs location permission + location enabled on Android 8.1+).
        if (type == "Wi-Fi") {
            try {
                val wifi = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                val ssid = wifi?.connectionInfo?.ssid?.trim('"')
                if (!ssid.isNullOrBlank() && ssid != "02:00:00:00:00:00" && ssid != "<unknown ssid>") {
                    parts += "Network name is $ssid."
                }
            } catch (e: Exception) {
                Log.w(TAG, "SSID unavailable: ${e.message}")
            }
        }

        // IP addresses + DNS from LinkProperties (no deprecated APIs).
        try {
            val lp = cm.getLinkProperties(active)
            val ip = lp?.linkAddresses
                ?.firstOrNull { it.address is java.net.Inet4Address }
                ?.address?.hostAddress
            if (!ip.isNullOrBlank()) parts += "Local IP is $ip."
            val dns = lp?.dnsServers?.firstOrNull { it is java.net.Inet4Address }?.hostAddress
            if (!dns.isNullOrBlank()) parts += "DNS is $dns."
        } catch (e: Exception) {
            Log.w(TAG, "Link properties unavailable: ${e.message}")
        }

        // Signal strength (API 29+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val dbm = caps.signalStrength
                if (dbm != 0) parts += "Signal is about $dbm dBm."
            } catch (e: Exception) {
                Log.w(TAG, "Signal unavailable: ${e.message}")
            }
        }

        if (!caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            parts += "You're connected to the network but don't have internet access."
        }
        return parts.joinToString(" ")
    }

    private fun executeSetAirplaneMode(context: Context, intent: EvIntent.SetAirplaneMode): EvExecutionResult {
        val enable = intent.enabled
        val ack = "${if (enable) "Enabling" else "Disabling"} airplane mode..."
        return executeRootAction(context, intent, ack) {
            // Preferred: the connectivity service command (Android 11+).
            val primary = "cmd connectivity airplane-mode ${if (enable) "enable" else "disable"}"
            // Legacy fallback: flip the global setting and broadcast the change.
            val legacy = "settings put global airplane_mode_on ${if (enable) 1 else 0}; " +
                "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enable"
            if (EvRoot.run(primary) || EvRoot.run(legacy)) {
                "Airplane mode ${if (enable) "enabled" else "disabled"}."
            } else {
                postToMain {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open airplane mode settings", e)
                    }
                }
                "I can't change airplane mode directly on this device. Opening its settings."
            }
        }
    }

    private fun executeSetDoNotDisturb(context: Context, intent: EvIntent.SetDoNotDisturb): EvExecutionResult {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
        if (!canWrite) {
            val msg = "Notification access permission is required to toggle Do Not Disturb."
            return EvExecutionResult(false, msg, msg, intent)
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.setInterruptionFilter(
                    if (intent.enabled) {
                        android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    } else {
                        android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                    }
                )
            }
            val durationText = intent.durationMinutes?.let { " for $it minutes" } ?: ""
            val action = if (intent.enabled) "Enabled" else "Disabled"
            val msg = "$action Do Not Disturb$durationText."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not toggle Do Not Disturb: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Settings
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeOpenSettings(context: Context, intent: EvIntent.OpenSettings): EvExecutionResult {
        val action = when (intent.settingType) {
            SettingType.WIFI ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI
                else Settings.ACTION_WIFI_SETTINGS
            SettingType.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingType.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingType.SOUND -> Settings.ACTION_SOUND_SETTINGS
            // There is no public battery-settings action; POWER_USAGE_SUMMARY opens the
            // real battery screen on most devices.
            SettingType.BATTERY -> "android.intent.action.POWER_USAGE_SUMMARY"
            SettingType.DATE_TIME -> Settings.ACTION_DATE_SETTINGS
            SettingType.APPS -> Settings.ACTION_APPLICATION_SETTINGS
            SettingType.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            SettingType.SECURITY -> Settings.ACTION_SECURITY_SETTINGS
            SettingType.ACCOUNTS -> Settings.ACTION_SYNC_SETTINGS
            SettingType.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            SettingType.GENERAL -> Settings.ACTION_SETTINGS
        }
        return try {
            val settingsIntent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(settingsIntent)
            val msg = "Opening ${intent.label}..."
            EvExecutionResult(true, msg, msg, intent)
        } catch (_: Exception) {
            // Some devices don't implement a specific panel — fall back to battery saver
            // for BATTERY, otherwise the general settings screen.
            return try {
                val fallbackAction = if (intent.settingType == SettingType.BATTERY) {
                    Settings.ACTION_BATTERY_SAVER_SETTINGS
                } else {
                    Settings.ACTION_SETTINGS
                }
                val generalIntent = Intent(fallbackAction).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(generalIntent)
                val msg = "Opening ${intent.label}..."
                EvExecutionResult(true, msg, msg, intent)
            } catch (e: Exception) {
                val msg = "Could not open settings: ${e.localizedMessage ?: "unknown error"}"
                EvExecutionResult(false, msg, msg, intent)
            }
        }
    }

    private fun executeSetBrightness(context: Context, intent: EvIntent.SetBrightness): EvExecutionResult {
        val level = coerceIn(intent.level, 0, 100)
        val ack = "Setting brightness to $level percent..."
        return executeRootAction(context, intent, ack) {
            // Root can change the real system brightness.
            val raw = (level * 255 / 100).coerceIn(0, 255)
            if (EvRoot.run("settings put system screen_brightness_mode 0; settings put system screen_brightness $raw")) {
                "Brightness set to $level percent."
            } else {
                // Otherwise adjust the current window's brightness if we have one.
                val window = (context as? android.app.Activity)?.window
                if (window != null) {
                    postToMain {
                        window.attributes = window.attributes.apply { screenBrightness = level / 100f }
                    }
                    "Brightness set to $level percent."
                } else {
                    postToMain {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_DISPLAY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open display settings", e)
                        }
                    }
                    "I can't change the system brightness without root. Opening display settings."
                }
            }
        }
    }

    private fun executeSetVolume(context: Context, intent: EvIntent.SetVolume): EvExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamType = when (intent.stream) {
            VolumeStream.RING -> AudioManager.STREAM_RING
            VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
            VolumeStream.ALARM -> AudioManager.STREAM_ALARM
            VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            VolumeStream.CALL -> AudioManager.STREAM_VOICE_CALL
            VolumeStream.SYSTEM -> AudioManager.STREAM_SYSTEM
        }
        val maxVolume = am.getStreamMaxVolume(streamType)
        val targetLevel = if (intent.relative) {
            val current = am.getStreamVolume(streamType)
            val step = maxOf(1, maxVolume / 10)
            val delta = if (intent.level >= 50) step else -step
            coerceIn(current + delta, 0, maxVolume)
        } else {
            (intent.level * maxVolume / 100).coerceIn(0, maxVolume)
        }
        return try {
            am.setStreamVolume(streamType, targetLevel, 0)
            val pct = if (maxVolume > 0) (targetLevel * 100 / maxVolume) else 0
            val label = prettyStream(intent.stream)
            val msg = when {
                !intent.relative && targetLevel == 0 -> "$label muted."
                else -> "$label volume set to $pct percent."
            }
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not set volume: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun prettyStream(stream: VolumeStream): String = when (stream) {
        VolumeStream.RING -> "Ring"
        VolumeStream.MEDIA -> "Media"
        VolumeStream.ALARM -> "Alarm"
        VolumeStream.NOTIFICATION -> "Notification"
        VolumeStream.CALL -> "Call"
        VolumeStream.SYSTEM -> "System"
    }

    private fun executeToggleFlashlight(context: Context, intent: EvIntent.ToggleFlashlight): EvExecutionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        ensureTorchCallback(cameraManager)
        val cameraId = findFlashCamera(cameraManager) ?: return EvExecutionResult(
            false, "This device has no camera flash.", "This device has no camera flash.", intent
        )
        val enabled = intent.enabled ?: !torchOn
        return try {
            cameraManager.setTorchMode(cameraId, enabled)
            torchOn = enabled
            val msg = if (enabled) "Flashlight on." else "Flashlight off."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not toggle the flashlight: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    /** True torch state, kept in sync with the system through a camera callback. */
    @Volatile private var torchOn = false
    private var torchCallbackRegistered = false

    private fun ensureTorchCallback(cameraManager: CameraManager) {
        if (torchCallbackRegistered) return
        try {
            cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchOn = enabled
                }
            }, null)
            torchCallbackRegistered = true
        } catch (_: Exception) {
            // Devices without a flash may reject the callback; ignore.
        }
    }

    private fun findFlashCamera(cameraManager: CameraManager): String? {
        var firstFlash: String? = null
        for (id in cameraManager.cameraIdList) {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_BACK
                    ) {
                        return id
                    }
                    if (firstFlash == null) firstFlash = id
                }
            } catch (_: Exception) {
                // Skip cameras we can't query.
            }
        }
        return firstFlash
    }

    private fun executeGetBatteryStatus(context: Context): EvExecutionResult {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val msg = "Battery is at $pct percent" + if (charging) " and charging." else "."
        return EvExecutionResult(true, msg, msg, EvIntent.GetBatteryStatus)
    }

    private fun executeGetStorageStatus(context: Context): EvExecutionResult {
        return try {
            val stat = StatFs(context.filesDir.path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            val used = (total - free).coerceAtLeast(0)
            val usedPercent = if (total > 0) (used * 100 / total) else 0
            val msg = "Storage: ${formatBytes(free)} free of ${formatBytes(total)} ($usedPercent percent used)."
            EvExecutionResult(true, msg, msg, EvIntent.GetStorageStatus)
        } catch (e: Exception) {
            val msg = "Could not read storage: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, EvIntent.GetStorageStatus)
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 1.0) String.format(Locale.getDefault(), "%.1f GB", gb)
        else String.format(Locale.getDefault(), "%.0f MB", bytes / 1_000_000.0)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Device Control
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeLockScreen(context: Context): EvExecutionResult {
        return executeRootAction(context, EvIntent.LockScreen, "Locking the screen...") {
            // Order matters: DevicePolicyManager locks instantly and needs no root, so
            // try that before the root power-key path.
            if (lockWithDeviceAdmin(context)) {
                "Screen locked."
            } else if (EvRoot.run("input keyevent ${KeyEvent.KEYCODE_POWER}")) {
                "Screen locked."
            } else {
                // Neither device-admin nor root is available yet; open the prompt that
                // lets the user grant device-admin (the one-tap non-root path).
                promptEnableDeviceAdmin(context)
                "I've opened the device administrator screen. Enable E.V., then say lock screen again."
            }
        }
    }

    /** True when E.V. is already an active device administrator and the lock succeeded. */
    private fun lockWithDeviceAdmin(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            val admin = ComponentName(context, EvDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Device-admin lock failed: ${e.message}")
            false
        }
    }

    /** Opens the system screen that grants E.V. device-administrator (force-lock) rights. */
    private fun promptEnableDeviceAdmin(context: Context) {
        try {
            val admin = ComponentName(context, EvDeviceAdminReceiver::class.java)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "E.V. needs administrator access to lock the screen on command without root."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open device-admin prompt", e)
        }
    }

    private fun executeTakeScreenshot(context: Context): EvExecutionResult {
        return executeRootAction(context, EvIntent.TakeScreenshot, "Taking a screenshot...") {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "EV Assistant"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "EV_${System.currentTimeMillis()}.png")
            val ok = EvRoot.run("screencap -p \"${file.absolutePath}\"")
            if (ok && file.exists() && file.length() > 0) {
                postToMain { MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null) }
                "Screenshot saved as ${file.name}."
            } else {
                // Be honest about the limitation and point at the real fix (grant root).
                "Capturing the screen needs root. Grant E.V. root in Magisk, then try again."
            }
        }
    }

    private fun executeOpenCamera(context: Context): EvExecutionResult {
        return try {
            val pm = context.packageManager
            val cameraIntent = pm.getLaunchIntentForPackage("com.sec.android.app.camera")
                    ?: pm.getLaunchIntentForPackage("com.android.camera")
                    ?: Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(cameraIntent)
            val msg = "Opening camera..."
            EvExecutionResult(true, msg, msg, EvIntent.OpenCamera)
        } catch (e: Exception) {
            val msg = "Could not open camera: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, EvIntent.OpenCamera)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Communication
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeMakeCall(context: Context, intent: EvIntent.MakeCall): EvExecutionResult {
        return try {
            // Resolve what to dial. A spoken number is used verbatim; a hardcoded
            // speed-dial relationship ("dad", "shruti didi") wins over the fuzzy
            // address book so "call dad" can never ring "Omkar Dada PICT pune";
            // otherwise the text is looked up in the contacts database.
            val quick = EvQuickContacts.resolve(intent.target)
            val number = normalizePhoneNumber(intent.target)
                ?: quick?.number
                ?: resolveContactNumber(context, intent.target)

            // Never dial a fallback value: if the name could not be resolved to a real
            // number we must fail loudly instead of placing a call to garbage.
            if (number.isNullOrBlank()) {
                val label = intent.displayName ?: intent.target
                val msg = "I couldn't find a contact named \"$label\". Please check the name and try again."
                return EvExecutionResult(false, msg, msg, intent)
            }

            val action = if (
                context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
            ) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val callIntent = Intent(action, Uri.parse("tel:${Uri.encode(number)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            val spokenName = quick?.name ?: intent.displayName ?: number
            val msg = if (action == Intent.ACTION_CALL) {
                "Calling $spokenName..."
            } else {
                "Opening the dialer for $spokenName..."
            }
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not start call: ${e.localizedMessage}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeSendSms(context: Context, intent: EvIntent.SendSms): EvExecutionResult {
        if (intent.recipient.isBlank() || intent.message.isBlank()) {
            val msg = "Please specify a recipient and message."
            return EvExecutionResult(false, msg, msg, intent)
        }
        // The recipient may be a name ("mom") or a number ("5551234"). Hardcoded
        // speed-dial relationships win over the fuzzy address book.
        val number = normalizePhoneNumber(intent.recipient)
            ?: EvQuickContacts.resolve(intent.recipient)?.number
            ?: resolveContactNumber(context, intent.recipient)
        if (number.isNullOrBlank()) {
            val msg = "I couldn't find a contact named \"${intent.recipient}\". Please check the name and try again."
            return EvExecutionResult(false, msg, msg, intent)
        }

        val hasPermission = context.checkSelfPermission(Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                SmsManager.getDefault().sendTextMessage(number, null, intent.message, null, null)
                val msg = "Message sent to ${intent.recipient}."
                return EvExecutionResult(true, msg, msg, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Direct SMS failed, opening composer: ${e.message}")
            }
        }
        // No permission (or sending failed): hand off to the Messages composer, which
        // needs no permission and still results in a real, sendable message.
        return openSmsComposer(context, number, intent.message, intent.recipient, intent)
    }

    private fun openSmsComposer(
        context: Context,
        number: String,
        body: String,
        label: String,
        intent: EvIntent
    ): EvExecutionResult {
        return try {
            val uri = Uri.parse("smsto:${Uri.encode(number)}")
            val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            val msg = "Opening Messages to send to $label."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not open the messaging app: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeWhatsApp(context: Context, intent: EvIntent.SendWhatsAppMessage): EvExecutionResult {
        val pm = context.packageManager
        val waPackage = "com.whatsapp"
        return try {
            if (intent.message != null && intent.message.isNotBlank()) {
                val recipient = intent.recipient
                val digits = if (recipient != null && recipient.isNotBlank()) {
                    normalizePhoneNumber(recipient)?.filter { it.isDigit() }
                        ?: EvQuickContacts.resolve(recipient)?.number?.filter { it.isDigit() }
                        ?: resolveContactNumber(context, recipient)?.filter { it.isDigit() }
                } else {
                    null
                }
                if (recipient != null && recipient.isNotBlank() && digits.isNullOrBlank()) {
                    val msg = "I couldn't find a contact named \"$recipient\"."
                    return EvExecutionResult(false, msg, msg, intent)
                }
                val uri = if (digits != null) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$digits&text=${Uri.encode(intent.message)}")
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(intent.message)}")
                }
                val waIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    setPackage(waPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(waIntent)
                val msg = "Opening WhatsApp to send the message..."
                EvExecutionResult(true, msg, msg, intent)
            } else {
                val launchIntent = pm.getLaunchIntentForPackage(waPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val msg = "Opening WhatsApp..."
                    EvExecutionResult(true, msg, msg, intent)
                } else {
                    val msg = "WhatsApp is not installed on this device."
                    EvExecutionResult(false, msg, msg, intent)
                }
            }
        } catch (e: Exception) {
            val msg = "Could not open WhatsApp: ${e.localizedMessage}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeSendEmail(context: Context, intent: EvIntent.SendEmail): EvExecutionResult {
        return try {
            val raw = intent.recipient?.trim()
            val address = when {
                raw.isNullOrBlank() -> ""
                raw.contains("@") -> raw
                else -> resolveContactEmail(context, raw) ?: raw
            }
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(address)}")
                putExtra(Intent.EXTRA_SUBJECT, intent.subject)
                putExtra(Intent.EXTRA_TEXT, intent.body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(emailIntent)
            val msg = if (address.isNotBlank() && address.contains("@")) {
                "Opening the email composer for $address..."
            } else {
                "Opening the email composer..."
            }
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not open email: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    /** Best-effort email lookup, ranked the same way as phone numbers. */
    private fun resolveContactEmail(context: Context, target: String): String? {
        val query = target.trim()
        if (query.isEmpty() || !query.any { it.isLetter() }) return null
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val projection = arrayOf(
            android.provider.ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
            android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS
        )
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val selection =
            "${android.provider.ContactsContract.CommonDataKinds.Email.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
        var bestEmail: String? = null
        var bestScore = 0
        context.contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            selection,
            arrayOf("%$escaped%"),
            null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(
                android.provider.ContactsContract.CommonDataKinds.Email.DISPLAY_NAME
            )
            val emailIdx = cursor.getColumnIndexOrThrow(
                android.provider.ContactsContract.CommonDataKinds.Email.ADDRESS
            )
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIdx) ?: continue
                val email = cursor.getString(emailIdx)
                if (email.isNullOrBlank()) continue
                val score = contactMatchScore(displayName, query)
                if (score >= 3) return email
                if (score > bestScore) {
                    bestScore = score
                    bestEmail = email
                }
            }
        }
        return bestEmail
    }

    /** Opens the device's default SMS app (used when we lack the read permission). */
    private fun openMessagesApp(context: Context): Boolean {
        return try {
            val msgIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(msgIntent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not open Messages app: ${e.message}")
            false
        }
    }

    private fun executeReadMessages(context: Context, intent: EvIntent.ReadMessages): EvExecutionResult {
        val permission = context.checkSelfPermission(Manifest.permission.READ_SMS)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // Without permission we can't read the inbox, but opening Messages is still useful.
            val fallback = openMessagesApp(context)
            val msg = if (fallback) {
                "SMS read permission is required to read messages. Opening your Messages app instead."
            } else {
                "SMS read permission is required to read messages."
            }
            return EvExecutionResult(fallback, msg, msg, intent)
        }
        return try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("address", "body", "date")
            val selection = if (intent.unreadOnly) "read = 0" else null
            context.contentResolver.query(uri, projection, selection, null, "date DESC")?.use { cursor ->
                val count = cursor.count
                if (count == 0) {
                    val msg = "No messages found."
                    return@use EvExecutionResult(true, msg, msg, intent)
                }
                val messages = mutableListOf<String>()
                var i = 0
                while (cursor.moveToNext() && i < intent.limit) {
                    val address = cursor.getString(0)
                    val body = cursor.getString(1)
                    messages.add("$address: $body")
                    i++
                }
                val msg = "You have $count messages. ${messages.joinToString(" ")}"
                return@use EvExecutionResult(true, msg, msg, intent)
            }
            val msg = "Could not read messages."
            EvExecutionResult(false, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not read messages: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Information & Web
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeSearchWeb(context: Context, intent: EvIntent.SearchWeb): EvExecutionResult {
        return try {
            val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, intent.query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            val msg = "Searching the web for \"${intent.query}\"..."
            EvExecutionResult(true, msg, msg, intent)
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(intent.query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                val msg = "Searching Google for \"${intent.query}\"..."
                EvExecutionResult(true, msg, msg, intent)
            } catch (e: Exception) {
                val msg = "Could not launch web search: ${e.localizedMessage}"
                EvExecutionResult(false, msg, msg, intent)
            }
        }
    }

    /**
     * Runs a blocking web lookup on a background thread and speaks the real answer
     * through TTS. Returns immediately with an acknowledgement so the UI never blocks.
     */
    private fun executeNetworkInfo(
        context: Context,
        intent: EvIntent,
        ack: String,
        notFound: String = "Sorry, I couldn't fetch that right now. Check your internet connection and try again.",
        fetch: () -> String?
    ): EvExecutionResult {
        val appContext = context.applicationContext
        Thread {
            val answer = try {
                fetch()
            } catch (e: Exception) {
                Log.w(TAG, "Network lookup failed: ${e.message}")
                null
            }
            val text = if (answer.isNullOrBlank()) notFound else answer
            // Speak on the main thread: TextToSpeech must be built with a Looper, and
            // this work runs on a background thread.
            postToMain { speakIfEnabled(appContext, text) }
        }.apply { isDaemon = true }.start()
        // spokenFeedback is intentionally blank: the background thread speaks the answer.
        return EvExecutionResult(true, "", ack, intent)
    }

    /**
     * Answers a general-knowledge question ("who is the president", "president of usa")
     * from the live web. When nothing is found we fall back to the plain
     * "I didn't understand that." line instead of a canned identity response.
     */
    private fun executeAnswer(context: Context, intent: EvIntent.Answer): EvExecutionResult {
        val query = intent.query
        if (query.isBlank()) {
            val msg = "What would you like to know?"
            return EvExecutionResult(false, msg, msg, intent)
        }
        return executeNetworkInfo(
            context,
            intent,
            ack = "Let me look that up...",
            notFound = "I didn't understand that."
        ) {
            EvWeb.answer(query)
        }
    }

    private fun executeGetWeather(context: Context, intent: EvIntent.GetWeather): EvExecutionResult {
        val location = intent.location
        val ack = if (location.isNullOrBlank()) "Checking the weather..." else "Checking the weather in $location..."
        return executeNetworkInfo(context, intent, ack) {
            EvWeb.weather(location)?.let { "Weather: $it." }
        }
    }

    private fun executeGetNews(context: Context, intent: EvIntent.GetNews): EvExecutionResult {
        return executeNetworkInfo(context, intent, "Getting the latest headlines...") {
            val items = EvWeb.news(intent.limit)
            if (items.isEmpty()) null else "Here are the top headlines. " + items.joinToString(". ") + "."
        }
    }

    private fun executeGetSportsScore(context: Context, intent: EvIntent.GetSportsScore): EvExecutionResult {
        // No free key-less sports API; a web search is the honest fallback.
        val query = if (intent.team.isNullOrBlank()) "live sports scores" else "${intent.team} score"
        return executeSearchWeb(context, EvIntent.SearchWeb(query))
    }

    private fun executeGetStockPrice(context: Context, intent: EvIntent.GetStockPrice): EvExecutionResult {
        if (intent.symbol.isBlank()) {
            val msg = "Which company or ticker should I look up?"
            return EvExecutionResult(false, msg, msg, intent)
        }
        return executeNetworkInfo(context, intent, "Checking ${intent.symbol}...") {
            EvWeb.stock(intent.symbol)
        }
    }

    private fun executeGetDefinition(context: Context, intent: EvIntent.GetDefinition): EvExecutionResult {
        if (intent.word.isBlank()) {
            val msg = "Which word should I define?"
            return EvExecutionResult(false, msg, msg, intent)
        }
        return executeNetworkInfo(context, intent, "Looking up ${intent.word}...") {
            EvWeb.definition(intent.word)?.let { "${intent.word}: $it" }
        }
    }

    private fun executeTranslate(context: Context, intent: EvIntent.Translate): EvExecutionResult {
        if (intent.text.isBlank()) {
            val msg = "What should I translate?"
            return EvExecutionResult(false, msg, msg, intent)
        }
        val target = intent.targetLanguage
        val ack = if (target.isNullOrBlank()) "Translating..." else "Translating into $target..."
        return executeNetworkInfo(context, intent, ack) {
            EvWeb.translate(intent.text, target)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Media
    // ════════════════════════════════════════════════════════════════════════════

    private fun executePlayMusic(context: Context, intent: EvIntent.PlayMusic): EvExecutionResult {
        val query = intent.query ?: ""
        // Request audio focus first so the player that opens is actually allowed to play.
        requestMediaAudioFocus(context)
        return try {
            val musicIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                // Narrow the focus to audio (not "*") so music apps — not YouTube video
                // search — handle the request and autoplay the best match.
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
                if (query.isNotBlank()) {
                    putExtra(SearchManager.QUERY, query)
                    putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, query)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(musicIntent)
            // The search screen does not autoplay, so nudge the active media session.
            // Two delays: one shortly after the app opens, one after it has buffered.
            scheduleMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY, 1200)
            scheduleMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY, 3200)
            val msg = if (query.isNotBlank()) "Playing $query..." else "Playing music..."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not play music: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun requestMediaAudioFocus(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val req = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .build()
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus({ }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed: ${e.message}")
        }
    }

    private fun executePauseMusic(context: Context): EvExecutionResult {
        return mediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PAUSE, "Pausing music.")
    }

    private fun executeResumeMusic(context: Context): EvExecutionResult {
        return mediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY, "Resuming music.")
    }

    private fun executeNextTrack(context: Context): EvExecutionResult {
        return mediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT, "Next track.")
    }

    private fun executePreviousTrack(context: Context): EvExecutionResult {
        return mediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Previous track.")
    }

    private fun executeStopMusic(context: Context): EvExecutionResult {
        return mediaKeyEvent(context, KeyEvent.KEYCODE_MEDIA_STOP, "Stopping music.")
    }

    private fun executeSetMusicVolume(context: Context, intent: EvIntent.SetMusicVolume): EvExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetLevel = (intent.level * maxVolume / 100).coerceIn(0, maxVolume)
        return try {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetLevel, 0)
            val pct = (targetLevel * 100 / maxVolume)
            val msg = "Music volume set to $pct percent."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not set music volume: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun mediaKeyEvent(context: Context, keyCode: Int, msg: String): EvExecutionResult {
        // Send the key on a background thread: the first root call may block on the
        // superuser prompt, which must not happen on the main thread.
        return executeRootAction(context, EvIntent.Conversation(msg), msg) {
            sendMediaKey(context.applicationContext, keyCode)
            msg
        }
    }

    /**
     * Delivers a media key event. A root `input keyevent` is the only method that works
     * without holding a media-session/notification-listener permission; the broadcast is
     * kept as a fallback for rooted-free devices that expose a player.
     */
    private fun sendMediaKey(context: Context, keyCode: Int) {
        if (EvRoot.run("input keyevent $keyCode")) return
        try {
            val down = Intent(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            val up = Intent(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            context.sendOrderedBroadcast(down, null)
            context.sendOrderedBroadcast(up, null)
        } catch (e: Exception) {
            Log.w(TAG, "Media key broadcast failed: ${e.message}")
        }
    }

    /** Sends [keyCode] after [delayMs] without blocking the calling thread. */
    private fun scheduleMediaKey(context: Context, keyCode: Int, delayMs: Long) {
        val appContext = context.applicationContext
        Thread {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                return@Thread
            }
            sendMediaKey(appContext, keyCode)
        }.apply { isDaemon = true }.start()
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Productivity
    // ════════════════════════════════════════════════════════════════════════════

    // ── Persistence ────────────────────────────────────────────────────────
    // Reminders and notes are actually stored so "list"/"search"/"delete" return
    // real data instead of canned phrases.

    private const val PREFS_NAME = "ev_productivity"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readArray(context: Context, key: String): JSONArray {
        val raw = prefs(context).getString(key, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun writeArray(context: Context, key: String, arr: JSONArray) {
        prefs(context).edit().putString(key, arr.toString()).apply()
    }

    private fun prettyTime(raw: String): String {
        val m = Regex("""^(\d{1,2}):(\d{2})$""").find(raw.trim()) ?: return raw
        val h = m.groupValues[1].toIntOrNull() ?: return raw
        val min = m.groupValues[2]
        val suffix = if (h >= 12) "PM" else "AM"
        val h12 = (h % 12).let { if (it == 0) 12 else it }
        return "$h12:$min $suffix"
    }

    private fun executeCreateReminder(context: Context, intent: EvIntent.CreateReminder): EvExecutionResult {
        val arr = readArray(context, "reminders")
        arr.put(
            JSONObject()
                .put("title", intent.title)
                .putOpt("dueTime", intent.dueTime)
                .putOpt("dueDate", intent.dueDate)
                .putOpt("repeat", intent.repeat)
                .put("createdAt", System.currentTimeMillis())
        )
        writeArray(context, "reminders", arr)
        val whenText = buildString {
            intent.dueTime?.let { append(" at ${prettyTime(it)}") }
            intent.dueDate?.let { append(" on $it") }
            intent.repeat?.let { append(" every $it") }
        }
        val msg = "Reminder saved: ${intent.title}$whenText."
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeListReminders(context: Context, intent: EvIntent.ListReminders): EvExecutionResult {
        val arr = readArray(context, "reminders")
        if (arr.length() == 0) {
            val msg = "You have no reminders yet."
            return EvExecutionResult(true, msg, msg, intent)
        }
        val items = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val title = o.optString("title")
            val time = o.optString("dueTime")
            val date = o.optString("dueDate")
            when {
                time.isNotBlank() && date.isNotBlank() -> "$title, ${prettyTime(time)} on $date"
                time.isNotBlank() -> "$title, ${prettyTime(time)}"
                date.isNotBlank() -> "$title, on $date"
                else -> title
            }
        }
        val msg = "You have ${arr.length()} reminder(s): " + items.joinToString("; ")
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeDeleteReminder(context: Context, intent: EvIntent.DeleteReminder): EvExecutionResult {
        val arr = readArray(context, "reminders")
        if (arr.length() == 0) {
            val msg = "You have no reminders to delete."
            return EvExecutionResult(true, msg, msg, intent)
        }
        if (intent.all) {
            writeArray(context, "reminders", JSONArray())
            val msg = "All reminders deleted."
            return EvExecutionResult(true, msg, msg, intent)
        }
        val query = intent.title?.trim()
        if (query.isNullOrBlank()) {
            val msg = "Which reminder should I delete?"
            return EvExecutionResult(false, msg, msg, intent)
        }
        val kept = JSONArray()
        var removed = ""
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (removed.isEmpty() && o.optString("title").contains(query, ignoreCase = true)) {
                removed = o.optString("title")
            } else {
                kept.put(o)
            }
        }
        return if (removed.isEmpty()) {
            val msg = "I couldn't find a reminder matching \"$query\"."
            EvExecutionResult(false, msg, msg, intent)
        } else {
            writeArray(context, "reminders", kept)
            val msg = "Deleted reminder: $removed."
            EvExecutionResult(true, msg, msg, intent)
        }
    }

    private fun executeCreateNote(context: Context, intent: EvIntent.CreateNote): EvExecutionResult {
        val arr = readArray(context, "notes")
        arr.put(
            JSONObject()
                .putOpt("title", intent.title)
                .put("content", intent.content)
                .put("createdAt", System.currentTimeMillis())
        )
        writeArray(context, "notes", arr)
        val msg = "Note saved: ${intent.content}"
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeListNotes(context: Context, intent: EvIntent.ListNotes): EvExecutionResult {
        val arr = readArray(context, "notes")
        if (arr.length() == 0) {
            val msg = "You have no notes yet."
            return EvExecutionResult(true, msg, msg, intent)
        }
        val items = (0 until arr.length()).map { arr.getJSONObject(it).optString("content") }
        val msg = "You have ${arr.length()} note(s): " + items.joinToString("; ")
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeSearchNotes(context: Context, intent: EvIntent.SearchNotes): EvExecutionResult {
        val arr = readArray(context, "notes")
        val query = intent.query.trim()
        val matches = (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter {
                it.optString("content").contains(query, ignoreCase = true) ||
                    it.optString("title").contains(query, ignoreCase = true)
            }
        return if (matches.isEmpty()) {
            val msg = "No notes found for \"$query\"."
            EvExecutionResult(true, msg, msg, intent)
        } else {
            val msg = "Matching notes: " + matches.joinToString("; ") { it.optString("content") }
            EvExecutionResult(true, msg, msg, intent)
        }
    }

    /**
     * Deletes stored notes. This is deliberately separate from [executeDeleteReminder]:
     * notes and reminders live in different stores, so asking to drop a note must never
     * report "you have no reminders to delete".
     */
    private fun executeDeleteNote(context: Context, intent: EvIntent.DeleteNote): EvExecutionResult {
        val arr = readArray(context, "notes")
        if (arr.length() == 0) {
            val msg = "You have no notes to delete."
            return EvExecutionResult(false, msg, msg, intent)
        }
        val title = intent.title?.trim()?.lowercase()
        val kept = JSONArray()
        val removed = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val content = obj.optString("content")
            val noteTitle = obj.optString("title")
            val matchesAll = intent.all && title == null
            val matchesTitle = title != null &&
                (content.lowercase().contains(title) || noteTitle.lowercase().contains(title))
            if (matchesAll || matchesTitle) {
                removed += content.ifBlank { noteTitle }
            } else {
                kept.put(obj)
            }
        }
        return if (removed.isEmpty()) {
            val msg = intent.title?.let { "I couldn't find a note matching \"$it\"." }
                ?: "You have no notes to delete."
            EvExecutionResult(false, msg, msg, intent)
        } else {
            writeArray(context, "notes", kept)
            val msg = if (intent.all && title == null) {
                "Deleted all ${removed.size} note(s)."
            } else {
                "Deleted note: ${removed.joinToString("; ")}."
            }
            EvExecutionResult(true, msg, msg, intent)
        }
    }

    private fun executeAddCalendarEvent(context: Context, intent: EvIntent.AddCalendarEvent): EvExecutionResult {
        return try {
            val calIntent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, intent.title)
                .putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    intent.description
                        ?: intent.startTime?.let { "Starts at $it" }
                        ?: ""
                )
            val beginMs = intent.startTime?.let { parseTodayMillis(it) } ?: System.currentTimeMillis()
            calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMs)
            calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, beginMs + 60 * 60 * 1000L)
            calIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(calIntent)
            val msg = "Opening the calendar to create: ${intent.title}."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not open the calendar: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun parseTodayMillis(raw: String): Long? {
        for (pattern in listOf("h:mm a", "H:mm a", "h:mm", "H:mm")) {
            val parsed = try {
                SimpleDateFormat(pattern, Locale.getDefault()).parse(raw.trim())
            } catch (_: Exception) {
                null
            } ?: continue
            val source = Calendar.getInstance().apply { time = parsed }
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, source.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, source.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        return null
    }

    private fun executeGetCalendarEvents(context: Context, intent: EvIntent.GetCalendarEvents): EvExecutionResult {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            val msg = "Calendar permission is required to read your events."
            return EvExecutionResult(false, msg, msg, intent)
        }
        return try {
            val now = System.currentTimeMillis()
            val end = now + 24 * 60 * 60 * 1000L
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN
            )
            val selection =
                "${CalendarContract.Instances.BEGIN} >= ? AND ${CalendarContract.Instances.BEGIN} <= ?"
            context.contentResolver.query(
                CalendarContract.Instances.CONTENT_URI,
                projection,
                selection,
                arrayOf(now.toString(), end.toString()),
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val items = mutableListOf<String>()
                while (cursor.moveToNext() && items.size < intent.limit) {
                    val title = cursor.getString(0)
                    val begin = cursor.getLong(1)
                    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(begin)
                    items.add("$title at $time")
                }
                val msg = if (items.isEmpty()) "You have no events in the next 24 hours."
                else "Upcoming events: " + items.joinToString("; ")
                return@use EvExecutionResult(true, msg, msg, intent)
            }
            val msg = "Could not read calendar events."
            EvExecutionResult(false, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not read calendar events: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Navigation
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeOpenMaps(context: Context, intent: EvIntent.OpenMaps): EvExecutionResult {
        val destination = intent.destination ?: ""
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(mapIntent)
            val msg = if (destination.isNotBlank()) "Opening maps to $destination..." else "Opening maps..."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not open maps: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    private fun executeGetDirections(context: Context, intent: EvIntent.GetDirections): EvExecutionResult {
        val destination = Uri.encode(intent.destination)
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination")
        val intentAction = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            context.startActivity(intentAction)
            val msg = "Getting directions to ${intent.destination}..."
            EvExecutionResult(true, msg, msg, intent)
        } catch (e: Exception) {
            val msg = "Could not get directions: ${e.localizedMessage ?: "unknown error"}"
            EvExecutionResult(false, msg, msg, intent)
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Conversation & Assistant
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeHelp(intent: EvIntent.Help): EvExecutionResult {
        val msg = "I can help you with alarms, timers, calls, messages, WhatsApp, apps, settings, weather, news, music, reminders, notes, calendar, maps, calculations, and more. Just ask!"
        return EvExecutionResult(true, msg, msg, intent)
    }

    private fun executeUnknown(intent: EvIntent.Unknown): EvExecutionResult {
        // Bixby-style: a plain "I didn't understand that." with no lecture and no
        // "Try: ..." list. Genuinely invalid commands just get a short no.
        val msg = "I didn't understand that."
        return EvExecutionResult(false, msg, msg, intent, suggestions = emptyList())
    }

    // ════════════════════════════════════════════════════════════════════════════
    // System
    // ════════════════════════════════════════════════════════════════════════════

    private fun executeShutdown(context: Context, intent: EvIntent.Shutdown): EvExecutionResult {
        if (!intent.confirm) {
            val msg = "Are you sure you want to shut down? Say confirm shutdown."
            return EvExecutionResult(false, msg, msg, intent)
        }
        return executeRootAction(context, intent, "Shutting down...") {
            if (EvRoot.run("svc power shutdown") || EvRoot.run("reboot -p")) {
                "Shutting down..."
            } else {
                "Shutting down requires root access on this device."
            }
        }
    }

    private fun executeRestart(context: Context): EvExecutionResult {
        return executeRootAction(context, EvIntent.Restart, "Restarting the device...") {
            if (EvRoot.run("reboot")) {
                "Restarting the device..."
            } else {
                "Restarting requires root access on this device."
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Resolves a spoken contact name to a phone number.
     *
     * The lookup is deliberately ranked rather than "first row wins": an unanchored
     * `LIKE '%name%'` can match several contacts whose name merely *contains* the
     * query (e.g. "mom" also matches "Ramon", "Momo", "Momina"). Without ranking,
     * the ContactsProvider's row order decides the result, so the wrong person gets
     * called. We therefore score every candidate and prefer exact > prefix > substring.
     */
    private fun resolveContactNumber(context: Context, target: String): String? {
        val query = stripDialKeyword(target).trim()
        if (query.isEmpty() || !query.any { it.isLetter() }) return null
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val projection = arrayOf(
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        // Escape LIKE wildcards so characters such as % or _ in a name are literal.
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val selection =
            "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
        val selectionArgs = arrayOf("%$escaped%")

        var bestNumber: String? = null
        var bestScore = 0
        context.contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val numberIdx = cursor.getColumnIndexOrThrow(
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIdx) ?: continue
                val number = cursor.getString(numberIdx)
                if (number.isNullOrBlank()) continue
                val score = contactMatchScore(displayName, query)
                if (score >= 3) return number // exact match — cannot do better
                if (score > bestScore) {
                    bestScore = score
                    bestNumber = number
                }
            }
        }
        return bestNumber
    }

    /** Exact (case-insensitive) match = 3, prefix = 2, substring = 1, otherwise 0. */
    internal fun contactMatchScore(displayName: String, query: String): Int {
        val name = displayName.trim()
        return when {
            name.equals(query, ignoreCase = true) -> 3
            name.startsWith(query, ignoreCase = true) -> 2
            name.contains(query, ignoreCase = true) -> 1
            else -> 0
        }
    }

    /**
     * Returns a dialable number string if [input] is a spoken phone number
     * (optionally prefixed with a command word like "call" or "dial"), or null when
     * the input is a name. This is what prevents a contact name from being dialed
     * literally as `tel:mom`.
     */
    internal fun normalizePhoneNumber(input: String): String? {
        val stripped = stripDialKeyword(input).trim()
        if (stripped.isEmpty() || stripped.any { it.isLetter() }) return null
        val digits = stripped.filter { it.isDigit() }
        if (digits.length < 3) return null
        return if (stripped.startsWith("+")) "+$digits" else digits
    }

    /** Removes a leading command keyword ("call mom" -> "mom", "dial 5551234" -> "5551234"). */
    private fun stripDialKeyword(input: String): String {
        return input.trim().replace(
            Regex("^(?:call|dial|phone|ring|text|sms|message|whatsapp|email|mail|to)\\s+", RegexOption.IGNORE_CASE),
            ""
        )
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return buildString {
            if (hours > 0) append("$hours hour${if (hours > 1) "s" else ""} ")
            if (minutes > 0) append("$minutes minute${if (minutes > 1) "s" else ""} ")
            if (secs > 0 && hours == 0) append("$secs second${if (secs > 1) "s" else ""}")
        }.trim()
    }

    private fun coerceIn(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(max, value))
}
package com.ev.ai.assistant.nlu

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.Random

/**
 * Generates the small-talk replies for "who are you", "how are you", greetings and
 * thanks.
 *
 * These replies are deliberately never fixed strings: EV is a *mobile* assistant, so
 * how it is doing is reported from the actual state of the phone — battery level,
 * whether it is charging, how warm it is, free storage, connectivity and the time of
 * day. A fresh [Random] seed is used on every call so repeated questions get varied
 * answers instead of the same sentence every time.
 *
 * Every read is guarded, so a missing battery broadcast, a denied permission or a
 * unit test's null [Context] degrades to a sensible line instead of throwing.
 */
object PersonaResponder {

    /** Snapshot of the bits of device state the replies talk about. */
    private data class DeviceState(
        val batteryPct: Int = -1,
        val charging: Boolean = false,
        val full: Boolean = false,
        val thermal: Int = PowerManager.THERMAL_STATUS_NONE,
        val freeStorageGb: Double = -1.0,
        val connected: Boolean = false,
        val networkType: String? = null
    )

    // ── Public API ────────────────────────────────────────────────────────

    /** "who are you" / "what are you" / "who r u" — identity, tinted by the phone's mood. */
    fun whoAreYou(context: Context?): String {
        val state = readState(context)
        val openers = listOf(
            "I'm E.V., your personal AI assistant running entirely on your device.",
            "E.V. here — a fully on-device assistant, no cloud required.",
            "I'm E.V. I live on this phone and work without sending your voice anywhere.",
            "The name's E.V. I run locally on your handset, so I'm always close by."
        )
        val tails = listOf(
            "Right now I'm ${moodPhrase(state)} — ${batteryPhrase(state)}.",
            "Battery's at ${batteryPhrase(state)} right now.",
            "Right now I'm ${networkPhrase(state)} and ${thermalPhrase(state)}.",
            "Plenty of room to work — ${storagePhrase(state)}."
        )
        return "${pick(openers, state)} ${pick(tails, state)}"
    }

    /** "how are you" — answered from the phone's real condition. */
    fun howAreYou(context: Context?): String {
        val state = readState(context)
        val openers = listOf(
            "I'm ${moodPhrase(state)}.",
            "Feeling ${moodPhrase(state)} today.",
            "Doing ${moodPhrase(state)}, thanks for asking.",
            "All good on my end — ${moodPhrase(state)}."
        )
        val details = listOf(
            "${batteryPhrase(state)}, ${thermalPhrase(state)}.",
            "${networkPhrase(state)} and ${storagePhrase(state)}.",
            batteryDetail(state),
            "Battery's at ${batteryPhrase(state)} and ${thermalPhrase(state)}."
        )
        return "${pick(openers, state)} ${pick(details, state)}"
    }

    /** Generic greeting, varies with the time of day and how the phone is doing. */
    fun greeting(context: Context?): String {
        val state = readState(context)
        val openers = listOf(
            timeGreeting(),
            "${timeGreeting()} E.V. speaking.",
            "Hey — ${timeGreeting().lowercase(Locale.ROOT)}",
            "Hello! ${timeGreeting()}"
        )
        val tails = listOf(
            "How can I help?",
            "What would you like to do?",
            "Battery's at ${batteryPhrase(state)} if you were wondering.",
            "${storagePhrase(state)}."
        )
        return "${pick(openers, state)} ${pick(tails, state)}"
    }

    /** "thanks" — short, varied acknowledgement. */
    fun thanks(context: Context?): String {
        val state = readState(context)
        val replies = listOf(
            "You're welcome!",
            "Anytime — that's what I'm here for.",
            "Happy to help.",
            "No problem at all.",
            "Always happy to help.",
            "Of course!"
        )
        val tails = listOf(
            "",
            " Anything else?",
            " Battery's at ${batteryPhrase(state)} while we're talking.",
            " Say the word and I'll get the next thing done."
        )
        return pick(replies, state) + pick(tails, state)
    }

    // ── Device state ──────────────────────────────────────────────────────

    private fun readState(context: Context?): DeviceState {
        if (context == null) return DeviceState()
        val app = context.applicationContext
        return try {
            val (pct, charging, full) = readBattery(app)
            val connected = readConnectivity(app)
            DeviceState(
                batteryPct = pct,
                charging = charging,
                full = full,
                thermal = readThermal(app),
                freeStorageGb = readFreeStorage(app),
                connected = connected.first,
                networkType = connected.second
            )
        } catch (_: Exception) {
            DeviceState()
        }
    }

    private fun readBattery(context: Context): Triple<Int, Boolean, Boolean> {
        return try {
            val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return Triple(-1, false, false)
            val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val full = status == BatteryManager.BATTERY_STATUS_FULL ||
                (pct in 99..100 && !charging)
            Triple(pct, charging, full)
        } catch (_: Exception) {
            Triple(-1, false, false)
        }
    }

    /** PowerManager thermal status constant (THERMAL_STATUS_*), or NONE when unknown. */
    private fun readThermal(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            } else {
                PowerManager.THERMAL_STATUS_NONE
            }
        } catch (_: Exception) {
            PowerManager.THERMAL_STATUS_NONE
        }
    }

    private fun readFreeStorage(context: Context): Double {
        return try {
            val stat = StatFs(File(context.filesDir, ".").path)
            val free = stat.availableBytes
            if (free <= 0) -1.0 else free / 1_000_000_000.0
        } catch (_: Exception) {
            -1.0
        }
    }

    private fun readConnectivity(context: Context): Pair<Boolean, String?> {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false to null
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false to null
            val type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "a VPN"
                else -> "a network"
            }
            (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) to type
        } catch (_: Exception) {
            false to null
        }
    }

    // ── Phrases ───────────────────────────────────────────────────────────

    private fun batteryPhrase(state: DeviceState): String = when {
        state.batteryPct < 0 -> "battery unknown"
        state.full -> "battery full at ${state.batteryPct} percent"
        state.charging && state.batteryPct >= 80 ->
            "charging at ${state.batteryPct} percent, nearly full"
        state.charging -> "charging at ${state.batteryPct} percent"
        state.batteryPct <= 15 -> "down to ${state.batteryPct} percent — you may want to charge soon"
        state.batteryPct <= 35 -> "${state.batteryPct} percent left"
        else -> "${state.batteryPct} percent"
    }

    private fun batteryDetail(state: DeviceState): String = when {
        state.batteryPct < 0 -> "I can't read the battery right now."
        state.charging -> "You're plugged in and charging, so I've got plenty of power to work with."
        state.batteryPct <= 15 ->
            "Heads up — only ${state.batteryPct} percent left. I'll keep things light."
        else -> "Sitting at a comfortable ${state.batteryPct} percent."
    }

    private fun thermalPhrase(state: DeviceState): String = when {
        state.thermal >= PowerManager.THERMAL_STATUS_CRITICAL -> "I'm running hot, so I'm throttling back"
        state.thermal >= PowerManager.THERMAL_STATUS_SEVERE -> "I'm getting pretty warm"
        state.thermal >= PowerManager.THERMAL_STATUS_MODERATE -> "I'm running a little warm"
        state.thermal >= PowerManager.THERMAL_STATUS_LIGHT -> "I'm slightly warm"
        else -> "I'm running cool"
    }

    private fun storagePhrase(state: DeviceState): String = when {
        state.freeStorageGb < 0 -> "storage status unknown"
        state.freeStorageGb < 1.0 ->
            "storage is nearly full, only ${state.freeStorageGb * 1000} MB free"
        else -> "${formatGb(state.freeStorageGb)} of storage free"
    }

    private fun networkPhrase(state: DeviceState): String = when {
        state.connected && state.networkType != null -> "connected over ${state.networkType}"
        state.connected -> "online"
        else -> "offline right now"
    }

    /** Overall "mood" derived from battery, heat and connectivity. */
    private fun moodPhrase(state: DeviceState): String = when {
        state.thermal >= PowerManager.THERMAL_STATUS_SEVERE -> "a bit overheated and taking it easy"
        state.batteryPct in 0..10 && !state.charging -> "running on fumes — I need a charger soon"
        state.batteryPct in 0..20 && !state.charging -> "getting low on battery"
        state.charging && state.batteryPct >= 95 -> "plugged in, fully charged and raring to go"
        state.charging -> "charging up and feeling good"
        !state.connected && state.batteryPct >= 0 -> "fine, just offline at the moment"
        state.batteryPct >= 60 -> "great"
        state.batteryPct >= 30 -> "pretty good"
        state.batteryPct < 0 -> "ready to go"
        else -> "doing okay"
    }

    private fun timeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 5 -> "Still up late, I see."
            hour < 12 -> "Good morning."
            hour < 17 -> "Good afternoon."
            hour < 21 -> "Good evening."
            else -> "Working late tonight."
        }
    }

    // ── Randomness helpers ────────────────────────────────────────────────

    /**
     * Picks a line using a generator seeded from the current time and the current
     * battery reading, so consecutive answers differ but repeats of the same device
     * state still vary.
     */
    private fun pick(options: List<String>, state: DeviceState): String {
        if (options.isEmpty()) return ""
        val seed = System.nanoTime() xor (state.batteryPct.toLong() shl 32) xor
            state.freeStorageGb.toRawBits()
        val random = Random(seed)
        return options[random.nextInt(options.size)]
    }

    private fun formatGb(gb: Double): String =
        if (gb >= 1.0) String.format(Locale.getDefault(), "%.1f GB", gb)
        else String.format(Locale.getDefault(), "%.0f MB", gb * 1000)
}

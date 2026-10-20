package com.ev.ai.assistant.nlu

import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around a superuser (`su`) shell.
 *
 * Several system actions (toggling Wi-Fi/Bluetooth/airplane mode, rebooting,
 * capturing the screen) are not exposed to ordinary apps by the public Android
 * APIs. When the device is rooted we perform them through `su`; when it is not,
 * callers fall back to opening the relevant system settings screen.
 */
object EvRoot {

    private const val TAG = "EvRoot"

    @Volatile private var checked = false
    @Volatile private var granted = false

    /** Whether a working root shell is available. Result is cached after the first check. */
    fun isAvailable(): Boolean {
        if (checked) return granted
        synchronized(this) {
            if (checked) return granted
            granted = try {
                val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor(8, TimeUnit.SECONDS)
                output.contains("uid=0")
            } catch (e: Exception) {
                Log.w(TAG, "Root check failed: ${e.message}")
                false
            }
            checked = true
            return granted
        }
    }

    /** Runs [command] as root. Returns true when the shell exits successfully. */
    fun run(command: String, timeoutMs: Long = 8000): Boolean {
        if (!isAvailable()) return false
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.w(TAG, "Root command failed ($command): ${e.message}")
            false
        }
    }
}

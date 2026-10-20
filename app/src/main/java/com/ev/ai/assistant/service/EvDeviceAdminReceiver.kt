package com.ev.ai.assistant.service

import android.app.admin.DeviceAdminReceiver
import android.util.Log

/**
 * Device-administrator receiver that lets E.V. lock the screen on command without root.
 *
 * Android exposes [android.app.admin.DevicePolicyManager.lockNow] only to apps that the
 * user has explicitly granted administrator rights. Registering this receiver (plus the
 * force-lock policy in res/xml/ev_device_admin.xml) makes the grant possible from the
 * Settings screen E.V. opens when you say "lock screen".
 */
class EvDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: android.content.Context, intent: android.content.Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device administrator enabled: screen lock now available without root.")
    }

    override fun onDisabled(context: android.content.Context, intent: android.content.Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device administrator disabled: lock screen falls back to root.")
    }

    private companion object {
        const val TAG = "EvDeviceAdmin"
    }
}

package com.nova.ai.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.nova.ai.assistant.R
import com.nova.ai.assistant.core.runtime.LifecycleState
import com.nova.ai.assistant.core.runtime.RuntimeEvent
import com.nova.ai.assistant.core.runtime.RuntimeEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantForegroundService : Service() {
    private val binder = LocalBinder()
    private val _runtimeStatus = MutableStateFlow(RuntimeStatus.CREATED)
    val runtimeStatus: StateFlow<RuntimeStatus> = _runtimeStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_READY))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            _runtimeStatus.value = RuntimeStatus.STOPPED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundRuntime()
        _runtimeStatus.value = RuntimeStatus.RUNNING
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        _runtimeStatus.value = RuntimeStatus.STOPPED
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_STOPPED))
        super.onDestroy()
    }

    private fun startForegroundRuntime() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.assistant_notification_title))
            .setContentText(getString(R.string.assistant_notification_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.assistant_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.assistant_notification_channel_description)
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "assistant_runtime"
        const val ACTION_START = "com.nova.ai.assistant.action.SERVICE_START"
        const val ACTION_STOP = "com.nova.ai.assistant.action.SERVICE_STOP"
        private const val NOTIFICATION_ID = 1101
    }

    inner class LocalBinder : Binder() {
        fun getService(): AssistantForegroundService = this@AssistantForegroundService
    }

    enum class RuntimeStatus {
        CREATED,
        RUNNING,
        STOPPED
    }
}

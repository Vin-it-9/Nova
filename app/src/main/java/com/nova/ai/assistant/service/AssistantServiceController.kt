package com.nova.ai.assistant.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.nova.ai.assistant.core.runtime.LifecycleState
import com.nova.ai.assistant.core.runtime.RuntimeEvent
import com.nova.ai.assistant.core.runtime.RuntimeEventBus
import com.nova.ai.assistant.core.runtime.RuntimeServiceState
import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.core.runtime.ServiceRuntimeContract
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AssistantServiceController(
    context: Context,
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) : ServiceRuntimeContract {
    private val appContext = context.applicationContext
    private val _serviceState = MutableStateFlow(RuntimeServiceState.STOPPED)
    override val serviceState: StateFlow<RuntimeServiceState> = _serviceState.asStateFlow()

    @Volatile
    private var boundService: AssistantForegroundService? = null

    @Volatile
    private var serviceBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as? AssistantForegroundService.LocalBinder)?.getService()
            serviceBound = boundService != null
            _serviceState.value = if (serviceBound) RuntimeServiceState.BOUND else RuntimeServiceState.RUNNING
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_BOUND))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            serviceBound = false
            _serviceState.value = RuntimeServiceState.RUNNING
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_DISCONNECTED))
        }

        override fun onNullBinding(name: ComponentName?) {
            _serviceState.value = RuntimeServiceState.FAILED
            RuntimeEventBus.tryPublish(
                RuntimeEvent.Lifecycle(
                    state = LifecycleState.ERROR,
                    details = "service-null-binding"
                )
            )
        }

        override fun onBindingDied(name: ComponentName?) {
            boundService = null
            serviceBound = false
            _serviceState.value = RuntimeServiceState.FAILED
            RuntimeEventBus.tryPublish(
                RuntimeEvent.Lifecycle(
                    state = LifecycleState.ERROR,
                    details = "service-binding-died"
                )
            )
        }
    }

    override suspend fun startForegroundRuntime() = withContext(dispatcher) {
        if (_serviceState.value == RuntimeServiceState.RUNNING ||
            _serviceState.value == RuntimeServiceState.BOUND
        ) {
            return@withContext
        }

        _serviceState.value = RuntimeServiceState.STARTING
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_STARTING))

        val startIntent = Intent(appContext, AssistantForegroundService::class.java).apply {
            action = AssistantForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(appContext, startIntent)
        _serviceState.value = RuntimeServiceState.RUNNING
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_RUNNING))
    }

    override suspend fun bindRuntime() = withContext(dispatcher) {
        if (serviceBound) return@withContext

        _serviceState.value = RuntimeServiceState.BINDING
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_BINDING))

        val bindResult = appContext.bindService(
            Intent(appContext, AssistantForegroundService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        if (!bindResult) {
            _serviceState.value = RuntimeServiceState.FAILED
            RuntimeEventBus.tryPublish(
                RuntimeEvent.Lifecycle(
                    state = LifecycleState.ERROR,
                    details = "service-bind-failed"
                )
            )
        }
    }

    override suspend fun unbindRuntime() = withContext(dispatcher) {
        if (!serviceBound) return@withContext

        appContext.unbindService(connection)
        serviceBound = false
        boundService = null
        _serviceState.value = RuntimeServiceState.RUNNING
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_UNBOUND))
    }

    override suspend fun stopForegroundRuntime() = withContext(dispatcher) {
        if (_serviceState.value == RuntimeServiceState.STOPPED) return@withContext

        _serviceState.value = RuntimeServiceState.STOPPING
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_STOPPING))
        val stopIntent = Intent(appContext, AssistantForegroundService::class.java).apply {
            action = AssistantForegroundService.ACTION_STOP
        }
        appContext.startService(stopIntent)
        _serviceState.value = RuntimeServiceState.STOPPED
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.SERVICE_STOPPED))
    }

    fun getBoundService(): AssistantForegroundService? = boundService
}

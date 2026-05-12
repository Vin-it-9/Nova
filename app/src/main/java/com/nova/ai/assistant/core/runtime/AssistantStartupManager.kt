package com.nova.ai.assistant.core.runtime

import com.nova.ai.assistant.nativecore.NativeBridge
import com.nova.ai.assistant.service.AssistantServiceController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AssistantStartupManager(
    private val serviceController: AssistantServiceController,
    private val startupDispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) {
    private val _startupStep = MutableStateFlow(StartupStep.IDLE)
    val startupStep: StateFlow<StartupStep> = _startupStep.asStateFlow()

    suspend fun start(config: StartupConfig = StartupConfig()): Boolean =
        withContext(startupDispatcher) {
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.RUNTIME_STARTING))

            _startupStep.value = StartupStep.NATIVE_CORE
            if (!NativeBridge.nativeRuntimePing()) {
                _startupStep.value = StartupStep.FAILED
                RuntimeEventBus.tryPublish(
                    RuntimeEvent.Lifecycle(
                        state = LifecycleState.ERROR,
                        details = "native-runtime-unavailable"
                    )
                )
                return@withContext false
            }
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.NATIVE_READY))

            if (config.startService) {
                _startupStep.value = StartupStep.SERVICE_LAYER
                serviceController.startForegroundRuntime()
                if (config.bindService) {
                    serviceController.bindRuntime()
                }
            }

            _startupStep.value = StartupStep.VOICE_LAYER
            config.voiceInitializer()
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.VOICE_READY))

            _startupStep.value = StartupStep.INTELLIGENCE_LAYER
            config.intelligenceInitializer()
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.INTELLIGENCE_READY))

            _startupStep.value = StartupStep.READY
            RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.RUNTIME_READY))
            true
        }

    suspend fun stop(stopService: Boolean = false) = withContext(startupDispatcher) {
        if (stopService) {
            serviceController.unbindRuntime()
            serviceController.stopForegroundRuntime()
        }
        _startupStep.value = StartupStep.IDLE
        RuntimeEventBus.tryPublish(RuntimeEvent.Lifecycle(LifecycleState.RUNTIME_STOPPED))
    }

    data class StartupConfig(
        val startService: Boolean = false,
        val bindService: Boolean = false,
        val voiceInitializer: suspend () -> Unit = {},
        val intelligenceInitializer: suspend () -> Unit = {}
    )

    enum class StartupStep {
        IDLE,
        NATIVE_CORE,
        SERVICE_LAYER,
        VOICE_LAYER,
        INTELLIGENCE_LAYER,
        READY,
        FAILED
    }
}

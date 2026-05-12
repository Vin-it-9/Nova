package com.nova.ai.assistant.ui.visual.runtime

import com.nova.ai.assistant.core.AssistantState
import com.nova.ai.assistant.core.runtime.InferencePhase
import com.nova.ai.assistant.core.runtime.LifecycleState
import com.nova.ai.assistant.core.runtime.RuntimeEvent
import com.nova.ai.assistant.core.runtime.RuntimeEventBus
import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VisualStateCoordinator(
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var eventJob: Job? = null

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    private val _lifecycleState = MutableStateFlow(LifecycleState.RUNTIME_STOPPED)
    private val _nativeReady = MutableStateFlow(false)
    private val _visualState = MutableStateFlow<AssistantVisualState>(AssistantVisualState.Transitioning(
        from = AssistantVisualState.Semantic.IDLE,
        to = AssistantVisualState.Semantic.IDLE,
        reason = "initializing"
    ))

    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()
    val lifecycleState: StateFlow<LifecycleState> = _lifecycleState.asStateFlow()
    val visualState: StateFlow<AssistantVisualState> = _visualState.asStateFlow()

    fun start() {
        if (eventJob != null) return
        eventJob = scope.launch {
            RuntimeEventBus.observe().collect(::onRuntimeEvent)
        }
        recomputeVisualState()
    }

    fun stop() {
        eventJob?.cancel()
        eventJob = null
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    fun setNativeReady(ready: Boolean) {
        _nativeReady.value = ready
        recomputeVisualState()
    }

    fun updateAssistantState(state: AssistantState) {
        _assistantState.value = state
        recomputeVisualState()
    }

    private fun onRuntimeEvent(event: RuntimeEvent) {
        when (event) {
            is RuntimeEvent.Lifecycle -> {
                _lifecycleState.value = event.state
                if (event.state == LifecycleState.ERROR) {
                    _visualState.value = AssistantVisualState.Error(event.details ?: "runtime-error")
                    return
                }
            }

            is RuntimeEvent.UiSync -> _assistantState.value = event.state
            is RuntimeEvent.Wake -> {
                if (event.activated) {
                    _assistantState.value = AssistantState.LISTENING
                }
            }

            is RuntimeEvent.Inference -> {
                _assistantState.value = when (event.phase) {
                    InferencePhase.REQUESTED,
                    InferencePhase.STARTED -> AssistantState.PROCESSING

                    InferencePhase.COMPLETED -> AssistantState.IDLE
                    InferencePhase.FAILED -> AssistantState.PROCESSING
                }
            }

            is RuntimeEvent.Audio -> {
                if (_assistantState.value == AssistantState.IDLE) {
                    _assistantState.value = AssistantState.LISTENING
                }
            }

            is RuntimeEvent.Callback -> Unit
        }
        recomputeVisualState()
    }

    private fun recomputeVisualState() {
        val lifecycle = _lifecycleState.value
        val assistant = _assistantState.value

        if (!_nativeReady.value) {
            _visualState.value = AssistantVisualState.Transitioning(
                from = AssistantVisualState.Semantic.IDLE,
                to = AssistantVisualState.Semantic.PROCESSING,
                reason = "native-initializing"
            )
            return
        }

        _visualState.value = when (lifecycle) {
            LifecycleState.RUNTIME_STARTING,
            LifecycleState.SERVICE_STARTING,
            LifecycleState.SERVICE_BINDING -> AssistantVisualState.Transitioning(
                from = AssistantVisualState.Semantic.IDLE,
                to = AssistantVisualState.Semantic.PROCESSING,
                reason = "runtime-starting"
            )

            LifecycleState.SERVICE_STOPPING -> AssistantVisualState.Transitioning(
                from = AssistantVisualState.Semantic.PROCESSING,
                to = AssistantVisualState.Semantic.IDLE,
                reason = "runtime-stopping"
            )

            LifecycleState.ERROR -> AssistantVisualState.Error()
            else -> mapAssistantState(assistant)
        }
    }

    private fun mapAssistantState(state: AssistantState): AssistantVisualState = when (state) {
        AssistantState.IDLE -> AssistantVisualState.Idle
        AssistantState.LISTENING -> AssistantVisualState.Listening
        AssistantState.PROCESSING -> AssistantVisualState.Processing
        AssistantState.SPEAKING -> AssistantVisualState.Speaking
    }
}

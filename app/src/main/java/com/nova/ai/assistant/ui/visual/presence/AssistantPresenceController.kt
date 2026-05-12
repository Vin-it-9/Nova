package com.nova.ai.assistant.ui.visual.presence

import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AssistantPresenceController(
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) {
    private val _profile = MutableStateFlow(PresenceProfile())
    val profile: StateFlow<PresenceProfile> = _profile.asStateFlow()

    suspend fun onVisualState(state: AssistantVisualState) = withContext(dispatcher) {
        _profile.value = when (state.semantic) {
            AssistantVisualState.Semantic.IDLE -> PresenceProfile(
                breathingAmplitude = 0.024f,
                breathingPeriodMillis = 2400,
                driftAmplitude = 0.015f,
                liveliness = 0.3f
            )

            AssistantVisualState.Semantic.LISTENING -> PresenceProfile(
                breathingAmplitude = 0.04f,
                breathingPeriodMillis = 980,
                driftAmplitude = 0.03f,
                liveliness = 0.72f
            )

            AssistantVisualState.Semantic.PROCESSING -> PresenceProfile(
                breathingAmplitude = 0.028f,
                breathingPeriodMillis = 1240,
                driftAmplitude = 0.022f,
                liveliness = 0.6f
            )

            AssistantVisualState.Semantic.SPEAKING -> PresenceProfile(
                breathingAmplitude = 0.048f,
                breathingPeriodMillis = 720,
                driftAmplitude = 0.034f,
                liveliness = 0.88f
            )

            AssistantVisualState.Semantic.ERROR -> PresenceProfile(
                breathingAmplitude = 0.018f,
                breathingPeriodMillis = 640,
                driftAmplitude = 0.038f,
                liveliness = 0.52f
            )

            AssistantVisualState.Semantic.TRANSITIONING -> PresenceProfile(
                breathingAmplitude = 0.03f,
                breathingPeriodMillis = 1120,
                driftAmplitude = 0.026f,
                liveliness = 0.66f
            )
        }
    }
}

data class PresenceProfile(
    val breathingAmplitude: Float = 0.024f,
    val breathingPeriodMillis: Int = 2400,
    val driftAmplitude: Float = 0.015f,
    val liveliness: Float = 0.3f
)

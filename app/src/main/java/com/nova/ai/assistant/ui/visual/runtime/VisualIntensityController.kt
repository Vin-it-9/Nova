package com.nova.ai.assistant.ui.visual.runtime

import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class VisualIntensityController(
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) {
    private val _profile = MutableStateFlow(VisualIntensityProfile())
    val profile: StateFlow<VisualIntensityProfile> = _profile.asStateFlow()

    suspend fun onVisualState(state: AssistantVisualState) = withContext(dispatcher) {
        val target = when (state.semantic) {
            AssistantVisualState.Semantic.IDLE -> VisualIntensityProfile(
                level = 0.24f,
                glowBoost = 0.06f,
                waveformGain = 0.18f,
                transitionEnergy = 0.2f
            )

            AssistantVisualState.Semantic.LISTENING -> VisualIntensityProfile(
                level = 0.55f,
                glowBoost = 0.2f,
                waveformGain = 0.65f,
                transitionEnergy = 0.56f
            )

            AssistantVisualState.Semantic.PROCESSING -> VisualIntensityProfile(
                level = 0.48f,
                glowBoost = 0.16f,
                waveformGain = 0.4f,
                transitionEnergy = 0.5f
            )

            AssistantVisualState.Semantic.SPEAKING -> VisualIntensityProfile(
                level = 0.72f,
                glowBoost = 0.25f,
                waveformGain = 0.84f,
                transitionEnergy = 0.68f
            )

            AssistantVisualState.Semantic.ERROR -> VisualIntensityProfile(
                level = 0.44f,
                glowBoost = 0.1f,
                waveformGain = 0.28f,
                transitionEnergy = 0.74f
            )

            AssistantVisualState.Semantic.TRANSITIONING -> VisualIntensityProfile(
                level = 0.52f,
                glowBoost = 0.18f,
                waveformGain = 0.45f,
                transitionEnergy = 0.85f
            )
        }

        _profile.value = _profile.value.interpolateTowards(target, factor = 0.42f)
    }

    suspend fun applyRealtimeSignal(signal: Float) = withContext(dispatcher) {
        val clamped = signal.coerceIn(0f, 1f)
        _profile.value = _profile.value.copy(
            level = blend(_profile.value.level, max(_profile.value.level, clamped), 0.28f),
            glowBoost = min(0.35f, blend(_profile.value.glowBoost, clamped * 0.32f, 0.25f)),
            waveformGain = min(1f, blend(_profile.value.waveformGain, clamped, 0.32f))
        )
    }

    private fun VisualIntensityProfile.interpolateTowards(
        target: VisualIntensityProfile,
        factor: Float
    ): VisualIntensityProfile = VisualIntensityProfile(
        level = blend(level, target.level, factor),
        glowBoost = blend(glowBoost, target.glowBoost, factor),
        waveformGain = blend(waveformGain, target.waveformGain, factor),
        transitionEnergy = blend(transitionEnergy, target.transitionEnergy, factor)
    )

    private fun blend(current: Float, target: Float, factor: Float): Float =
        current + ((target - current) * factor)
}

data class VisualIntensityProfile(
    val level: Float = 0.24f,
    val glowBoost: Float = 0.06f,
    val waveformGain: Float = 0.18f,
    val transitionEnergy: Float = 0.2f
)

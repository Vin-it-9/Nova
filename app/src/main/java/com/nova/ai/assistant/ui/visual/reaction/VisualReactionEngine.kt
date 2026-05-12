package com.nova.ai.assistant.ui.visual.reaction

import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.ui.visual.presence.PresenceProfile
import com.nova.ai.assistant.ui.visual.runtime.VisualIntensityProfile
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class VisualReactionEngine(
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) {
    private var currentState: AssistantVisualState = AssistantVisualState.Idle
    private var currentIntensity: VisualIntensityProfile = VisualIntensityProfile()
    private var currentPresence: PresenceProfile = PresenceProfile()

    private val _reaction = MutableStateFlow(buildReaction())
    val reaction: StateFlow<VisualReactionModel> = _reaction.asStateFlow()

    suspend fun onVisualState(state: AssistantVisualState) = withContext(dispatcher) {
        currentState = state
        _reaction.value = buildReaction()
    }

    suspend fun onIntensity(profile: VisualIntensityProfile) = withContext(dispatcher) {
        currentIntensity = profile
        _reaction.value = buildReaction()
    }

    suspend fun onPresence(profile: PresenceProfile) = withContext(dispatcher) {
        currentPresence = profile
        _reaction.value = buildReaction()
    }

    private fun buildReaction(): VisualReactionModel {
        val intensity = currentIntensity.level.coerceIn(0f, 1f)
        val presence = currentPresence.liveliness.coerceIn(0f, 1f)

        val orb = when (currentState.semantic) {
            AssistantVisualState.Semantic.IDLE -> OrbReactionModel(
                scaleMultiplier = 1f + (currentPresence.breathingAmplitude * 0.35f),
                breathingAmplitude = currentPresence.breathingAmplitude,
                pulseAmplitude = 0.012f,
                glowBoost = currentIntensity.glowBoost * 0.65f,
                instability = 0f,
                alpha = 0.94f,
                pulseSpeedHz = 0.34f
            )

            AssistantVisualState.Semantic.LISTENING -> OrbReactionModel(
                scaleMultiplier = 1.04f + (intensity * 0.06f),
                breathingAmplitude = currentPresence.breathingAmplitude,
                pulseAmplitude = 0.032f + (intensity * 0.03f),
                glowBoost = 0.14f + currentIntensity.glowBoost,
                instability = 0.005f,
                alpha = 0.98f,
                pulseSpeedHz = 0.92f
            )

            AssistantVisualState.Semantic.PROCESSING -> OrbReactionModel(
                scaleMultiplier = 1.03f + (presence * 0.03f),
                breathingAmplitude = currentPresence.breathingAmplitude * 0.82f,
                pulseAmplitude = 0.022f + (intensity * 0.02f),
                glowBoost = 0.12f + currentIntensity.glowBoost,
                instability = 0.01f,
                alpha = 0.97f,
                pulseSpeedHz = 0.78f
            )

            AssistantVisualState.Semantic.SPEAKING -> OrbReactionModel(
                scaleMultiplier = 1.08f + (intensity * 0.07f),
                breathingAmplitude = currentPresence.breathingAmplitude * 0.58f,
                pulseAmplitude = 0.04f + (intensity * 0.045f),
                glowBoost = 0.2f + currentIntensity.glowBoost,
                instability = 0.008f,
                alpha = 1f,
                pulseSpeedHz = 1.18f
            )

            AssistantVisualState.Semantic.ERROR -> OrbReactionModel(
                scaleMultiplier = 0.985f,
                breathingAmplitude = 0.01f,
                pulseAmplitude = 0.014f,
                glowBoost = 0.05f + (currentIntensity.glowBoost * 0.5f),
                instability = 0.028f,
                alpha = 0.94f,
                pulseSpeedHz = 1.5f
            )

            AssistantVisualState.Semantic.TRANSITIONING -> OrbReactionModel(
                scaleMultiplier = 1.02f,
                breathingAmplitude = currentPresence.breathingAmplitude * 0.72f,
                pulseAmplitude = 0.025f + (currentIntensity.transitionEnergy * 0.02f),
                glowBoost = 0.11f + currentIntensity.glowBoost,
                instability = 0.012f,
                alpha = 0.96f,
                pulseSpeedHz = 0.9f
            )
        }

        val waveform = when (currentState.semantic) {
            AssistantVisualState.Semantic.IDLE -> WaveformReactionModel(
                amplitude = 0.08f + (presence * 0.05f),
                speed = 0.24f,
                lineThickness = 2f,
                alpha = 0.18f,
                softness = 0.84f
            )

            AssistantVisualState.Semantic.LISTENING -> WaveformReactionModel(
                amplitude = min(0.7f, 0.25f + currentIntensity.waveformGain * 0.55f),
                speed = 0.72f,
                lineThickness = 2.4f,
                alpha = 0.52f,
                softness = 0.72f
            )

            AssistantVisualState.Semantic.PROCESSING -> WaveformReactionModel(
                amplitude = 0.2f + (currentIntensity.waveformGain * 0.34f),
                speed = 0.6f,
                lineThickness = 2.1f,
                alpha = 0.36f,
                softness = 0.74f
            )

            AssistantVisualState.Semantic.SPEAKING -> WaveformReactionModel(
                amplitude = 0.35f + (currentIntensity.waveformGain * 0.48f),
                speed = 0.96f,
                lineThickness = 2.6f,
                alpha = 0.62f,
                softness = 0.66f
            )

            AssistantVisualState.Semantic.ERROR -> WaveformReactionModel(
                amplitude = 0.14f,
                speed = 1.2f,
                lineThickness = 2.2f,
                alpha = 0.24f,
                softness = 0.5f
            )

            AssistantVisualState.Semantic.TRANSITIONING -> WaveformReactionModel(
                amplitude = 0.2f + (currentIntensity.transitionEnergy * 0.28f),
                speed = 0.78f,
                lineThickness = 2.2f,
                alpha = 0.46f,
                softness = 0.68f
            )
        }

        return VisualReactionModel(
            orb = orb,
            waveform = waveform,
            statusAlpha = max(0.72f, min(1f, 0.75f + (presence * 0.2f))),
            immersiveDepth = 0.88f + (intensity * 0.12f)
        )
    }
}

data class VisualReactionModel(
    val orb: OrbReactionModel,
    val waveform: WaveformReactionModel,
    val statusAlpha: Float,
    val immersiveDepth: Float
)

data class OrbReactionModel(
    val scaleMultiplier: Float,
    val breathingAmplitude: Float,
    val pulseAmplitude: Float,
    val glowBoost: Float,
    val instability: Float,
    val alpha: Float,
    val pulseSpeedHz: Float
)

data class WaveformReactionModel(
    val amplitude: Float,
    val speed: Float,
    val lineThickness: Float,
    val alpha: Float,
    val softness: Float
)

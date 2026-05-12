package com.nova.ai.assistant.ui.visual.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import com.nova.ai.assistant.ui.visual.motion.MotionProfile
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState

class AssistantAnimationEngine {
    fun targetFor(
        state: AssistantVisualState,
        profile: MotionProfile
    ): AssistantAnimationTarget = when (state.semantic) {
        AssistantVisualState.Semantic.IDLE -> AssistantAnimationTarget(
            orbScale = 1f,
            orbAlpha = 0.95f,
            glowIntensity = 0.14f,
            backgroundDepth = 0.92f,
            labelAlpha = 0.78f,
            orbDiameterDp = 168f,
            pulseAmplitude = profile.pulseAmplitude
        )

        AssistantVisualState.Semantic.LISTENING -> AssistantAnimationTarget(
            orbScale = 1.06f,
            orbAlpha = 0.98f,
            glowIntensity = 0.3f,
            backgroundDepth = 1f,
            labelAlpha = 0.9f,
            orbDiameterDp = 176f,
            pulseAmplitude = profile.pulseAmplitude
        )

        AssistantVisualState.Semantic.PROCESSING -> AssistantAnimationTarget(
            orbScale = 1.03f,
            orbAlpha = 0.96f,
            glowIntensity = 0.24f,
            backgroundDepth = 0.97f,
            labelAlpha = 0.88f,
            orbDiameterDp = 172f,
            pulseAmplitude = profile.pulseAmplitude
        )

        AssistantVisualState.Semantic.SPEAKING -> AssistantAnimationTarget(
            orbScale = 1.1f,
            orbAlpha = 1f,
            glowIntensity = 0.35f,
            backgroundDepth = 1f,
            labelAlpha = 0.95f,
            orbDiameterDp = 180f,
            pulseAmplitude = profile.pulseAmplitude
        )

        AssistantVisualState.Semantic.ERROR -> AssistantAnimationTarget(
            orbScale = 0.98f,
            orbAlpha = 0.94f,
            glowIntensity = 0.12f,
            backgroundDepth = 0.85f,
            labelAlpha = 0.94f,
            orbDiameterDp = 164f,
            pulseAmplitude = profile.pulseAmplitude
        )

        AssistantVisualState.Semantic.TRANSITIONING -> AssistantAnimationTarget(
            orbScale = 1.02f,
            orbAlpha = 0.96f,
            glowIntensity = 0.2f,
            backgroundDepth = 0.94f,
            labelAlpha = 0.86f,
            orbDiameterDp = 170f,
            pulseAmplitude = profile.pulseAmplitude
        )
    }

    fun springFor(profile: MotionProfile): SpringSpec<Float> = spring(
        dampingRatio = profile.dampingRatio,
        stiffness = profile.stiffness,
        visibilityThreshold = Spring.DefaultDisplacementThreshold
    )
}

@Immutable
data class AssistantAnimationTarget(
    val orbScale: Float,
    val orbAlpha: Float,
    val glowIntensity: Float,
    val backgroundDepth: Float,
    val labelAlpha: Float,
    val orbDiameterDp: Float,
    val pulseAmplitude: Float
)

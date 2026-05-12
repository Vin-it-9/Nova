package com.nova.ai.assistant.ui.visual.motion

import com.nova.ai.assistant.ui.visual.state.AssistantVisualState

class MotionCoordinator {
    fun profileFor(state: AssistantVisualState): MotionProfile = when (state.semantic) {
        AssistantVisualState.Semantic.IDLE -> MotionProfile(
            stiffness = 190f,
            dampingRatio = 0.95f,
            pulsePeriodMillis = 1800,
            pulseAmplitude = 0.02f
        )

        AssistantVisualState.Semantic.LISTENING -> MotionProfile(
            stiffness = 220f,
            dampingRatio = 0.82f,
            pulsePeriodMillis = 780,
            pulseAmplitude = 0.05f
        )

        AssistantVisualState.Semantic.PROCESSING -> MotionProfile(
            stiffness = 210f,
            dampingRatio = 0.86f,
            pulsePeriodMillis = 980,
            pulseAmplitude = 0.035f
        )

        AssistantVisualState.Semantic.SPEAKING -> MotionProfile(
            stiffness = 240f,
            dampingRatio = 0.8f,
            pulsePeriodMillis = 620,
            pulseAmplitude = 0.06f
        )

        AssistantVisualState.Semantic.ERROR -> MotionProfile(
            stiffness = 260f,
            dampingRatio = 0.92f,
            pulsePeriodMillis = 1400,
            pulseAmplitude = 0.015f
        )

        AssistantVisualState.Semantic.TRANSITIONING -> MotionProfile(
            stiffness = 230f,
            dampingRatio = 0.88f,
            pulsePeriodMillis = 900,
            pulseAmplitude = 0.03f
        )
    }

    fun frameTimestampNanos(): Long = System.nanoTime()
}

data class MotionProfile(
    val stiffness: Float,
    val dampingRatio: Float,
    val pulsePeriodMillis: Int,
    val pulseAmplitude: Float
)

package com.ev.ai.assistant.ui.util

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Motion tokens used by EV's One UI-inspired interactions.
 *
 * The cubic-bezier is the shared screen-transition curve. The spring values
 * are intentionally centralized so press feedback and controls remain
 * consistent across the app.
 */
object OneUiMotion {
    const val ScreenDurationMillis = 280
    const val ShortDurationMillis = 180
    const val StandardDurationMillis = 280
    const val EmphasizedDurationMillis = 420

    const val ScreenIncomingOffsetFraction = 0.20f
    const val ScreenOutgoingScale = 0.95f
    const val ListeningPulseDurationMillis = 900

    val StandardEasing = CubicBezierEasing(0.22f, 0.25f, 0f, 1f)
    val PressSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 650f
    )
    val ControlSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 400f
    )

    fun <T> screenSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = ScreenDurationMillis, easing = StandardEasing)

    fun <T> shortSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = ShortDurationMillis, easing = StandardEasing)

    @Composable
    fun rememberListeningPulse(): ListeningPulse {
        val transition = rememberInfiniteTransition(label = "one_ui_listening_pulse")
        val scale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.18f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ListeningPulseDurationMillis,
                    easing = StandardEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "one_ui_listening_scale"
        )
        val alpha = transition.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.48f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = ListeningPulseDurationMillis,
                    easing = StandardEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "one_ui_listening_alpha"
        )
        return ListeningPulse(scale = scale, alpha = alpha)
    }
}

data class ListeningPulse(
    val scale: State<Float>,
    val alpha: State<Float>
)

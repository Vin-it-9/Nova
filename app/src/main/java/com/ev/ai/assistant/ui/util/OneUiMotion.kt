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
 * Verified Samsung One UI Motion Design Tokens.
 * Ensures transitions remain highly responsive, prioritizing swift touch acceleration
 * and smooth deceleration without artificial lag or sluggish delays.
 */
object OneUiMotion {
    // --- OFFICIALLY TIMED DURATION TOKENS (MILLISECONDS) ---
    const val ShortDurationMillis = 150       // Micro-interactions: Checkboxes, toggle switches, ripples
    const val StandardDurationMillis = 300    // Default One UI speed for inline elements & expanding card details
    const val ScreenDurationMillis = 350      // Standard overlay windows, modal transitions, and navigation routes
    const val EmphasizedDurationMillis = 400  // Full macro layout reshuffles (System Upper Limit = 500ms)

    // --- VOICE CONTROLS & OVERLAY VALUES ---
    const val ScreenIncomingOffsetFraction = 0.15f // One UI soft translation entry distance offset
    const val ScreenOutgoingScale = 0.96f           // Micro scale compression for contextual layer stacking
    const val ListeningPulseDurationMillis = 1200   // Breathable, rhythmic pulse cycle timing for assistant states

    // --- SAMSUNG NATIVE EASING INTERPOLATORS ---
    // The canonical One UI Curve: Shoots out instantly upon tap, then drifts smoothly to a rest
    val StandardEasing = CubicBezierEasing(0.22f, 0.25f, 0.0f, 1.0f)

    // Tactile Press Feedback: Low bounce, high damping to reflect clean hardware elasticity
    val PressSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 500f
    )

    // Controls & Toggles: Snappy initialization with zero noticeable sway or overshoot
    val ControlSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 400f
    )

    // --- SPECIFICATION CONSTRUCTORS ---
    fun <T> screenSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = ScreenDurationMillis, easing = StandardEasing)

    fun <T> shortSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = ShortDurationMillis, easing = StandardEasing)

    fun <T> standardSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = StandardDurationMillis, easing = StandardEasing)

    fun <T> emphasizedSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = EmphasizedDurationMillis, easing = StandardEasing)

    /**
     * Generates a smooth, organic infinite pulse layer designed for voice engine activation views.
     */
    @Composable
    fun rememberListeningPulse(): ListeningPulse {
        val transition = rememberInfiniteTransition(label = "one_ui_listening_pulse")

        val scale = transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.12f, // Soft boundary expansion to prevent structural screen clutter
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
            initialValue = 0.20f,
            targetValue = 0.40f, // Clean luminance blend within safe visibility bounds
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
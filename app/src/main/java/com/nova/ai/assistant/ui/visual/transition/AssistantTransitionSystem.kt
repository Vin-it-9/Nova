package com.nova.ai.assistant.ui.visual.transition

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AssistantTransitionSystem(
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) {
    private val _state = MutableStateFlow(
        AssistantTransitionState(
            from = AssistantVisualState.Semantic.IDLE,
            to = AssistantVisualState.Semantic.IDLE
        )
    )
    val state: StateFlow<AssistantTransitionState> = _state.asStateFlow()

    suspend fun onVisualStateChanged(to: AssistantVisualState.Semantic) = withContext(dispatcher) {
        val current = _state.value
        if (current.to == to) return@withContext

        _state.value = AssistantTransitionState(
            from = current.to,
            to = to,
            overlayAlpha = overlayAlphaFor(to),
            orbScaleBoost = scaleBoostFor(to),
            waveformAlphaBoost = waveformBoostFor(to),
            depthBlurRadius = depthBlurFor(to),
            timestampNanos = System.nanoTime()
        )
    }

    fun orbSpringSpec(stiffnessMultiplier: Float = 1f): SpringSpec<Float> = spring(
        dampingRatio = 0.86f,
        stiffness = 260f * stiffnessMultiplier
    )

    fun waveformSpringSpec(): SpringSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 210f
    )

    fun overlayFadeSpec(): TweenSpec<Float> = tween(durationMillis = 520)
    
    fun cinematicFadeSpec(): TweenSpec<Float> = tween(durationMillis = 850)

    private fun overlayAlphaFor(state: AssistantVisualState.Semantic): Float = when (state) {
        AssistantVisualState.Semantic.IDLE -> 0.03f
        AssistantVisualState.Semantic.LISTENING -> 0.08f
        AssistantVisualState.Semantic.PROCESSING -> 0.1f
        AssistantVisualState.Semantic.SPEAKING -> 0.12f
        AssistantVisualState.Semantic.ERROR -> 0.14f
        AssistantVisualState.Semantic.TRANSITIONING -> 0.16f
    }

    private fun scaleBoostFor(state: AssistantVisualState.Semantic): Float = when (state) {
        AssistantVisualState.Semantic.IDLE -> 0f
        AssistantVisualState.Semantic.LISTENING -> 0.024f
        AssistantVisualState.Semantic.PROCESSING -> 0.02f
        AssistantVisualState.Semantic.SPEAKING -> 0.035f
        AssistantVisualState.Semantic.ERROR -> -0.01f
        AssistantVisualState.Semantic.TRANSITIONING -> 0.03f
    }

    private fun waveformBoostFor(state: AssistantVisualState.Semantic): Float = when (state) {
        AssistantVisualState.Semantic.IDLE -> 0.18f
        AssistantVisualState.Semantic.LISTENING -> 0.65f
        AssistantVisualState.Semantic.PROCESSING -> 0.48f
        AssistantVisualState.Semantic.SPEAKING -> 0.82f
        AssistantVisualState.Semantic.ERROR -> 0.24f
        AssistantVisualState.Semantic.TRANSITIONING -> 0.56f
    }
    
    private fun depthBlurFor(state: AssistantVisualState.Semantic): Float = when (state) {
        AssistantVisualState.Semantic.IDLE -> 0f
        AssistantVisualState.Semantic.LISTENING -> 4f
        AssistantVisualState.Semantic.PROCESSING -> 2f
        AssistantVisualState.Semantic.SPEAKING -> 0f
        AssistantVisualState.Semantic.ERROR -> 8f
        AssistantVisualState.Semantic.TRANSITIONING -> 12f
    }
}

data class AssistantTransitionState(
    val from: AssistantVisualState.Semantic,
    val to: AssistantVisualState.Semantic,
    val overlayAlpha: Float = 0f,
    val orbScaleBoost: Float = 0f,
    val waveformAlphaBoost: Float = 0f,
    val depthBlurRadius: Float = 0f,
    val timestampNanos: Long = System.nanoTime()
)

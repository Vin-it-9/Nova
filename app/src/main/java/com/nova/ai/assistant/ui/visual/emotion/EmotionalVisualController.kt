package com.nova.ai.assistant.ui.visual.emotion

import com.nova.ai.assistant.ui.visual.state.AssistantVisualState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class EmotionalMood {
    CALM, ENERGETIC, ALERT, FOCUSED, ERROR
}

data class EmotionalProfile(
    val mood: EmotionalMood = EmotionalMood.CALM,
    val colorShiftBoost: Float = 0f,
    val warmth: Float = 0.5f // 0.0 cool, 1.0 warm
)

class EmotionalVisualController {
    private val _profile = MutableStateFlow(EmotionalProfile())
    val profile: StateFlow<EmotionalProfile> = _profile.asStateFlow()

    fun onVisualState(state: AssistantVisualState) {
        val target = when (state.semantic) {
            AssistantVisualState.Semantic.IDLE -> EmotionalProfile(EmotionalMood.CALM, 0f, 0.5f)
            AssistantVisualState.Semantic.LISTENING -> EmotionalProfile(EmotionalMood.ALERT, 0.2f, 0.6f)
            AssistantVisualState.Semantic.PROCESSING -> EmotionalProfile(EmotionalMood.FOCUSED, 0.1f, 0.4f)
            AssistantVisualState.Semantic.SPEAKING -> EmotionalProfile(EmotionalMood.ENERGETIC, 0.3f, 0.7f)
            AssistantVisualState.Semantic.ERROR -> EmotionalProfile(EmotionalMood.ERROR, 0.5f, 0.9f)
            AssistantVisualState.Semantic.TRANSITIONING -> EmotionalProfile(EmotionalMood.CALM, 0.1f, 0.5f)
        }
        // In a full implementation, we'd smoothly interpolate towards the target.
        // For now, snap to target.
        _profile.value = target
    }
}

@Composable
fun rememberEmotionalVisualController(): EmotionalVisualController {
    return remember { EmotionalVisualController() }
}

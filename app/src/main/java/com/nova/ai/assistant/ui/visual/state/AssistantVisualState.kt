package com.nova.ai.assistant.ui.visual.state

import androidx.compose.runtime.Immutable

@Immutable
sealed interface AssistantVisualState {
    val semantic: Semantic
    val statusLabel: String

    @Immutable
    data object Idle : AssistantVisualState {
        override val semantic: Semantic = Semantic.IDLE
        override val statusLabel: String = "Idle"
    }

    @Immutable
    data object Listening : AssistantVisualState {
        override val semantic: Semantic = Semantic.LISTENING
        override val statusLabel: String = "Listening"
    }

    @Immutable
    data object Processing : AssistantVisualState {
        override val semantic: Semantic = Semantic.PROCESSING
        override val statusLabel: String = "Processing"
    }

    @Immutable
    data object Speaking : AssistantVisualState {
        override val semantic: Semantic = Semantic.SPEAKING
        override val statusLabel: String = "Speaking"
    }

    @Immutable
    data class Error(
        val reason: String = "runtime-error"
    ) : AssistantVisualState {
        override val semantic: Semantic = Semantic.ERROR
        override val statusLabel: String = "Error"
    }

    @Immutable
    data class Transitioning(
        val from: Semantic,
        val to: Semantic,
        val reason: String = "transition"
    ) : AssistantVisualState {
        override val semantic: Semantic = Semantic.TRANSITIONING
        override val statusLabel: String = "Transitioning"
    }

    enum class Semantic {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING,
        ERROR,
        TRANSITIONING
    }
}

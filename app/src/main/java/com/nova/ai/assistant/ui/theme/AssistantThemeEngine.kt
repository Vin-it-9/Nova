package com.nova.ai.assistant.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nova.ai.assistant.ui.visual.state.AssistantVisualState

class AssistantThemeEngine {
    fun paletteFor(state: AssistantVisualState): AssistantVisualPalette = when (state.semantic) {
        AssistantVisualState.Semantic.IDLE -> AssistantVisualPalette(
            backgroundTop = Color(0xFF05070B),
            backgroundBottom = Color(0xFF020204),
            orbInner = Color(0xFF8DABFF),
            orbOuter = Color(0xFF364A84),
            orbHalo = Color(0xFF9CB2FF),
            text = Color(0xFFE8ECFF)
        )

        AssistantVisualState.Semantic.LISTENING -> AssistantVisualPalette(
            backgroundTop = Color(0xFF071118),
            backgroundBottom = Color(0xFF02070A),
            orbInner = Color(0xFF84E3FF),
            orbOuter = Color(0xFF2D738A),
            orbHalo = Color(0xFF85E5FF),
            text = Color(0xFFE5FBFF)
        )

        AssistantVisualState.Semantic.PROCESSING -> AssistantVisualPalette(
            backgroundTop = Color(0xFF0C0A1A),
            backgroundBottom = Color(0xFF04020A),
            orbInner = Color(0xFFAF9BFF),
            orbOuter = Color(0xFF564596),
            orbHalo = Color(0xFFC0B2FF),
            text = Color(0xFFF0EFFF)
        )

        AssistantVisualState.Semantic.SPEAKING -> AssistantVisualPalette(
            backgroundTop = Color(0xFF0B101F),
            backgroundBottom = Color(0xFF030510),
            orbInner = Color(0xFF99D4FF),
            orbOuter = Color(0xFF3A5AA0),
            orbHalo = Color(0xFFB8DDFF),
            text = Color(0xFFEAF3FF)
        )

        AssistantVisualState.Semantic.ERROR -> AssistantVisualPalette(
            backgroundTop = Color(0xFF1A0B0B),
            backgroundBottom = Color(0xFF080303),
            orbInner = Color(0xFFFF9999),
            orbOuter = Color(0xFF8A3A3A),
            orbHalo = Color(0xFFFFB3B3),
            text = Color(0xFFFFECEC)
        )

        AssistantVisualState.Semantic.TRANSITIONING -> AssistantVisualPalette(
            backgroundTop = Color(0xFF0A0D14),
            backgroundBottom = Color(0xFF030407),
            orbInner = Color(0xFF9AA9D9),
            orbOuter = Color(0xFF46516E),
            orbHalo = Color(0xFFB0BAE1),
            text = Color(0xFFEAEFFF)
        )
    }
}

@Immutable
data class AssistantVisualPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val orbInner: Color,
    val orbOuter: Color,
    val orbHalo: Color,
    val text: Color
)

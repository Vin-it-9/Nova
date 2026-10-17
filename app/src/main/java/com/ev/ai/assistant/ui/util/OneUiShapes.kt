package com.ev.ai.assistant.ui.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * One UI-inspired corner-radius tokens.
 *
 * Use outer containers for screen-level grouping and inner containers for
 * controls/list content. Keeping these roles named prevents accidental radius
 * drift between screens.
 */
object OneUiShapes {
    val OuterContainer: Shape = RoundedCornerShape(26.dp)
    val InnerContainer: Shape = RoundedCornerShape(16.dp)
    val Card: Shape = RoundedCornerShape(20.dp)
    val CardCompact: Shape = RoundedCornerShape(16.dp)
    val Composer: Shape = RoundedCornerShape(28.dp)
    val TextField: Shape = RoundedCornerShape(20.dp)
    val Suggestion: Shape = RoundedCornerShape(16.dp)
    val ListItem: Shape = RoundedCornerShape(12.dp)
    val IconButton: Shape = RoundedCornerShape(24.dp)
    val Avatar: Shape = RoundedCornerShape(16.dp)
    val Full: Shape = RoundedCornerShape(50)
}

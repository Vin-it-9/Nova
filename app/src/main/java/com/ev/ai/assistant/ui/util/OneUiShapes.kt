package com.ev.ai.assistant.ui.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Official Samsung One UI Corner-Radius and Shape Design Tokens.
 * Ensures consistent structural curvature across custom voice assistant surfaces.
 */
object OneUiShapes {
    // --- MAIN WINDOW SURFACES ---
    // Used for heavy bottom sheet blocks, global dialog frames, and assistant overlay surfaces
    val OuterContainer: Shape = RoundedCornerShape(OneUiDimensions.RadiusFocusBlockLarge)

    // --- FEATURE CARD GROUPS ---
    // Standard grouped content grids or settings panel categories
    val Card: Shape = RoundedCornerShape(OneUiDimensions.RadiusThumbnailMedium)

    // Compact containers, nested tiles, and smaller card focus blocks
    val CardCompact: Shape = RoundedCornerShape(OneUiDimensions.CardContentHorizontal - 8.dp)

    // --- INTERACTIVE SYSTEM CONTROLS ---
    // Unified interactive boundaries matching One UI text inputs and search structures
    val InnerContainer: Shape = RoundedCornerShape(OneUiDimensions.CardContentHorizontal - 8.dp)
    val TextField: Shape = RoundedCornerShape(OneUiDimensions.CardContentHorizontal - 8.dp)
    val Composer: Shape = RoundedCornerShape(OneUiDimensions.RadiusFocusBlockLarge)

    // Quick action tags, capsule suggestions, pill filters, and contextual buttons
    val Suggestion: Shape = RoundedCornerShape(OneUiDimensions.RadiusButton)

    // --- LIST ELEMENTS & DATA BROWSERS ---
    // Inner rows, list item item selections, and focus grid thumbnails
    val ListItem: Shape = RoundedCornerShape(OneUiDimensions.RadiusThumbnailSmall)
    val Avatar: Shape = RoundedCornerShape(OneUiDimensions.RadiusThumbnailSmall)

    // --- SOLID PERIMETER CIRCLES ---
    // Fully circular components for standard system ripples and media actions
    val IconButton: Shape = RoundedCornerShape(50)
    val Full: Shape = RoundedCornerShape(50)
}

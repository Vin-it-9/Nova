package com.ev.ai.assistant.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Official Samsung One UI Layering, Elevation, and Border Tokens.
 * Enforces flat, soft, non-obtrusive layered dimensions for clean focus blocks.
 */
object OneUiElevation {

    // --- ELEVATION TOKENS (Z-AXIS) ---
    // Standard flat app canvas view layer (Level 0)
    val None: Dp = 0.dp

    // Hairline structural framing alignment boundaries (Level 1)
    val Hairline: Dp = 1.dp

    // One UI Focus Blocks, primary content card elements, and action tiles
    val Raised: Dp = 2.dp

    // Floating component triggers, menus, and bottom sheets (Level 2/3 cap)
    val Floating: Dp = 4.dp

    // Modal alert dialog views requiring temporary high prominence isolation
    val Overlay: Dp = 6.dp

    // --- ALPHAS FOR CLEAN OUTLINE CONTAINER FRAMING ---
    // Samsung layers soft borders to establish clear containment without heavy contrast
    const val BorderAlphaLight = 0.08f // Soft boundary outline stroke for Light Mode layout
    const val BorderAlphaDark = 0.15f  // Slightly boosted luminance boundary outline for Dark Mode

    // Glassmorphism background alpha used for semi-transparent voice sheets (Bixby/Assistant panel style)
    const val SurfaceAlphaLight = 0.92f // High-density clean viewing surface translucency
    const val SurfaceAlphaDark = 0.86f  // Smooth dark canvas visual blending blend layer
}
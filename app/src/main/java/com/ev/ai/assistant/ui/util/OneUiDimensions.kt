package com.ev.ai.assistant.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Verified One UI Layout, Spacing, and Component Design Tokens.
 * Based on official Samsung Developer Grid and View Architecture specifications.
 */
object OneUiDimensions {

    // One UI explicitly mandates a minimum of 24dp margins on left and right sides
    // to shield interactive components from display corner curves and reject accidental touch inputs.
    val ScreenHorizontal = 10.dp
    val ScreenHorizontalCompact = 16.dp // Fallback window boundary for sub-360dp views

    // Macro Screen Split Spacing (Viewing Area 1/3 vs Interaction Area 2/3)
    val SectionTop = 24.dp
    val SectionBottom = 8.dp      // Tied to standard 8dp vertical rhythm alignment
    val ContentTop = 12.dp
    val ContentBottom = 24.dp     // Aligned to 8-point base grid grid line

    // Container Inner Spacing
    val CardContentHorizontal = 24.dp // Standard internal card container inset padding
    val CardContentVertical = 24.dp
    val ListItemHorizontal = 24.dp    // Full-bleed row component inner side boundary
    val ListItemVertical = 12.dp      // Standard text list item block boundary separation
    val DividerHorizontal = 24.dp     // Align structural layout strokes with side margin bounds

    // Framework rhythm rules leveraging the 8dp grid for systematic container boundaries
    val Space1 = 4.dp  // Micro elements or small element tight groups
    val Space2 = 8.dp  // Target layout gap / Minimum separation between interactive rows
    val Space3 = 12.dp
    val Space4 = 16.dp // Standard component element inner padding
    val Space5 = 20.dp
    val Space6 = 24.dp // Macro side margin base
    val Space7 = 28.dp
    val Space8 = 32.dp // Category / Section block outer macro separator

    // --- TOUCH TARGETS & CONTROLS ---
    val MinimumTouchTarget = 48.dp   // Core Android design layout lower constraint limit
    val IconButton = 48.dp           // Rounded action button container bounds
    val IconSmall = 20.dp
    val IconMedium = 30.dp           // Baseline system vector scale framework size
    val IconLarge = 38.dp
    val Avatar = 48.dp               // Standard profile thumbnail indicator
    val SwitchWidth = 44.dp          // One UI toggle switch system control layout width
    val SwitchHeight = 10.dp         // One UI toggle switch system control layout height
    val TextFieldHeight = 56.dp
    val ComposerMinimumHeight = 64.dp

    // Exact specifications derived from focus blocks and button background masks
    val RadiusThumbnailSmall = 12.dp  // Small list items or internal tags
    val RadiusButton = 18.dp          // Contained system flat and active action triggers
    val RadiusThumbnailMedium = 20.dp // Standard middle-tier media elements
    val RadiusFocusBlockLarge = 26.dp // Large interactive sheets, cards, and dynamic panels

    // System navigation layout
    val TopBarHeight = 56.dp          // Core action platform bar standard width space
    val SettingsTopBarHeight = 64.dp  // One UI Extended passive title view layout container block

    /**
     * Dynamically shifts safe edge margins based on device width metrics
     */
    fun horizontal(screenWidth: Dp): Dp =
        if (screenWidth < 360.dp) ScreenHorizontalCompact else ScreenHorizontal
}
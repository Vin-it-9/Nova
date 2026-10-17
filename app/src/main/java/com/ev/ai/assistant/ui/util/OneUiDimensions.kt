package com.ev.ai.assistant.ui.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One UI-inspired layout and touch-target tokens.
 *
 * These are public design tokens for EV's UI layer. Samsung does not publish
 * one universal dp table for every One UI surface, so product-specific values
 * are kept here rather than scattered through individual composables.
 */
object OneUiDimensions {
    // Layout grid
    val ScreenHorizontal = 24.dp
    val ScreenHorizontalCompact = 16.dp
    val SectionTop = 24.dp
    val SectionBottom = 10.dp
    val ContentTop = 12.dp
    val ContentBottom = 28.dp
    val CardContentHorizontal = 20.dp
    val CardContentVertical = 20.dp
    val ListItemHorizontal = 18.dp
    val ListItemVertical = 15.dp
    val DividerHorizontal = 18.dp

    // Common spacing
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space7 = 28.dp
    val Space8 = 32.dp

    // Touch targets and controls
    val MinimumTouchTarget = 48.dp
    val IconButton = 48.dp
    val IconSmall = 20.dp
    val IconMedium = 24.dp
    val IconLarge = 28.dp
    val Avatar = 48.dp
    val SwitchWidth = 52.dp
    val SwitchHeight = 32.dp
    val TextFieldHeight = 56.dp
    val ComposerMinimumHeight = 64.dp

    // Navigation
    val TopBarHeight = 64.dp
    val SettingsTopBarHeight = 74.dp

    fun horizontal(screenWidth: Dp): Dp =
        if (screenWidth < 360.dp) ScreenHorizontalCompact else ScreenHorizontal
}

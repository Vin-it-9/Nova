package com.ev.ai.assistant.ui.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Applies EV's Samsung One UI design system to a Compose subtree.
 *
 * This is the single source of truth for theming. Every surface — the main activity
 * and the system [android.service.voice.VoiceInteractionSession] overlay — wraps its
 * content in [EvTheme], so both render with the exact same One UI color scheme and
 * corner radii and follow the device's light/dark setting automatically.
 *
 * @param darkTheme whether to use the dark One UI palette. Defaults to the system
 *   setting so light/dark is picked up automatically without any user-facing toggle.
 */
@Composable
fun EvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OneUiColors.colorScheme(darkTheme),
        shapes = OneUiShapes.Material3,
        content = content
    )
}

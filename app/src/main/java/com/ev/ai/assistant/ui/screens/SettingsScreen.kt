package com.ev.ai.assistant.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ev.ai.assistant.data.preferences.rememberEvPreferences
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.SettingsCard
import com.ev.ai.assistant.ui.SettingsDivider
import com.ev.ai.assistant.ui.SettingsRow
import com.ev.ai.assistant.ui.SettingsSectionTitle
import com.ev.ai.assistant.ui.ToggleRow
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiTypography

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenVoiceWakeUp: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val bgColor = if (darkTheme) OneUiColors.BackgroundDark else OneUiColors.BackgroundLight
    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

    val preferences = rememberEvPreferences()
    val isWakeUpEnabled by preferences.voiceWakeUpEnabledState
    val wakeUpWord by preferences.wakeUpWordState
    val fullVoiceResponses by preferences.fullVoiceResponsesState
    val soundFeedback by preferences.soundFeedbackState
    val useWhileLocked by preferences.useWhileLockedState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = OneUiDimensions.ScreenHorizontal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OneUiDimensions.SettingsTopBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvIconButton(EvIcon.Back, "Back", onBack)
            Text(
                text = "EV settings",
                color = textPrimary,
                style = OneUiTypography.ScreenTitle,
                modifier = Modifier.padding(start = OneUiDimensions.Space3)
            )
        }

        SettingsSectionTitle("Wake up EV")
        SettingsCard {
            SettingsRow(
                title = "Voice wake-up",
                subtitle = if (isWakeUpEnabled) wakeUpWord else "Off",
                onClick = onOpenVoiceWakeUp
            )
            SettingsDivider()
            SettingsRow(
                title = "Side button",
                onClick = { openSamsungSideButtonSettings(context) }
            )
        }

        SettingsSectionTitle("Preferences")
        SettingsCard {
            SettingsRow("Language and voice style")
            SettingsDivider()
            ToggleRow(
                title = "Full voice responses",
                initiallyChecked = fullVoiceResponses,
                onCheckedChange = { preferences.fullVoiceResponses = it }
            )
        }

        Spacer(Modifier.height(OneUiDimensions.Space5))

        SettingsCard {
            ToggleRow(
                title = "Sound feedback",
                initiallyChecked = soundFeedback,
                onCheckedChange = { preferences.soundFeedback = it }
            )
            Text(
                text = "Play a sound when EV starts and stops listening.",
                color = textSecondary,
                style = OneUiTypography.Caption,
                modifier = Modifier.padding(
                    start = OneUiDimensions.CardContentHorizontal,
                    end = OneUiDimensions.CardContentHorizontal,
                    bottom = OneUiDimensions.Space4
                )
            )
        }

        Spacer(Modifier.height(OneUiDimensions.Space5))

        SettingsCard {
            ToggleRow(
                title = "Use while locked",
                initiallyChecked = useWhileLocked,
                onCheckedChange = { preferences.useWhileLocked = it }
            )
        }

        SettingsSectionTitle("Privacy")
        SettingsCard {
            SettingsRow("Privacy Notice", onClick = onOpenPrivacy)
            SettingsDivider()
            SettingsRow("Permissions", onClick = onOpenPermissions)
        }

        Spacer(Modifier.height(OneUiDimensions.Space6))

        SettingsCard {
            SettingsRow("About EV", onClick = onOpenAbout)
        }
        Spacer(Modifier.height(OneUiDimensions.ContentBottom))
        Spacer(Modifier.height(OneUiDimensions.Space8))
    }
}

/**
 * Navigates the user directly to Samsung One UI's Side key / Side button configuration.
 * Includes graceful fallbacks for different One UI and Android versions.
 */
private fun openSamsungSideButtonSettings(context: Context) {
    val intents = listOf(
        // Direct Samsung One UI Side Key settings intent
        Intent("com.samsung.android.settings.SIDE_KEY_SETTINGS"),
        // Samsung Side Key Activity component
        Intent().apply {
            setClassName(
                "com.android.settings",
                "com.android.settings.Settings\$SideKeySettingsActivity"
            )
        },
        // Samsung Advanced Features settings
        Intent("com.samsung.android.settings.ADVANCED_FEATURES"),
        // Voice input / default assistant settings
        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
        // System settings fallback
        Intent(Settings.ACTION_SETTINGS)
    )

    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
            // Try next candidate
        }
    }

    // Ultimate safe fallback
    try {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) {
        // Safe no-op
    }
}

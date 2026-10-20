package com.ev.ai.assistant.ui.screens.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.SettingsCard
import com.ev.ai.assistant.ui.SettingsDivider
import com.ev.ai.assistant.ui.SettingsSectionTitle
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiTypography

/**
 * Dedicated privacy page. Everything about how E.V. handles the user's data
 * lives here — the settings screen only links to it.
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val darkTheme = isSystemInDarkTheme()
    val bgColor = if (darkTheme) OneUiColors.BackgroundDark else OneUiColors.BackgroundLight
    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

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
                text = "Privacy notice",
                color = textPrimary,
                style = OneUiTypography.ScreenTitle,
                modifier = Modifier.padding(start = OneUiDimensions.Space3)
            )
        }

        PrivacySection(
            title = "On-device by default",
            body = "E.V. processes your voice on your phone using the system speech " +
                "recognizer. Commands, notes, reminders and settings are stored locally " +
                "on this device only. No assistant account is created and nothing is " +
                "uploaded to E.V. servers."
        )

        PrivacySection(
            title = "No Google account",
            body = "E.V. never connects, requests or stores a Google account or any " +
                "other sign-in. There is no cloud sync, no telemetry and no advertising " +
                "identifier in this app."
        )

        PrivacySection(
            title = "Permissions you grant",
            body = "Microphone is needed to hear you. Contacts is used to match people " +
                "you ask to call or message. Phone, SMS, calendar and alarm access are " +
                "used only for the action you request, at the moment you request it. You " +
                "can review or revoke any permission from the Permissions screen."
        )

        PrivacySection(
            title = "What leaves your device",
            body = "Only the action you ask for reaches an outside service — for example " +
                "opening a web search, a map route or a nearby-place search hands your " +
                "query to your browser or maps app, where that app's own privacy policy " +
                "applies. E.V. itself does not copy, store or retransmit these queries."
        )

        PrivacySection(
            title = "Root features",
            body = "Lock screen works without root. Screenshots require root (Magisk). " +
                "Root commands run locally through su and are never sent anywhere else. " +
                "Granting root is optional and can be withdrawn in Magisk at any time."
        )

        PrivacySection(
            title = "Voice recordings",
            body = "Audio captured while listening is handled by Android's speech " +
                "service and discarded once a result is returned. E.V. does not keep " +
                "recordings of your conversations."
        )

        SettingsSectionTitle("Your control")
        SettingsCard {
            Text(
                text = "You can clear everything at any time with Android's " +
                    "\"Clear storage\" for E.V., or revoke any permission from the " +
                    "Permissions screen. Uninstalling the app removes all of its local data.",
                color = textSecondary,
                style = OneUiTypography.Body,
                modifier = Modifier.padding(
                    horizontal = OneUiDimensions.CardContentHorizontal,
                    vertical = OneUiDimensions.Space4
                )
            )
        }

        Spacer(Modifier.height(OneUiDimensions.ContentBottom))
        Spacer(Modifier.height(OneUiDimensions.Space8))
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String
) {
    val darkTheme = isSystemInDarkTheme()
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

    SettingsSectionTitle(title)
    SettingsCard {
        Text(
            text = body,
            color = textSecondary,
            style = OneUiTypography.Body,
            modifier = Modifier.padding(
                horizontal = OneUiDimensions.CardContentHorizontal,
                vertical = OneUiDimensions.Space4
            )
        )
    }
}

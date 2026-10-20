@file:Suppress("DEPRECATION")

package com.ev.ai.assistant.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ev.ai.assistant.R
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiMotion
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    val bgColor = if (darkTheme) OneUiColors.BackgroundDark else OneUiColors.BackgroundLight
    val surfaceColor = if (darkTheme) OneUiColors.SurfaceDark else OneUiColors.SurfaceLight
    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight
    val textTertiary = if (darkTheme) OneUiColors.TextTertiaryDark else OneUiColors.TextTertiaryLight

    val versionName = remember(context) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    var showTermsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(OneUiDimensions.TopBarHeight)
                .padding(horizontal = OneUiDimensions.ScreenHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EvIconButton(
                icon = EvIcon.Back,
                description = "Back to settings",
                onClick = onBack
            )

            EvIconButton(
                icon = EvIcon.Info,
                description = "App info",
                onClick = {
                    try {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Safe fallback
                    }
                }
            )
        }

        // --- SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OneUiDimensions.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            // App Logo
            Image(
                painter = painterResource(R.drawable.ic_ev_logo),
                contentDescription = "EV Logo",
                modifier = Modifier
                    .size(104.dp)
                    .clip(OneUiShapes.OuterContainer)
            )

            Spacer(modifier = Modifier.height(OneUiDimensions.Space4))

            // App Title
            Text(
                text = "E.V.",
                style = OneUiTypography.AppTitle.copy(
                    fontSize = 28.sp,
                    lineHeight = 32.sp
                ),
                color = textPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(OneUiDimensions.Space2))

            // Version Info
            Text(
                text = "Version $versionName",
                style = OneUiTypography.BodyMedium,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(OneUiDimensions.Space2))

            // Version Status
            Text(
                text = "The latest version is installed.",
                style = OneUiTypography.Caption,
                color = textTertiary
            )

            Spacer(modifier = Modifier.height(48.dp))
            Spacer(modifier = Modifier.weight(1.2f, fill = false))

            // --- ACTION BUTTONS (One UI Rounded Pills) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .padding(horizontal = OneUiDimensions.Space4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OneUiDimensions.Space3)
            ) {
                AboutActionButton(
                    text = "Terms and conditions",
                    onClick = { showTermsDialog = true },
                    darkTheme = darkTheme
                )
            }

            Spacer(modifier = Modifier.height(OneUiDimensions.Space8))

            // Copyright Notice
            Text(
                text = "© 2026 EV Assistant. All rights reserved.",
                style = OneUiTypography.Supporting,
                color = textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = OneUiDimensions.Space6)
            )
        }
    }

    // --- TERMS & CONDITIONS DIALOG ---
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text(
                        text = "OK",
                        color = OneUiColors.primaryAction(darkTheme),
                        style = OneUiTypography.Label
                    )
                }
            },
            title = {
                Text(
                    text = "Terms and Conditions",
                    style = OneUiTypography.ScreenTitle,
                    color = textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(OneUiDimensions.Space3)
                ) {
                    Text(
                        text = "Welcome to E.V. Assistant. By using this service, you agree to the following terms:",
                        style = OneUiTypography.Body,
                        color = textSecondary
                    )
                    Text(
                        text = "1. Service Usage\nE.V. provides an AI voice assistant experience on your device. Audio recognition and assistant capabilities are provided for personal assistance.",
                        style = OneUiTypography.Caption,
                        color = textSecondary
                    )
                    Text(
                        text = "2. Privacy & Data Handling\nVoice inputs and speech transcriptions are processed strictly to handle your immediate commands. Your data is handled in accordance with on-device privacy protection policies.",
                        style = OneUiTypography.Caption,
                        color = textSecondary
                    )
                    Text(
                        text = "3. Availability & Changes\nFeatures and compatibility may vary depending on device capabilities, Android OS version, and language configuration.",
                        style = OneUiTypography.Caption,
                        color = textSecondary
                    )
                }
            },
            shape = OneUiShapes.OuterContainer,
            containerColor = surfaceColor
        )
    }
}

@Composable
private fun AboutActionButton(
    text: String,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = OneUiMotion.PressSpring,
        label = "about_action_button_scale"
    )

    val shape = OneUiShapes.OuterContainer

    val buttonBg = if (darkTheme) {
        OneUiColors.SurfaceVariantDark.copy(alpha = 0.85f)
    } else {
        OneUiColors.SurfaceVariantLight
    }

    val buttonBorderColor = if (darkTheme) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE2E5E8)
    }

    val textPrimary = if (darkTheme) {
        OneUiColors.TextPrimaryDark
    } else {
        OneUiColors.TextPrimaryLight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(color = buttonBg, shape = shape)
            .border(
                width = OneUiElevation.Hairline,
                color = buttonBorderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = text }
            .heightIn(min = OneUiDimensions.MinimumTouchTarget)
            .padding(
                horizontal = OneUiDimensions.Space6,
                vertical = OneUiDimensions.Space3
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textPrimary,
            style = OneUiTypography.Label,
            fontWeight = FontWeight.Medium
        )
    }
}

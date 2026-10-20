package com.ev.ai.assistant.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ev.ai.assistant.data.preferences.rememberEvPreferences
import com.ev.ai.assistant.service.EvVoiceInteractionService
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.SettingsCard
import com.ev.ai.assistant.ui.SettingsDivider
import com.ev.ai.assistant.ui.SettingsSectionTitle
import com.ev.ai.assistant.ui.overlay.EvAssistantOverlayPanel
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiMotion
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography

@Composable
fun VoiceWakeUpScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val bgColor = if (darkTheme) OneUiColors.BackgroundDark else OneUiColors.BackgroundLight
    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

    val preferences = rememberEvPreferences()
    val isWakeUpEnabled by preferences.voiceWakeUpEnabledState
    val selectedWakeUpWord by preferences.wakeUpWordState
    val wakeUpOptions = listOf("EV", "Jarvis", "Friday", "Edith")

    // Lifecycle observer to detect when user returns from Default Assistant settings
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isDefaultAssistant = remember(context, refreshTrigger) {
        EvVoiceInteractionService.isEvDefaultAssistant(context)
    }

    var showTestOverlay by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = OneUiDimensions.ScreenHorizontal)
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OneUiDimensions.SettingsTopBarHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvIconButton(
                    icon = EvIcon.Back,
                    description = "Back to settings",
                    onClick = onBack
                )
                Text(
                    text = "Voice wake-up",
                    color = textPrimary,
                    style = OneUiTypography.ScreenTitle,
                    modifier = Modifier.padding(start = OneUiDimensions.Space3)
                )
            }

            Spacer(modifier = Modifier.height(OneUiDimensions.Space2))

            // --- DEFAULT ASSISTANT STATUS CARD ---
            SettingsCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = OneUiDimensions.ListItemHorizontal,
                            vertical = OneUiDimensions.ListItemVertical
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default digital assistant",
                                color = textPrimary,
                                style = OneUiTypography.BodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDefaultAssistant) {
                                    "EV is active as your device assistant"
                                } else {
                                    "EV must be selected as default assistant to listen for wake words in background"
                                },
                                color = if (isDefaultAssistant) {
                                    OneUiColors.ControlActive
                                } else {
                                    textSecondary
                                },
                                style = OneUiTypography.Caption
                            )
                        }

                        if (isDefaultAssistant) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(OneUiShapes.Full)
                                    .background(OneUiColors.ControlActive.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                EvIcon(
                                    icon = EvIcon.Check,
                                    modifier = Modifier.size(16.dp),
                                    tint = OneUiColors.ControlActive
                                )
                            }
                        }
                    }

                    if (!isDefaultAssistant) {
                        Spacer(modifier = Modifier.height(OneUiDimensions.Space3))
                        AssistantActionButton(
                            text = "Set EV as default assistant",
                            darkTheme = darkTheme,
                            onClick = {
                                EvVoiceInteractionService.openAssistantSettings(context)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(OneUiDimensions.Space5))

            // --- MASTER TOGGLE CARD ---
            SettingsCard {
                val interactionSource = remember { MutableInteractionSource() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(OneUiShapes.InnerContainer)
                        .toggleable(
                            value = isWakeUpEnabled,
                            role = Role.Switch,
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onValueChange = { preferences.isVoiceWakeUpEnabled = it }
                        )
                        .heightIn(min = OneUiDimensions.MinimumTouchTarget)
                        .padding(
                            start = OneUiDimensions.ListItemHorizontal,
                            end = OneUiDimensions.Space3,
                            top = OneUiDimensions.Space3,
                            bottom = OneUiDimensions.Space3
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWakeUpEnabled) "On" else "Off",
                        color = textPrimary,
                        style = OneUiTypography.BodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = isWakeUpEnabled,
                        onCheckedChange = null,
                        modifier = Modifier
                            .size(
                                width = OneUiDimensions.SwitchWidth,
                                height = OneUiDimensions.SwitchHeight
                            )
                            .scale(
                                animateFloatAsState(
                                    targetValue = 0.70f,
                                    animationSpec = OneUiMotion.ControlSpring,
                                    label = "master_switch_scale"
                                ).value
                            ),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = OneUiColors.ControlActive,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = if (darkTheme) {
                                OneUiColors.ControlTrackDark
                            } else {
                                OneUiColors.ControlTrackLight
                            },
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(OneUiDimensions.Space6))

            // --- WAKE-UP OPTIONS SECTION ---
            SettingsSectionTitle("Wake-up phrase")
            SettingsCard {
                wakeUpOptions.forEachIndexed { index, optionName ->
                    WakeUpOptionRow(
                        name = optionName,
                        isSelected = (selectedWakeUpWord == optionName),
                        enabled = isWakeUpEnabled,
                        darkTheme = darkTheme,
                        onClick = {
                            if (isWakeUpEnabled) {
                                preferences.wakeUpWord = optionName
                            }
                        }
                    )
                    if (index < wakeUpOptions.lastIndex) {
                        SettingsDivider()
                    }
                }
            }

            // --- INFORMATIVE CAPTION ---
            Spacer(modifier = Modifier.height(OneUiDimensions.Space3))
            Text(
                text = "Say the wake-up phrase to start talking to E.V. at any time. When enabled, your device listens for the selected wake word.",
                color = textSecondary,
                style = OneUiTypography.Caption,
                modifier = Modifier.padding(
                    horizontal = OneUiDimensions.CardContentHorizontal,
                    vertical = OneUiDimensions.Space2
                )
            )

            // --- TEST OVERLAY BUTTON ---
            Spacer(modifier = Modifier.height(OneUiDimensions.Space5))
            AssistantActionButton(
                text = "Test voice wake-up overlay",
                darkTheme = darkTheme,
                onClick = {
                    val triggered = EvVoiceInteractionService.triggerWakeUp(context)
                    if (!triggered) {
                        showTestOverlay = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(OneUiDimensions.ContentBottom))
            Spacer(modifier = Modifier.height(OneUiDimensions.Space8))
        }

        // In-app floating test overlay modal
        if (showTestOverlay) {
            EvAssistantOverlayPanel(
                initialPrompt = "Heard \"$selectedWakeUpWord\"! How can I help you?",
                onDismiss = { showTestOverlay = false }
            )
        }
    }
}

@Composable
private fun WakeUpOptionRow(
    name: String,
    isSelected: Boolean,
    enabled: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowShape = OneUiShapes.ListItem

    val textPrimary = if (darkTheme) OneUiColors.TextPrimaryDark else OneUiColors.TextPrimaryLight
    val textSecondary = if (darkTheme) OneUiColors.TextSecondaryDark else OneUiColors.TextSecondaryLight

    val contentAlpha = if (enabled) 1f else 0.40f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
            .heightIn(min = OneUiDimensions.MinimumTouchTarget)
            .padding(
                horizontal = OneUiDimensions.ListItemHorizontal,
                vertical = OneUiDimensions.ListItemVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = textPrimary.copy(alpha = contentAlpha),
                style = OneUiTypography.BodyMedium,
                fontWeight = if (isSelected && enabled) FontWeight.SemiBold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Wake up by saying \"$name\"",
                color = textSecondary.copy(alpha = contentAlpha),
                style = OneUiTypography.Caption,
                lineHeight = 16.sp
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = OneUiColors.ControlActive,
                unselectedColor = textSecondary.copy(alpha = contentAlpha),
                disabledSelectedColor = OneUiColors.ControlActive.copy(alpha = 0.38f),
                disabledUnselectedColor = textSecondary.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun AssistantActionButton(
    text: String,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = OneUiMotion.PressSpring,
        label = "assistant_action_scale"
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
            .background(buttonBg, shape)
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
            .heightIn(min = OneUiDimensions.MinimumTouchTarget)
            .padding(
                horizontal = OneUiDimensions.Space6,
                vertical = OneUiDimensions.Space3
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OneUiDimensions.Space2)
        ) {
            Text(
                text = text,
                color = textPrimary,
                style = OneUiTypography.Label,
                fontWeight = FontWeight.Medium
            )
            EvIcon(
                icon = EvIcon.ChevronRight,
                modifier = Modifier.size(OneUiDimensions.IconSmall),
                tint = textPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

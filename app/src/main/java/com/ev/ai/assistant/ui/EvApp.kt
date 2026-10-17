package com.ev.ai.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiMotion
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import com.ev.ai.assistant.ui.util.OneUiColors


@Composable
fun EvApp() {
    var showSettings by remember { mutableStateOf(false) }
    val darkTheme = isSystemInDarkTheme()

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = OneUiColors.PrimaryDark,
            background = OneUiColors.BackgroundDark,
            surface = OneUiColors.SurfaceDark,
            surfaceVariant = OneUiColors.SurfaceVariantDark,
            onBackground = OneUiColors.TextPrimaryDark,
            onSurface = OneUiColors.TextPrimaryDark,
            onSurfaceVariant = OneUiColors.TextSecondaryDark,
            outline = OneUiColors.DividerDark
        )
    } else {
        lightColorScheme(
            primary = OneUiColors.PrimaryLight,
            background = OneUiColors.BackgroundLight,
            surface = OneUiColors.SurfaceLight,
            surfaceVariant = OneUiColors.SurfaceVariantLight,
            onBackground = OneUiColors.TextPrimaryLight,
            onSurface = OneUiColors.TextPrimaryLight,
            onSurfaceVariant = OneUiColors.TextSecondaryLight,
            outline = OneUiColors.DividerLight
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        AnimatedContent(
            targetState = showSettings,
            label = "one_ui_screen_transition",
            transitionSpec = {
                val slideSpec = OneUiMotion.screenSpec<IntOffset>()
                val fadeSpec = OneUiMotion.screenSpec<Float>()
                val depthSpec = OneUiMotion.screenSpec<Float>()

                (
                    slideInHorizontally(
                        animationSpec = slideSpec,
                        initialOffsetX = { (it * OneUiMotion.ScreenIncomingOffsetFraction).toInt() }
                    ) + fadeIn(
                        animationSpec = fadeSpec,
                        initialAlpha = 0f
                    )
                ).togetherWith(
                    fadeOut(
                        animationSpec = fadeSpec,
                        targetAlpha = 0f
                    ) + scaleOut(
                        animationSpec = depthSpec,
                        targetScale = OneUiMotion.ScreenOutgoingScale
                    )
                )
            }
        ) { isSettingsScreen ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
            ) {
                if (isSettingsScreen) {
                    SettingsScreen { showSettings = false }
                } else {
                    HomeScreen { showSettings = true }
                }
            }
        }
    }
}


@Composable
private fun HomeScreen(
    onOpenSettings: () -> Unit
) {
    var input by rememberSaveable() { mutableStateOf("") }
    var isListening by rememberSaveable() { mutableStateOf(false) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val speechRecognizer = remember(context) {
        SpeechRecognizerController(
            context = context,
            onTextChanged = { recognizedText -> input = recognizedText },
            onListeningChanged = { listening -> isListening = listening }
        )
    }

    DisposableEffect(speechRecognizer) {
        onDispose { speechRecognizer.release() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechRecognizer.start()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun toggleListening() {
        if (isListening) {
            speechRecognizer.stop()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            speechRecognizer.start()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val darkTheme = isSystemInDarkTheme()

    val bgColor = if (darkTheme) {
        OneUiColors.BackgroundDark
    } else {
        OneUiColors.BackgroundLight
    }

    val surfaceColor = if (darkTheme) {
        OneUiColors.SurfaceDark
    } else {
        OneUiColors.SurfaceLight
    }

    val textPrimary = if (darkTheme) {
        OneUiColors.TextPrimaryDark
    } else {
        OneUiColors.TextPrimaryLight
    }

    val primaryAccent = if (darkTheme) {
        OneUiColors.PrimaryDark
    } else {
        OneUiColors.PrimaryLight
    }

    val cardBorderColor = if (darkTheme) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE8EBF0)
    }

    val suggestionBorderColors = if (darkTheme) {
        listOf(
            Color(0xFF50BFC8),
            Color(0xFF7FA6E7),
            Color(0xFFAE96D6)
        )
    } else {
        listOf(
            Color(0xFF79DCE2),
            Color(0xFF8DB9F8),
            Color(0xFFC7B9ED)
        )
    }

    val suggestions = remember {
        listOf(
            "What is the stock price of Microsoft today?",
            "Enable vibration for all my alarms",
            "Turn on Bluetooth"
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(OneUiDimensions.TopBarHeight)
                    .padding(horizontal = OneUiDimensions.ScreenHorizontal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvIconButton(
                    EvIcon.Menu,
                    "Open EV settings",
                    onOpenSettings
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "EV",
                        color = textPrimary,
                        style = OneUiTypography.AppTitle
                    )

                    Text(
                        text = "Your AI assistant",
                        color = textPrimary.copy(alpha = 0.62f),
                        style = OneUiTypography.Supporting
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        },
        bottomBar = {
            AskBar(
                input,
                { input = it },
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                onMicrophoneClick = ::toggleListening,
                isListening = isListening
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = OneUiDimensions.ScreenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                    shape = OneUiShapes.OuterContainer,
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = OneUiElevation.None
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = cardBorderColor
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = OneUiDimensions.CardContentHorizontal,
                                vertical = OneUiDimensions.CardContentVertical
                            )
                    ) {
                        Text(
                            text = "You can say",
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF22C7D8),
                                        primaryAccent
                                    )
                                ),
                                fontSize = OneUiTypography.ScreenTitle.fontSize,
                                lineHeight = OneUiTypography.ScreenTitle.lineHeight,
                                fontWeight = OneUiTypography.ScreenTitle.fontWeight,
                                letterSpacing = OneUiTypography.ScreenTitle.letterSpacing
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(OneUiDimensions.Space2)
                        )

                        Text(
                            text = "Try a suggestion or ask EV in your own words.",
                            color = textPrimary.copy(alpha = 0.65f),
                            style = OneUiTypography.Label
                        )

                        Spacer(
                            modifier = Modifier.height(OneUiDimensions.Space5)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            suggestions.forEachIndexed { index, suggestion ->
                                SuggestionPill(
                                    text = suggestion,
                                    borderColor = suggestionBorderColors[index]
                                ) {
                                    input = suggestion
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SettingsScreen(onBack: () -> Unit) {
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
                text = "EV settings",
                color = textPrimary,
                style = OneUiTypography.ScreenTitle,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        AccountCard()

        SettingsSectionTitle("Wake up EV")
        SettingsCard {
            SettingsRow("Voice wake-up", "EV")
            SettingsDivider()
            SettingsRow("Side button")
        }

        SettingsSectionTitle("Preferences")
        SettingsCard {
            SettingsRow("Language and voice style")
            SettingsDivider()
            ToggleRow("Full voice responses", true)
        }

        Spacer(Modifier.height(OneUiDimensions.Space5))

        SettingsCard {
            ToggleRow("Sound feedback", true)
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

        SettingsCard { ToggleRow("Use while locked", true) }

        SettingsSectionTitle("Privacy")
        SettingsCard {
            SettingsRow("Privacy settings")
            SettingsDivider()
            SettingsRow("Privacy Notice")
            SettingsDivider()
            ToggleRow("Get news and special offers", false)
            SettingsDivider()
            SettingsRow("Permissions")
        }

        Spacer(Modifier.height(OneUiDimensions.Space4))

        SettingsCard {
            SettingsRow("About EV")
            SettingsDivider()
            SettingsRow("Contact us")
        }
        Spacer(Modifier.height(OneUiDimensions.ContentBottom))
    }
}
package com.ev.ai.assistant.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import com.ev.ai.assistant.nlu.EvActionExecutor
import com.ev.ai.assistant.nlu.EvNluEngine
import com.ev.ai.assistant.nlu.EvTtsSpeaker
import com.ev.ai.assistant.ui.AskBar
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.SpeechRecognizerController
import com.ev.ai.assistant.ui.SuggestionPill
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit
) {
    var input by rememberSaveable() { mutableStateOf("") }
    var isListening by rememberSaveable() { mutableStateOf(false) }
    var lastExecutionResult by rememberSaveable() { mutableStateOf<String?>(null) }
    var lastExecutedQuery by rememberSaveable() { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    fun handleQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        lastExecutedQuery = trimmed
        val parsedIntent = EvNluEngine.parse(trimmed, context)
        val result = EvActionExecutor.execute(context, parsedIntent, speakResponse = true)
        lastExecutionResult = result.userDisplayMessage
        input = ""
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val speechRecognizer = remember(context) {
        SpeechRecognizerController(
            context = context,
            onTextChanged = { recognizedText -> input = recognizedText },
            onFinalText = { recognizedText ->
                input = recognizedText
                handleQuery(recognizedText)
            },
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

    // Ask once for the permissions the assistant's features need (calls, SMS,
    // contacts, calendar, notifications). Each feature also degrades gracefully
    // when a permission is missing, so denying any of these is not fatal.
    val featurePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results are handled implicitly by each feature */ }

    LaunchedEffect(Unit) {
        val needed = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS
        ).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            featurePermissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Warm the speech engine WITHOUT touching the microphone (the old warm-up
    // grabbed the mic on the home page — the mic flickered start/stop on every
    // app open). The TTS engine, by contrast, is safe to pre-bind: it only
    // shortens the gap between an answer appearing and the spoken response
    // starting, and never activates the mic.
    LaunchedEffect(Unit) {
        EvTtsSpeaker.warmUpIfEnabled(context)
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
            Color(0xFFAE96D6),
            Color(0xFF79DCE2),
            Color(0xFF90CAF9)
        )
    } else {
        listOf(
            Color(0xFF79DCE2),
            Color(0xFF8DB9F8),
            Color(0xFFC7B9ED),
            Color(0xFF80CBC4),
            Color(0xFF64B5F6)
        )
    }

    val suggestions = remember {
        listOf(
            "Open WhatsApp",
            "Set Alarm at 7:00 AM",
            "What time is it?",
            "Turn on Bluetooth",
            "How are you?"
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        text = "E.V.",
                        color = textPrimary,
                        style = OneUiTypography.AppTitle
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        },
        bottomBar = {
            AskBar(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                onSubmit = { handleQuery(input) },
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
                    .padding(top = OneUiDimensions.ContentTop)
                    .padding(horizontal = OneUiDimensions.ScreenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Interactive EV Assistant Response Card
                if (lastExecutionResult != null) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(bottom = OneUiDimensions.Space4),
                        shape = OneUiShapes.OuterContainer,
                        colors = CardDefaults.cardColors(
                            containerColor = primaryAccent.copy(alpha = if (darkTheme) 0.16f else 0.10f)
                        ),
                        border = BorderStroke(
                            width = OneUiElevation.Hairline,
                            color = primaryAccent.copy(alpha = 0.35f)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(OneUiDimensions.Space2)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(OneUiShapes.Full)
                                            .background(primaryAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "E",
                                            color = Color.White,
                                            style = OneUiTypography.Caption,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = lastExecutedQuery ?: "E.V. Assistant",
                                        color = textPrimary,
                                        style = OneUiTypography.Label,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }

                                EvIconButton(
                                    icon = EvIcon.Back,
                                    description = "Clear response",
                                    onClick = { lastExecutionResult = null }
                                )
                            }

                            Spacer(modifier = Modifier.height(OneUiDimensions.Space2))

                            Text(
                                text = lastExecutionResult ?: "",
                                color = textPrimary,
                                style = OneUiTypography.BodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                    shape = OneUiShapes.OuterContainer,
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor.copy(
                            alpha = if (darkTheme) {
                                OneUiElevation.SurfaceAlphaDark
                            } else {
                                OneUiElevation.SurfaceAlphaLight
                            }
                        )
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = OneUiElevation.Raised
                    ),
                    border = BorderStroke(
                        width = OneUiElevation.Hairline,
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
                        val headingGradient = Brush.linearGradient(
                            colors = if (darkTheme) {
                                listOf(
                                    OneUiColors.ControlActive,
                                    OneUiColors.PrimaryPressedDark,
                                    OneUiColors.TertiaryAccent
                                )
                            } else {
                                listOf(
                                    OneUiColors.BrandPrimary,
                                    OneUiColors.PrimaryActionLight,
                                    OneUiColors.TertiaryAccent
                                )
                            }
                        )

                        Text(
                            text = "You can say",
                            style = TextStyle(
                                brush = headingGradient,
                                fontFamily = OneUiTypography.Display.fontFamily,
                                fontSize = OneUiTypography.Display.fontSize,
                                lineHeight = OneUiTypography.Display.lineHeight,
                                fontWeight = OneUiTypography.Display.fontWeight,
                                letterSpacing = OneUiTypography.Display.letterSpacing
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
                            verticalArrangement = Arrangement.spacedBy(OneUiDimensions.Space3)
                        ) {
                            suggestions.forEachIndexed { index, suggestion ->
                                SuggestionPill(
                                    text = suggestion,
                                    borderColor = suggestionBorderColors[index % suggestionBorderColors.size]
                                ) {
                                    handleQuery(suggestion)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.ev.ai.assistant.ui.overlay

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ev.ai.assistant.nlu.EvActionExecutor
import com.ev.ai.assistant.nlu.EvNluEngine
import com.ev.ai.assistant.ui.EvIcon
import com.ev.ai.assistant.ui.EvIconButton
import com.ev.ai.assistant.ui.SpeechRecognizerController
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiMotion
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography
import kotlinx.coroutines.delay

/**
 * Floating Bixby-paradigm Assistant Overlay Panel.
 *
 * Renders within the Android VoiceInteractionSession compose layer:
 * - Full-screen transparent canvas (0.0f alpha) — clicking outside collapses the overlay.
 * - Bottom-docked workspace card bound to OneUi design tokens.
 * - Center voice activation orb continuously animated via [OneUiMotion.rememberListeningPulse()].
 * - Live transcription text formatted with [OneUiTypography.ScreenTitle].
 * - AskBar is text-only in overlay mode — the center orb is the sole mic control.
 * - Processes one final speech result and dismisses after the response is delivered.
 *
 * All colors resolve through [MaterialTheme] (see [com.ev.ai.assistant.ui.util.EvTheme]), so
 * the panel follows the device light/dark setting exactly like the in-app UI.
 */
@Composable
fun EvAssistantOverlayPanel(
    initialPrompt: String = "Listening...",
    onDismiss: () -> Unit,
    onSubmitQuery: (String) -> Unit = {}
) {
    var transcriptionText by remember { mutableStateOf(initialPrompt) }
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var dismissAfterResponse by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val hasMicPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    // Executes only the final recognizer result; partial results never trigger actions.
    fun processQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        val parsed = EvNluEngine.parse(trimmed, context)
        val result = EvActionExecutor.execute(context, parsed, speakResponse = true)
        transcriptionText = result.userDisplayMessage
        inputText = ""
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        onSubmitQuery(trimmed)
        dismissAfterResponse = true
    }

    val speechRecognizer = remember(context) {
        SpeechRecognizerController(
            context = context,
            onTextChanged = { recognizedText ->
                transcriptionText = recognizedText
                inputText = recognizedText
            },
            onFinalText = { recognizedText -> processQuery(recognizedText) },
            onListeningChanged = { listening ->
                isListening = listening
            }
        )
    }

    LaunchedEffect(dismissAfterResponse) {
        if (dismissAfterResponse) {
            delay(1200L)
            onDismiss()
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose { speechRecognizer.release() }
    }

    fun toggleListening() {
        if (isListening) {
            speechRecognizer.stop()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }
        if (hasMicPermission) {
            transcriptionText = "Listening..."
            speechRecognizer.start()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            transcriptionText = "Microphone permission required"
        }
    }

    // Auto-start speech recognition when overlay first appears
    LaunchedEffect(Unit) {
        if (hasMicPermission) {
            delay(300L) // small grace period before listening
            speechRecognizer.start()
        } else {
            transcriptionText = "Tap the orb to speak"
        }
    }

    // Organic scaling loop: 1.0f → 1.12f across 1200ms
    val listeningPulse = OneUiMotion.rememberListeningPulse()
    val pulseScale = if (isListening) listeningPulse.scale.value else 1.0f
    val pulseAlpha = if (isListening) listeningPulse.alpha.value else 0.0f

    val darkTheme = isSystemInDarkTheme()
    val surfaceAlpha = OneUiElevation.surfaceAlpha(darkTheme)
    val borderAlpha = OneUiElevation.borderAlpha(darkTheme)

    // --- CANVAS OVERLAY LAYER (Full Screen, Transparent 0.0f alpha) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // --- WORKSPACE BOTTOM CARD LAYER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OneUiDimensions.ScreenHorizontal)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .clip(OneUiShapes.OuterContainer)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha))
                .border(
                    width = OneUiElevation.Hairline,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha),
                    shape = OneUiShapes.OuterContainer
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consume clicks to prevent outside dismiss */ }
                )
                .padding(
                    horizontal = OneUiDimensions.CardContentHorizontal,
                    vertical = OneUiDimensions.CardContentVertical
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Samsung One UI top pill drag handle
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
            )

            Spacer(modifier = Modifier.height(OneUiDimensions.Space3))

            // Header bar with EV logo brand and dismiss button
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = OneUiTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "E.V. Assistant",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        style = OneUiTypography.Label,
                        fontWeight = FontWeight.Medium
                    )
                }

                EvIconButton(
                    icon = EvIcon.Close,
                    description = "Close assistant",
                    onClick = onDismiss
                )
            }

            Spacer(modifier = Modifier.height(OneUiDimensions.Space4))

            // --- LIVE VOICE TRANSCRIPTION STATE TEXT ---
            Text(
                text = transcriptionText,
                style = OneUiTypography.ScreenTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OneUiDimensions.Space2)
            )

            Spacer(modifier = Modifier.height(OneUiDimensions.Space6))

            // --- CENTER VOICE ACTIVATION ORB (sole mic control in overlay mode) ---
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outermost ambient pulse halo
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    OneUiColors.BrandPrimary.copy(alpha = pulseAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Concentric boundary ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(OneUiColors.ControlActive.copy(alpha = pulseAlpha * 0.45f))
                )

                // Core voice engine button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    OneUiColors.BrandPrimary,
                                    OneUiColors.ControlActive
                                )
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { toggleListening() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    EvIcon(
                        icon = EvIcon.Microphone,
                        modifier = Modifier.size(OneUiDimensions.IconSmall),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(OneUiDimensions.Space5))

            // --- TEXT INPUT BAR (no mic button in overlay — orb is the only mic control) ---
            OverlayTextBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSubmit = {
                    val q = inputText.trim()
                    if (q.isNotBlank()) processQuery(q)
                }
            )
        }
    }
}

/**
 * Minimal text input bar for the overlay context.
 * Has NO mic button — the center orb is the sole voice control.
 * Only shows a send button when text is present.
 */
@Composable
private fun OverlayTextBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val hasInput = value.isNotBlank()
    val darkTheme = isSystemInDarkTheme()
    val borderAlpha = OneUiElevation.borderAlpha(darkTheme)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = OneUiShapes.Composer,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
        ),
        border = BorderStroke(
            width = OneUiElevation.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = OneUiDimensions.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = OneUiDimensions.Space1),
                placeholder = {
                    Text(
                        text = "Or type your question…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = OneUiTypography.Body
                    )
                },
                singleLine = true,
                textStyle = OneUiTypography.Body.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = OneUiShapes.TextField,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit(); keyboard?.hide() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (hasInput) {
                EvIconButton(
                    icon = EvIcon.Send,
                    description = "Send message",
                    onClick = { onSubmit(); keyboard?.hide() }
                )
            }
        }
    }
}

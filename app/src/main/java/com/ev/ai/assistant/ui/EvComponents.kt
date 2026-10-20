package com.ev.ai.assistant.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiDimensions
import com.ev.ai.assistant.ui.util.OneUiElevation
import com.ev.ai.assistant.ui.util.OneUiMotion
import com.ev.ai.assistant.ui.util.OneUiShapes
import com.ev.ai.assistant.ui.util.OneUiTypography
import com.ev.ai.assistant.R

internal enum class EvIcon {
    Menu,
    Back,
    Microphone,
    Account,
    ChevronRight,
    Send,
    Info,
    Check,
    Close
}

/** A 48 dp minimum-touch-target icon button with restrained press feedback. */
@Composable
internal fun EvIconButton(
    icon: EvIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = OneUiMotion.shortSpec(),
        label = "ev_icon_button_scale"
    )
    Box(
        modifier = modifier
            .size(OneUiDimensions.IconButton)
            .scale(scale)
            .clip(OneUiShapes.IconButton)
            .background(
                if (isPressed && enabled) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        EvIcon(
            icon = icon,
            modifier = Modifier.size(OneUiDimensions.IconLarge),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}

/** Hand-drawn icons keep the EV UI visually consistent without adding an icon dependency. */
@Composable
internal fun EvIcon(
    icon: EvIcon,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier) {
        val stroke = (OneUiDimensions.Space1 / 2).toPx()
        val w = size.width
        val h = size.height

        when (icon) {
            EvIcon.Menu -> {
                repeat(3) { line ->
                    val y = h * (0.32f + line * 0.18f)
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.25f, y),
                        end = Offset(w * 0.75f, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            EvIcon.Back -> {
                drawLine(tint, Offset(w * 0.72f, h * 0.50f), Offset(w * 0.30f, h * 0.50f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.49f, h * 0.31f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.49f, h * 0.69f), stroke, StrokeCap.Round)
            }

            EvIcon.Microphone -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.37f, h * 0.16f),
                    size = Size(w * 0.26f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.13f, w * 0.13f),
                    style = Stroke(stroke)
                )
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.25f, h * 0.28f),
                    size = Size(w * 0.50f, h * 0.46f),
                    style = Stroke(stroke)
                )
                drawLine(tint, Offset(w * 0.50f, h * 0.74f), Offset(w * 0.50f, h * 0.86f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.37f, h * 0.86f), Offset(w * 0.63f, h * 0.86f), stroke, StrokeCap.Round)
            }

            EvIcon.Account -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.16f,
                    center = Offset(w * 0.50f, h * 0.32f),
                    style = Stroke(stroke)
                )
                drawArc(
                    color = tint,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.34f),
                    size = Size(w * 0.56f, h * 0.48f),
                    style = Stroke(stroke)
                )
            }

            EvIcon.ChevronRight -> {
                drawLine(tint, Offset(w * 0.40f, h * 0.27f), Offset(w * 0.62f, h * 0.50f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.62f, h * 0.50f), Offset(w * 0.40f, h * 0.73f), stroke, StrokeCap.Round)
            }

            EvIcon.Send -> {
                val path = Path().apply {
                    moveTo(w * 0.16f, h * 0.48f)
                    lineTo(w * 0.84f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.84f)
                    lineTo(w * 0.48f, h * 0.57f)
                    close()
                }
                drawPath(path, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(tint, Offset(w * 0.48f, h * 0.57f), Offset(w * 0.84f, h * 0.16f), stroke, StrokeCap.Round)
            }

            EvIcon.Info -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.40f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = Stroke(stroke)
                )
                drawCircle(
                    color = tint,
                    radius = stroke * 0.85f,
                    center = Offset(w * 0.50f, h * 0.35f)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.47f),
                    end = Offset(w * 0.50f, h * 0.68f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            EvIcon.Check -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.52f), Offset(w * 0.42f, h * 0.72f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.72f), Offset(w * 0.78f, h * 0.32f), stroke, StrokeCap.Round)
            }

            EvIcon.Close -> {
                drawLine(tint, Offset(w * 0.30f, h * 0.30f), Offset(w * 0.70f, h * 0.70f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.30f), Offset(w * 0.30f, h * 0.70f), stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
internal fun EvMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = "EV logo"
) {
    val markModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Image(
        painter = painterResource(R.drawable.ic_ev_logo),
        contentDescription = contentDescription,
        modifier = markModifier
    )
}

/** Bottom composer. The parent owns message submission and microphone behavior. */
@Composable
internal fun AskBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {},
    onMicrophoneClick: () -> Unit = {},
    isListening: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val shape = OneUiShapes.Composer
    val hasInput = value.isNotBlank()
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    fun submit() {
        if (hasInput) {
            onSubmit()
            keyboardController?.hide()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = OneUiDimensions.ScreenHorizontalCompact,
                vertical = OneUiDimensions.Space2
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = OneUiElevation.surfaceAlpha(darkTheme)
            )
        ),
        border = BorderStroke(
            width = OneUiElevation.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = OneUiElevation.borderAlpha(darkTheme))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = OneUiElevation.Floating)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = OneUiDimensions.ComposerMinimumHeight)
                .padding(horizontal = OneUiDimensions.Space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvMark(
                modifier = Modifier.size(OneUiDimensions.IconLarge),
                contentDescription = null
            )

            Spacer(Modifier.width(OneUiDimensions.Space2))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(OneUiDimensions.TextFieldHeight)
                    .padding(horizontal = OneUiDimensions.Space1 / 2),
                placeholder = {
                    Text(
                        text = "Ask EV",
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
                keyboardActions = KeyboardActions(onSend = { submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (hasInput) {
                EvIconButton(
                    icon = EvIcon.Send,
                    description = "Send message",
                    onClick = ::submit
                )
            } else {
                val listeningPulse = OneUiMotion.rememberListeningPulse()
                val pulseScale = if (isListening) listeningPulse.scale.value else 1f
                val pulseAlpha = if (isListening) listeningPulse.alpha.value else 0f

                Box(
                    modifier = Modifier
                        .size(OneUiDimensions.IconButton)
                        .clip(OneUiShapes.Full),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(MaterialTheme.colorScheme.primary)
                    )
                    EvIconButton(
                        icon = EvIcon.Microphone,
                        description = if (isListening) "Stop voice input" else "Use voice input",
                        onClick = onMicrophoneClick,
                        tint = if (isListening) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun SuggestionPill(
    text: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = OneUiMotion.PressSpring,
        label = "suggestion_pill_scale"
    )

    val shape = OneUiShapes.Suggestion

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.62f),
                shape = shape
            )
            .border(
                width = OneUiElevation.Hairline,
                color = borderColor.copy(alpha = 0.9f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = borderColor),
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = text
            }
            .heightIn(
                min = OneUiDimensions.MinimumTouchTarget
            )
            .padding(
                horizontal = OneUiDimensions.Space4,
                vertical = OneUiDimensions.Space3
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = OneUiTypography.Label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AccountCard(
    email: String? = null,
    onClick: () -> Unit = {}
) {
    SettingsCard {
        val interactionSource = remember { MutableInteractionSource() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(OneUiShapes.CardCompact)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(
                    horizontal = OneUiDimensions.ListItemHorizontal,
                    vertical = OneUiDimensions.ListItemVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(OneUiDimensions.Avatar)
                    .clip(OneUiShapes.Avatar)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                EvIcon(
                    icon = EvIcon.Account,
                    modifier = Modifier.size(OneUiDimensions.IconLarge),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(OneUiDimensions.Space3))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(OneUiDimensions.Space1)
            ) {
                Text(
                    text = "Google account",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = OneUiTypography.BodyMedium
                )
                Text(
                    text = email?.takeIf { it.isNotBlank() }?.let { "Signed in as $it" }
                        ?: "Connect your Google account",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = OneUiTypography.Caption
                )
            }

            EvIcon(
                icon = EvIcon.ChevronRight,
                modifier = Modifier.size(OneUiDimensions.IconMedium),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = OneUiTypography.SectionTitle,
        modifier = modifier.padding(
            start = OneUiDimensions.Space1,
            top = OneUiDimensions.SectionTop,
            bottom = OneUiDimensions.SectionBottom
        )
    )
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = OneUiShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = OneUiElevation.surfaceAlpha(darkTheme)
            )
        ),
        border = BorderStroke(
            width = OneUiElevation.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = OneUiElevation.borderAlpha(darkTheme))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = OneUiElevation.Raised)
    ) {
        Column { content() }
    }
}

/** Optional click behavior: non-clickable rows don't advertise a false affordance. */
@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowShape = OneUiShapes.ListItem
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(rowShape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = { onClick() }
                )
            } else {
                Modifier
            }
        )
        .heightIn(min = OneUiDimensions.MinimumTouchTarget)
        .padding(
        horizontal = OneUiDimensions.ListItemHorizontal,
        vertical = OneUiDimensions.ListItemVertical
        )

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.45f
                ),
                style = OneUiTypography.BodyMedium
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(OneUiDimensions.Space1))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.45f
                    ),
                    style = OneUiTypography.Caption
                )
            }
        }

        if (!trailingText.isNullOrBlank()) {
            Spacer(Modifier.width(OneUiDimensions.Space3))
            Text(
                text = trailingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = OneUiTypography.Caption
            )
        }

        if (onClick != null) {
            Spacer(Modifier.width(OneUiDimensions.Space1))
            EvIcon(
                icon = EvIcon.ChevronRight,
                modifier = Modifier.size(OneUiDimensions.IconSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.45f
                )
            )
        }
    }
}

/** Locally managed toggle, with a callback for persisting the new value in the parent. */
@Composable
internal fun ToggleRow(
    title: String,
    initiallyChecked: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    var checked by rememberSaveable(title, initiallyChecked) {
        mutableStateOf(initiallyChecked)
    }
    androidx.compose.runtime.LaunchedEffect(initiallyChecked) {
        checked = initiallyChecked
    }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OneUiShapes.InnerContainer)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = ripple(),
                onValueChange = { newValue ->
                    checked = newValue
                    onCheckedChange(newValue)
                }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.45f
                ),
                style = OneUiTypography.BodyMedium
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(OneUiDimensions.Space1))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.45f
                    ),
                    style = OneUiTypography.Caption
                )
            }
        }

        Spacer(Modifier.width(OneUiDimensions.Space4))

        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier
                .size(
                    width = OneUiDimensions.SwitchWidth,
                    height = OneUiDimensions.SwitchHeight
                )
                .scale(
                    animateFloatAsState(
                        targetValue = if (checked) 0.70f else 0.70f,
                        animationSpec = OneUiMotion.ControlSpring,
                        label = "switch_scale"
                    ).value
                ),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OneUiColors.ControlActive,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                    OneUiColors.ControlTrackDark
                } else {
                    OneUiColors.ControlTrackLight
                },
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
                disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        )
    }
}

@Composable
internal fun SettingsDivider(modifier: Modifier = Modifier) {
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    Spacer(
        modifier
            .fillMaxWidth()
            .padding(horizontal = OneUiDimensions.DividerHorizontal)
            .height(OneUiElevation.Hairline)
            .background(
                MaterialTheme.colorScheme.outlineVariant.copy(
                    alpha = OneUiElevation.borderAlpha(darkTheme)
                )
            )
    )
}
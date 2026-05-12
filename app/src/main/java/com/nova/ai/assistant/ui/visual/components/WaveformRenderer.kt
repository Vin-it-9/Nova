package com.nova.ai.assistant.ui.visual.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nova.ai.assistant.ui.visual.reaction.WaveformReactionModel
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformRenderer(
    reaction: WaveformReactionModel,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2200f / reaction.speed.coerceAtLeast(0.18f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val baseAmplitude = (size.height * 0.18f * reaction.amplitude.coerceIn(0f, 1f))
            .coerceAtLeast(size.height * 0.02f)
        val centerY = size.height / 2f

        drawWaveLayer(
            centerY = centerY,
            amplitude = baseAmplitude * 0.8f,
            thickness = reaction.lineThickness,
            color = color.copy(alpha = reaction.alpha * 0.5f),
            phase = phase,
            frequencyScale = 1f,
            softness = reaction.softness
        )
        drawWaveLayer(
            centerY = centerY,
            amplitude = baseAmplitude,
            thickness = reaction.lineThickness + 0.3f,
            color = color.copy(alpha = reaction.alpha * 0.72f),
            phase = phase + 0.22f,
            frequencyScale = 1.22f,
            softness = reaction.softness
        )
        drawWaveLayer(
            centerY = centerY,
            amplitude = baseAmplitude * 1.16f,
            thickness = reaction.lineThickness + 0.6f,
            color = color.copy(alpha = reaction.alpha),
            phase = phase + 0.47f,
            frequencyScale = 1.5f,
            softness = reaction.softness
        )
    }
}

private fun DrawScope.drawWaveLayer(
    centerY: Float,
    amplitude: Float,
    thickness: Float,
    color: Color,
    phase: Float,
    frequencyScale: Float,
    softness: Float
) {
    val path = Path()
    val waveLength = size.width / (1.6f * frequencyScale)
    val points = 56
    val step = size.width / points

    var x = 0f
    var lastY = centerY
    path.moveTo(0f, centerY)
    repeat(points + 1) { index ->
        val progress = index / points.toFloat()
        val radians = ((x / waveLength) + phase) * (2f * PI).toFloat()
        val envelope = (0.66f + (0.34f * sin(progress * PI).toFloat()))
        val y = centerY + (sin(radians) * amplitude * envelope * softness)
        val midX = (x + (x - step)) * 0.5f
        val midY = (y + lastY) * 0.5f
        path.quadraticBezierTo(x - step, lastY, midX, midY)
        lastY = y
        x += step
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = thickness, cap = StrokeCap.Round)
    )
}

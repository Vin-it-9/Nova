package com.nova.ai.assistant.ui.visual.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.nova.ai.assistant.ui.theme.AssistantVisualPalette
import com.nova.ai.assistant.ui.visual.reaction.OrbReactionModel
import com.nova.ai.assistant.ui.visual.shader.ShaderManager
import com.nova.ai.assistant.ui.visual.shader.rememberShaderManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbRenderer(
    palette: AssistantVisualPalette,
    reaction: OrbReactionModel,
    modifier: Modifier = Modifier,
    shaderManager: ShaderManager = rememberShaderManager()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbInternal")
    
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000f / reaction.pulseSpeedHz.coerceAtLeast(0.1f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathingPhase"
    )

    val instabilityPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "instabilityPhase"
    )

    var timeMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { timeMillis = it }
        }
    }

    val orbShader = remember(shaderManager) { shaderManager.createOrbShader() }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension / 2.2f

        // Dynamic pulse and breathing factors
        val pulseFactor = sin(pulsePhase * 2f * PI.toFloat()) * reaction.pulseAmplitude
        val breathingFactor = sin(breathingPhase * 2f * PI.toFloat()) * reaction.breathingAmplitude

        val dynamicScale = reaction.scaleMultiplier + pulseFactor + breathingFactor
        val radius = baseRadius * dynamicScale

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && orbShader != null) {
            orbShader.setFloatUniform("iResolution", size.width, size.height)
            orbShader.setFloatUniform("iTime", timeMillis / 1000f)
            orbShader.setFloatUniform("glowBoost", reaction.glowBoost)
            
            // Adjust colors based on alpha
            val innerWithAlpha = palette.orbInner.copy(alpha = palette.orbInner.alpha * reaction.alpha)
            val outerWithAlpha = palette.orbOuter.copy(alpha = palette.orbOuter.alpha * reaction.alpha)
            
            orbShader.setColorUniform("innerColor", innerWithAlpha.value.toInt())
            orbShader.setColorUniform("outerColor", outerWithAlpha.value.toInt())
            
            // For shader rendering we can just draw a rect covering the canvas
            drawRect(brush = ShaderBrush(orbShader))
        } else {
            // Draw Glow/Halo
            drawOrbHalo(
                center = center,
                radius = radius,
                palette = palette,
                glowBoost = reaction.glowBoost,
                alpha = reaction.alpha
            )

            // Draw Core with Instability
            drawOrbCore(
                center = center,
                radius = radius,
                palette = palette,
                instability = reaction.instability,
                instabilityPhase = instabilityPhase,
                alpha = reaction.alpha
            )
        }
    }
}


private fun DrawScope.drawOrbHalo(
    center: Offset,
    radius: Float,
    palette: AssistantVisualPalette,
    glowBoost: Float,
    alpha: Float
) {
    val haloRadius = radius * (1.35f + (glowBoost * 0.4f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.orbHalo.copy(alpha = (0.22f + (glowBoost * 0.35f)) * alpha),
                palette.orbHalo.copy(alpha = (0.08f + (glowBoost * 0.15f)) * alpha),
                Color.Transparent
            ),
            center = center,
            radius = haloRadius
        ),
        radius = haloRadius,
        center = center
    )
}

private fun DrawScope.drawOrbCore(
    center: Offset,
    radius: Float,
    palette: AssistantVisualPalette,
    instability: Float,
    instabilityPhase: Float,
    alpha: Float
) {
    // Apply subtle instability to center for "alive" feel
    val offsetX = cos(instabilityPhase * 2f * PI.toFloat()) * radius * instability
    val offsetY = sin(instabilityPhase * 2f * PI.toFloat() * 1.3f) * radius * instability
    val dynamicCenter = center + Offset(offsetX, offsetY)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                palette.orbInner.copy(alpha = 0.98f * alpha),
                palette.orbOuter.copy(alpha = 0.85f * alpha),
                palette.orbOuter.copy(alpha = 0.4f * alpha),
                Color.Transparent
            ),
            center = dynamicCenter,
            radius = radius * 1.08f
        ),
        radius = radius,
        center = dynamicCenter
    )
    
    // Inner highlight
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f * alpha),
                Color.Transparent
            ),
            center = dynamicCenter - Offset(radius * 0.2f, radius * 0.2f),
            radius = radius * 0.6f
        ),
        radius = radius * 0.6f,
        center = dynamicCenter - Offset(radius * 0.2f, radius * 0.2f)
    )
}

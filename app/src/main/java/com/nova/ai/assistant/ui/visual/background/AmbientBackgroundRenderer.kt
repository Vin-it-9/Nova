package com.nova.ai.assistant.ui.visual.background

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
import androidx.compose.ui.graphics.ShaderBrush
import com.nova.ai.assistant.ui.theme.AssistantVisualPalette
import com.nova.ai.assistant.ui.visual.particles.ParticleEngine
import com.nova.ai.assistant.ui.visual.particles.rememberParticleEngine
import com.nova.ai.assistant.ui.visual.shader.ShaderManager
import com.nova.ai.assistant.ui.visual.shader.rememberShaderManager

@Composable
fun AmbientBackgroundRenderer(
    palette: AssistantVisualPalette,
    intensity: Float,
    modifier: Modifier = Modifier,
    shaderManager: ShaderManager = rememberShaderManager(),
    particleEngine: ParticleEngine = rememberParticleEngine()
) {
    val transition = rememberInfiniteTransition(label = "ambientBackground")
    val driftPrimary by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftPrimary"
    )
    val driftSecondary by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftSecondary"
    )

    var timeMillis by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { timeMillis = it }
        }
    }

    val liquidShader = remember(shaderManager) { shaderManager.createLiquidShader() }

    Canvas(modifier = modifier) {
        val normalizedIntensity = intensity.coerceIn(0f, 1f)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && liquidShader != null) {
            liquidShader.setFloatUniform("iResolution", size.width, size.height)
            liquidShader.setFloatUniform("iTime", timeMillis / 1000f)
            liquidShader.setFloatUniform("intensity", normalizedIntensity)
            liquidShader.setColorUniform("color1", palette.backgroundTop.value.toInt())
            liquidShader.setColorUniform("color2", palette.backgroundBottom.value.toInt())
            
            drawRect(brush = ShaderBrush(liquidShader))
        } else {
            // Fallback renderer
            val start = Offset(
                x = size.width * (0.14f + (driftPrimary * 0.24f)),
                y = size.height * (0.1f + (driftSecondary * 0.22f))
            )
            val end = Offset(
                x = size.width * (0.86f - (driftPrimary * 0.2f)),
                y = size.height * (0.9f - (driftSecondary * 0.24f))
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.backgroundTop.copy(alpha = 0.92f + (normalizedIntensity * 0.08f)),
                        palette.backgroundBottom
                    ),
                    start = start,
                    end = end
                )
            )

            val haloCenter = Offset(
                x = size.width * (0.3f + (driftPrimary * 0.36f)),
                y = size.height * (0.18f + (driftSecondary * 0.18f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.orbHalo.copy(alpha = 0.12f + (normalizedIntensity * 0.18f)),
                        palette.backgroundBottom.copy(alpha = 0f)
                    ),
                    center = haloCenter,
                    radius = size.minDimension * (0.62f + (normalizedIntensity * 0.1f))
                ),
                radius = size.minDimension * (0.62f + (normalizedIntensity * 0.1f)),
                center = haloCenter
            )
        }

        // Draw ambient particles
        particleEngine.updateAndDraw(
            drawScope = this,
            palette = palette,
            timeMillis = timeMillis,
            intensity = normalizedIntensity
        )
    }
}

package com.nova.ai.assistant.ui.visual.particles

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.nova.ai.assistant.ui.theme.AssistantVisualPalette
import kotlin.random.Random
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis

data class Particle(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var radius: Float,
    var alpha: Float,
    var phase: Float
)

class ParticleEngine {
    private var particles = mutableListOf<Particle>()
    private var initialized = false

    fun updateAndDraw(
        drawScope: DrawScope,
        palette: AssistantVisualPalette,
        timeMillis: Long,
        intensity: Float
    ) {
        val count = 20
        if (!initialized) {
            particles.clear()
            for (i in 0 until count) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * drawScope.size.width,
                        y = Random.nextFloat() * drawScope.size.height,
                        speedX = (Random.nextFloat() - 0.5f) * 0.5f,
                        speedY = -Random.nextFloat() * 1.5f - 0.5f,
                        radius = Random.nextFloat() * 3f + 1f,
                        alpha = Random.nextFloat() * 0.4f + 0.1f,
                        phase = Random.nextFloat() * 100f
                    )
                )
            }
            initialized = true
        }

        val dynamicSpeed = 1f + intensity * 2f
        val dynamicAlpha = 1f + intensity * 0.5f

        particles.forEach { p ->
            p.y += p.speedY * dynamicSpeed
            p.x += (p.speedX + kotlin.math.sin(timeMillis * 0.001f + p.phase) * 0.5f).toFloat() * dynamicSpeed

            if (p.y < -10f) {
                p.y = drawScope.size.height + 10f
                p.x = Random.nextFloat() * drawScope.size.width
            }
            if (p.x < -10f) p.x = drawScope.size.width + 10f
            if (p.x > drawScope.size.width + 10f) p.x = -10f

            val renderAlpha = (p.alpha * dynamicAlpha).coerceIn(0f, 0.6f)
            drawScope.drawCircle(
                color = palette.orbHalo.copy(alpha = renderAlpha),
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }
    }
}

@Composable
fun rememberParticleEngine(): ParticleEngine {
    return remember { ParticleEngine() }
}

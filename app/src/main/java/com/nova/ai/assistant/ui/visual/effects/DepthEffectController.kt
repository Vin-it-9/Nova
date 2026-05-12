package com.nova.ai.assistant.ui.visual.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.os.Build

class DepthEffectController {
    
    fun calculateBlurRadius(intensity: Float, isOverlay: Boolean): Float {
        val baseBlur = if (isOverlay) 16f else 0f
        return baseBlur + (intensity * 24f)
    }

    fun applyGlassmorphism(
        modifier: Modifier,
        blurRadius: Float,
        alpha: Float = 1f
    ): Modifier {
        if (blurRadius <= 0.1f) return modifier.graphicsLayer { this.alpha = alpha }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modifier.graphicsLayer {
                this.alpha = alpha
                this.renderEffect = androidx.compose.ui.graphics.BlurEffect(
                    radiusX = blurRadius,
                    radiusY = blurRadius,
                    edgeTreatment = androidx.compose.ui.graphics.TileMode.Clamp
                )
            }
        } else {
            modifier.graphicsLayer { this.alpha = alpha }
        }
    }
}

@Composable
fun rememberDepthEffectController(): DepthEffectController {
    return remember { DepthEffectController() }
}

package com.nova.ai.assistant.ui.visual.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import android.os.SystemClock

class VisualPerformanceOptimizer {
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var fps = 60f

    fun onFrame() {
        val now = SystemClock.elapsedRealtime()
        frameCount++
        if (now - lastFrameTime > 1000) {
            fps = frameCount * 1000f / (now - lastFrameTime)
            frameCount = 0
            lastFrameTime = now
        }
    }

    fun shouldEnableHeavyShaders(): Boolean {
        // Simple heuristic: disable heavy shaders if FPS drops below 45
        return fps >= 45f
    }
    
    fun getAdaptiveQualityScale(): Float {
        return if (fps > 55f) 1f else if (fps > 45f) 0.8f else 0.5f
    }
}

@Composable
fun rememberVisualPerformanceOptimizer(): VisualPerformanceOptimizer {
    return remember { VisualPerformanceOptimizer() }
}

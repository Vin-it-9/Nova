package com.nova.ai.assistant.ui.visual.shader

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class ShaderManager {
    // AGSL shader for a liquid ambient gradient
    private val liquidGradientShaderSrc = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float4 color1;
        uniform float4 color2;
        uniform float intensity;
        
        vec4 main(in float2 fragCoord) {
            vec2 uv = fragCoord.xy / iResolution.xy;
            float t = iTime * 0.4 * (1.0 + intensity);
            
            vec2 p = uv * 2.0 - 1.0;
            p.x *= iResolution.x / iResolution.y;
            
            float a = atan(p.y, p.x);
            float r = length(p);
            
            float v = sin(r * 3.0 - t) + cos(a * 2.0 + t * 0.8) * sin(uv.x * 4.0);
            float blend = smoothstep(-1.5, 1.5, v);
            
            return mix(color1, color2, blend * 0.5 + 0.5);
        }
    """.trimIndent()

    // AGSL shader for a premium orb glow/bloom
    private val orbGlowShaderSrc = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float4 innerColor;
        uniform float4 outerColor;
        uniform float glowBoost;
        
        vec4 main(in float2 fragCoord) {
            vec2 uv = fragCoord.xy / iResolution.xy;
            vec2 center = vec2(0.5, 0.5);
            float d = distance(uv, center) * 2.0;
            
            float pulse = sin(iTime * 2.0) * 0.1 * glowBoost;
            float radius = 0.6 + pulse;
            
            float core = 1.0 - smoothstep(0.0, radius, d);
            float halo = 1.0 - smoothstep(radius * 0.8, 1.5 + glowBoost * 0.5, d);
            
            vec4 color = mix(outerColor, innerColor, core);
            return color * halo * (1.0 + glowBoost);
        }
    """.trimIndent()

    fun createLiquidShader(): RuntimeShader? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(liquidGradientShaderSrc)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun createOrbShader(): RuntimeShader? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                RuntimeShader(orbGlowShaderSrc)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}

@Composable
fun rememberShaderManager(): ShaderManager {
    return remember { ShaderManager() }
}

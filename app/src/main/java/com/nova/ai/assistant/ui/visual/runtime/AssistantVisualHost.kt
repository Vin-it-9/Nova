package com.nova.ai.assistant.ui.visual.runtime

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.ai.assistant.ui.overlay.OverlayController
import com.nova.ai.assistant.ui.overlay.OverlayState
import com.nova.ai.assistant.ui.theme.AssistantThemeEngine
import com.nova.ai.assistant.ui.visual.background.AmbientBackgroundRenderer
import com.nova.ai.assistant.ui.visual.components.OrbRenderer
import com.nova.ai.assistant.ui.visual.components.WaveformRenderer
import com.nova.ai.assistant.ui.visual.presence.AssistantPresenceController
import com.nova.ai.assistant.ui.visual.reaction.VisualReactionEngine
import com.nova.ai.assistant.ui.visual.transition.AssistantTransitionSystem
import com.nova.ai.assistant.ui.visual.effects.DepthEffectController
import com.nova.ai.assistant.ui.visual.effects.rememberDepthEffectController
import com.nova.ai.assistant.ui.visual.emotion.EmotionalVisualController
import com.nova.ai.assistant.ui.visual.emotion.rememberEmotionalVisualController
import com.nova.ai.assistant.ui.visual.performance.VisualPerformanceOptimizer
import com.nova.ai.assistant.ui.visual.performance.rememberVisualPerformanceOptimizer
import kotlinx.coroutines.launch

@Composable
fun AssistantVisualHost(
    nativeCoreReady: Boolean,
    modifier: Modifier = Modifier,
    coordinator: VisualStateCoordinator = remember { VisualStateCoordinator() },
    reactionEngine: VisualReactionEngine = remember { VisualReactionEngine() },
    transitionSystem: AssistantTransitionSystem = remember { AssistantTransitionSystem() },
    intensityController: VisualIntensityController = remember { VisualIntensityController() },
    presenceController: AssistantPresenceController = remember { AssistantPresenceController() },
    overlayController: OverlayController = remember { OverlayController() },
    themeEngine: AssistantThemeEngine = remember { AssistantThemeEngine() },
    depthController: DepthEffectController = rememberDepthEffectController(),
    emotionController: EmotionalVisualController = rememberEmotionalVisualController(),
    performanceOptimizer: VisualPerformanceOptimizer = rememberVisualPerformanceOptimizer()
) {
    val scope = rememberCoroutineScope()
    
    DisposableEffect(coordinator) {
        coordinator.start()
        onDispose {
            coordinator.dispose()
        }
    }

    LaunchedEffect(nativeCoreReady) {
        coordinator.setNativeReady(nativeCoreReady)
    }

    val visualState by coordinator.visualState.collectAsState()
    val overlayState by overlayController.state.collectAsState()
    val palette = remember(visualState) { themeEngine.paletteFor(visualState) }

    // React to visual state changes across all controllers
    LaunchedEffect(visualState) {
        scope.launch {
            intensityController.onVisualState(visualState)
            presenceController.onVisualState(visualState)
            transitionSystem.onVisualStateChanged(visualState.semantic)
            reactionEngine.onVisualState(visualState)
            emotionController.onVisualState(visualState)
        }
    }

    // Observe output flows
    val intensity by intensityController.profile.collectAsState()
    val presence by presenceController.profile.collectAsState()
    val transitionState by transitionSystem.state.collectAsState()
    val reaction by reactionEngine.reaction.collectAsState()
    val emotionProfile by emotionController.profile.collectAsState()

    // Sync reaction engine with intensity and presence
    LaunchedEffect(intensity, presence) {
        reactionEngine.onIntensity(intensity)
        reactionEngine.onPresence(presence)
    }

    // Smoothly animated properties driven by reaction model and transition specs
    val orbSpring = transitionSystem.orbSpringSpec()
    val waveformSpring = transitionSystem.waveformSpringSpec()

    val animatedOrbScale by animateFloatAsState(
        targetValue = reaction.orb.scaleMultiplier + transitionState.orbScaleBoost,
        animationSpec = orbSpring,
        label = "orbScale"
    )
    val animatedOrbAlpha by animateFloatAsState(
        targetValue = reaction.orb.alpha,
        animationSpec = orbSpring,
        label = "orbAlpha"
    )
    val animatedWaveformAlpha by animateFloatAsState(
        targetValue = (reaction.waveform.alpha + transitionState.waveformAlphaBoost).coerceAtMost(1f),
        animationSpec = waveformSpring,
        label = "waveformAlpha"
    )
    val animatedLabelAlpha by animateFloatAsState(
        targetValue = reaction.statusAlpha,
        animationSpec = transitionSystem.cinematicFadeSpec(),
        label = "labelAlpha"
    )
    val animatedDepthBlur by animateFloatAsState(
        targetValue = transitionState.depthBlurRadius,
        animationSpec = transitionSystem.cinematicFadeSpec(),
        label = "depthBlur"
    )

    // Performance frame tick
    LaunchedEffect(Unit) {
        while(true) {
            withFrameNanos { performanceOptimizer.onFrame() }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // LAYER 1: Ambient Background (True Fullscreen)
        AmbientBackgroundRenderer(
            palette = palette,
            intensity = intensity.level + emotionProfile.colorShiftBoost,
            modifier = depthController.applyGlassmorphism(Modifier.fillMaxSize(), animatedDepthBlur)
        )

        // LAYER 2: Waveform
        WaveformRenderer(
            reaction = reaction.waveform.copy(alpha = animatedWaveformAlpha),
            color = palette.orbHalo,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
        )

        // LAYER 3: Assistant Orb
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            OrbRenderer(
                palette = palette,
                reaction = reaction.orb.copy(
                    scaleMultiplier = animatedOrbScale,
                    alpha = animatedOrbAlpha
                ),
                modifier = Modifier
                    .size(220.dp) // Slightly larger base size for premium feel
                    .graphicsLayer {
                        // Internal scaling is handled by OrbRenderer
                    }
            )
        }

        // LAYER 4: Status / Presence UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 92.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedContent(
                targetState = visualState.statusLabel,
                transitionSpec = {
                    (fadeIn(animationSpec = transitionSystem.overlayFadeSpec()) + 
                     transitionSystem.orbSpringSpec().let { spring -> 
                         // Use spring for a subtle bounce on text change if desired, 
                         // but crossfade is usually cleaner for OS feel.
                         fadeIn() 
                     }).togetherWith(fadeOut(animationSpec = transitionSystem.overlayFadeSpec()))
                },
                label = "statusLabelTransition"
            ) { label ->
                Text(
                    text = label.uppercase(),
                    color = palette.text.copy(alpha = animatedLabelAlpha),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = animatedLabelAlpha
                    }
                )
            }
        }

        // LAYER 5: Overlay Indicators
        if (overlayState == OverlayState.InlineVisible || overlayState == OverlayState.DetachedRequested) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(24.dp)
                    .size(8.dp)
                    .drawBehind {
                        drawCircle(color = palette.orbHalo.copy(alpha = 0.8f))
                    }
            )
        }
    }
}

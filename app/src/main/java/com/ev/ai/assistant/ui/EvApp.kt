package com.ev.ai.assistant.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.ev.ai.assistant.ui.screens.HomeScreen
import com.ev.ai.assistant.ui.screens.SettingsScreen
import com.ev.ai.assistant.ui.screens.settings.AboutScreen
import com.ev.ai.assistant.ui.screens.settings.PermissionsScreen
import com.ev.ai.assistant.ui.screens.settings.VoiceWakeUpScreen
import com.ev.ai.assistant.ui.util.OneUiColors
import com.ev.ai.assistant.ui.util.OneUiMotion

enum class AppScreen {
    Home,
    Settings,
    About,
    Permissions,
    VoiceWakeUp
}

@Composable
fun EvApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    val darkTheme = isSystemInDarkTheme()

    BackHandler(enabled = currentScreen != AppScreen.Home) {
        currentScreen = when (currentScreen) {
            AppScreen.VoiceWakeUp -> AppScreen.Settings
            AppScreen.Permissions -> AppScreen.Settings
            AppScreen.About -> AppScreen.Settings
            AppScreen.Settings -> AppScreen.Home
            AppScreen.Home -> AppScreen.Home
        }
    }

    MaterialTheme(colorScheme = OneUiColors.colorScheme(darkTheme)) {
        AnimatedContent(
            targetState = currentScreen,
            label = "one_ui_screen_transition",
            transitionSpec = {
                val slideSpec = OneUiMotion.screenSpec<IntOffset>()
                val fadeSpec = OneUiMotion.screenSpec<Float>()
                val depthSpec = OneUiMotion.screenSpec<Float>()

                val isForward = targetState.ordinal > initialState.ordinal

                if (isForward) {
                    (
                        slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { (it * OneUiMotion.ScreenIncomingOffsetFraction).toInt() }
                        ) + fadeIn(
                            animationSpec = fadeSpec,
                            initialAlpha = 0f
                        )
                    ).togetherWith(
                        fadeOut(
                            animationSpec = fadeSpec,
                            targetAlpha = 0f
                        ) + scaleOut(
                            animationSpec = depthSpec,
                            targetScale = OneUiMotion.ScreenOutgoingScale
                        )
                    )
                } else {
                    (
                        slideInHorizontally(
                            animationSpec = slideSpec,
                            initialOffsetX = { -(it * OneUiMotion.ScreenIncomingOffsetFraction).toInt() }
                        ) + fadeIn(
                            animationSpec = fadeSpec,
                            initialAlpha = 0f
                        )
                    ).togetherWith(
                        fadeOut(
                            animationSpec = fadeSpec,
                            targetAlpha = 0f
                        ) + scaleOut(
                            animationSpec = depthSpec,
                            targetScale = OneUiMotion.ScreenOutgoingScale
                        )
                    )
                }
            }
        ) { screen ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
            ) {
                when (screen) {
                    AppScreen.Home -> HomeScreen(
                        onOpenSettings = { currentScreen = AppScreen.Settings }
                    )
                    AppScreen.Settings -> SettingsScreen(
                        onBack = { currentScreen = AppScreen.Home },
                        onOpenAbout = { currentScreen = AppScreen.About },
                        onOpenPermissions = { currentScreen = AppScreen.Permissions },
                        onOpenVoiceWakeUp = { currentScreen = AppScreen.VoiceWakeUp }
                    )
                    AppScreen.About -> AboutScreen(
                        onBack = { currentScreen = AppScreen.Settings }
                    )
                    AppScreen.Permissions -> PermissionsScreen(
                        onBack = { currentScreen = AppScreen.Settings }
                    )
                    AppScreen.VoiceWakeUp -> VoiceWakeUpScreen(
                        onBack = { currentScreen = AppScreen.Settings }
                    )
                }
            }
        }
    }
}
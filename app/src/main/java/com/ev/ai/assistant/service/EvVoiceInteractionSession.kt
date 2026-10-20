package com.ev.ai.assistant.service

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ev.ai.assistant.ui.overlay.EvAssistantOverlayPanel
import com.ev.ai.assistant.ui.util.EvTheme

/**
 * File C: EvVoiceInteractionSession
 *
 * System VoiceInteractionSession implementation managing the floating overlay window
 * lifecycle and Jetpack Compose render pipeline with full ViewTree Lifecycle/SavedState integration.
 */
class EvVoiceInteractionSession(context: Context) : VoiceInteractionSession(context),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val myViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = myViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var activePrompt by mutableStateOf("Listening...")

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Configure system window: transparent canvas and full-screen match_parent bounds
        window.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0f)
        }
    }

    override fun onCreateContentView(): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@EvVoiceInteractionSession)
            setViewTreeViewModelStoreOwner(this@EvVoiceInteractionSession)
            setViewTreeSavedStateRegistryOwner(this@EvVoiceInteractionSession)
            setContent {
                // The system session window has no ambient app theme, so apply the same
                // One UI theme here. EvTheme follows the device light/dark setting
                // automatically, which gives the overlay dark mode for free.
                EvTheme {
                    EvAssistantOverlayPanel(
                        initialPrompt = activePrompt,
                        onDismiss = {
                            // Collapse active overlay window seamlessly back into system layer
                            finish()
                        },
                        onSubmitQuery = { query ->
                            // Handle voice query completion and dismiss or update
                            activePrompt = "Processing: $query"
                        }
                    )
                }
            }
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        val promptArg = args?.getString(KEY_SESSION_PROMPT)
        activePrompt = promptArg ?: "Listening..."
    }

    override fun onHide() {
        super.onHide()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        activePrompt = "Listening..."
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        myViewModelStore.clear()
        super.onDestroy()
    }

    companion object {
        const val KEY_SESSION_PROMPT = "extra_session_prompt"
    }
}

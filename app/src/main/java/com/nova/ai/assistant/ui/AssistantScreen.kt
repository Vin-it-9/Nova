package com.nova.ai.assistant.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nova.ai.assistant.ui.visual.runtime.AssistantVisualHost

@Composable
fun AssistantScreen(
    nativeCoreReady: Boolean,
    modifier: Modifier = Modifier
) {
    AssistantVisualHost(
        nativeCoreReady = nativeCoreReady,
        modifier = modifier.fillMaxSize()
    )
}

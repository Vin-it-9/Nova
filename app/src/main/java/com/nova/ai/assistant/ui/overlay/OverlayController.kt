package com.nova.ai.assistant.ui.overlay

import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface OverlayRuntimeContract {
    val state: StateFlow<OverlayState>

    suspend fun showInline()
    suspend fun hide()
    suspend fun requestDetachedOverlay()
}

sealed interface OverlayState {
    data object Hidden : OverlayState
    data object InlineVisible : OverlayState
    data object DetachedRequested : OverlayState
}

class OverlayController(
    private val dispatcher: CoroutineDispatcher = RuntimeThreadingModel.runtimeDispatcher
) : OverlayRuntimeContract {
    private val _state = MutableStateFlow<OverlayState>(OverlayState.Hidden)
    override val state: StateFlow<OverlayState> = _state.asStateFlow()

    override suspend fun showInline() = withContext(dispatcher) {
        _state.value = OverlayState.InlineVisible
    }

    override suspend fun hide() = withContext(dispatcher) {
        _state.value = OverlayState.Hidden
    }

    override suspend fun requestDetachedOverlay() = withContext(dispatcher) {
        _state.value = OverlayState.DetachedRequested
    }
}

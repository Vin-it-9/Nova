package com.nova.ai.assistant.core.runtime

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RuntimeEventBus {
    private val events = MutableSharedFlow<RuntimeEvent>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun observe(): Flow<RuntimeEvent> = events.asSharedFlow()

    suspend fun publish(event: RuntimeEvent) {
        events.emit(event)
    }

    fun tryPublish(event: RuntimeEvent): Boolean = events.tryEmit(event)
}

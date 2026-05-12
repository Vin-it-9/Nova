package com.nova.ai.assistant.nativecore

import com.nova.ai.assistant.core.runtime.RuntimeEvent
import com.nova.ai.assistant.core.runtime.RuntimeEventBus
import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

object NativeCallbackChannel {
    private val callbackFlow = MutableSharedFlow<NativeCallbackEvent>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun observe(): Flow<NativeCallbackEvent> = callbackFlow.asSharedFlow()

    fun tryPublish(event: NativeCallbackEvent): Boolean {
        RuntimeEventBus.tryPublish(
            RuntimeEvent.Callback(
                channel = event.type,
                payload = event.payload,
                createdAtNanos = event.timestampNanos
            )
        )
        return callbackFlow.tryEmit(event)
    }

    suspend fun publish(event: NativeCallbackEvent) {
        withContext(RuntimeThreadingModel.callbackDispatcher) {
            RuntimeEventBus.publish(
                RuntimeEvent.Callback(
                    channel = event.type,
                    payload = event.payload,
                    createdAtNanos = event.timestampNanos
                )
            )
            callbackFlow.emit(event)
        }
    }

    data class NativeCallbackEvent(
        val type: String,
        val payload: String,
        val timestampNanos: Long = System.nanoTime()
    )
}

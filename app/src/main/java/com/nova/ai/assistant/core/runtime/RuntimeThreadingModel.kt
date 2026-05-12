package com.nova.ai.assistant.core.runtime

import java.util.concurrent.Executors
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

object RuntimeThreadingModel {
    val audioDispatcher: ExecutorCoroutineDispatcher = namedDispatcher("assistant-audio")
    val inferenceDispatcher: ExecutorCoroutineDispatcher = namedDispatcher("assistant-inference")
    val automationDispatcher: ExecutorCoroutineDispatcher = namedDispatcher("assistant-automation")
    val callbackDispatcher: ExecutorCoroutineDispatcher = namedDispatcher("assistant-callback")
    val nativeDispatcher: ExecutorCoroutineDispatcher = namedDispatcher("assistant-native")
    val runtimeDispatcher: ExecutorCoroutineDispatcher = namedDispatcher("assistant-runtime")

    fun shutdown() {
        audioDispatcher.close()
        inferenceDispatcher.close()
        automationDispatcher.close()
        callbackDispatcher.close()
        nativeDispatcher.close()
        runtimeDispatcher.close()
    }

    private fun namedDispatcher(name: String): ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, name).apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()
}

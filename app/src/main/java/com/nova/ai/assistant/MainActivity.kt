package com.nova.ai.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.nova.ai.assistant.core.runtime.AssistantStartupManager
import com.nova.ai.assistant.core.runtime.RuntimeThreadingModel
import com.nova.ai.assistant.nativecore.NativeBridge
import com.nova.ai.assistant.service.AssistantServiceController
import com.nova.ai.assistant.ui.AssistantScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val startupScope = CoroutineScope(SupervisorJob() + RuntimeThreadingModel.runtimeDispatcher)
    private lateinit var serviceController: AssistantServiceController
    private lateinit var startupManager: AssistantStartupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        serviceController = AssistantServiceController(applicationContext)
        startupManager = AssistantStartupManager(serviceController)

        val nativeCoreReady = NativeBridge.nativeRuntimePing()
        startupScope.launch {
            startupManager.start()
        }

        setContent {
            MaterialTheme {
                AssistantScreen(nativeCoreReady = nativeCoreReady)
            }
        }
    }

    override fun onDestroy() {
        startupScope.cancel()
        super.onDestroy()
    }
}

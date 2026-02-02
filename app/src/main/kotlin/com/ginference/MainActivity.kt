package com.ginference

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ginference.ui.screens.InferenceScreen
import com.ginference.ui.theme.CyberpunkTheme
import com.ginference.viewmodels.InferenceViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity created")
        Log.d(TAG, "Cache dir: ${cacheDir.absolutePath}")

        setContent {
            CyberpunkTheme {
                val viewModel: InferenceViewModel = viewModel()
                val state by viewModel.state.collectAsState()

                InferenceScreen(
                    messages = state.messages,
                    currentOutput = state.currentOutput,
                    isGenerating = state.isGenerating,
                    onSendPrompt = { prompt -> viewModel.sendPrompt(prompt) },
                    onStopGeneration = { viewModel.stopGeneration() },
                    ttft = state.ttft,
                    tokensPerSec = state.tokensPerSec,
                    ramUsage = state.ramUsage,
                    vramUsage = state.vramUsage,
                    cpuUsage = state.cpuUsage,
                    gpuUsage = state.gpuUsage,
                    temperature = state.temperature
                )
            }
        }
    }
}

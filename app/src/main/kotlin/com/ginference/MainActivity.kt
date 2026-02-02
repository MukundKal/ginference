package com.ginference

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ginference.inference.LLMEngine
import com.ginference.inference.ModelInfo
import com.ginference.inference.ModelManager
import com.ginference.ui.theme.CyberpunkBackground
import com.ginference.ui.theme.CyberpunkTheme
import com.ginference.ui.theme.CyberpunkTypography
import com.ginference.ui.theme.MatrixGreen
import com.ginference.ui.theme.NeonRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var llmEngine: LLMEngine
    private lateinit var modelManager: ModelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        llmEngine = LLMEngine(this)
        modelManager = ModelManager(this)

        Log.d(TAG, "MainActivity created")
        Log.d(TAG, "Cache dir: ${cacheDir.absolutePath}")

        setContent {
            CyberpunkTheme {
                GinferenceApp(llmEngine, modelManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmEngine.unload()
    }
}

@Composable
fun GinferenceApp(llmEngine: LLMEngine, modelManager: ModelManager) {
    var status by remember { mutableStateOf("GINFERENCE INITIALIZING...") }
    var modelInfo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        status = "SCANNING MODELS..."

        withContext(Dispatchers.IO) {
            val models = modelManager.getAllModels()
            val downloaded = modelManager.getDownloadedModels()
            val cacheSize = modelManager.getTotalCacheSize()
            val availableSpace = modelManager.getAvailableSpace()

            Log.d(TAG, "Found ${models.size} models, ${downloaded.size} downloaded")
            Log.d(TAG, "Cache: ${modelManager.formatSize(cacheSize)}, Free: ${modelManager.formatSize(availableSpace)}")

            withContext(Dispatchers.Main) {
                modelInfo = buildString {
                    appendLine("AVAILABLE MODELS: ${models.size}")
                    appendLine("DOWNLOADED: ${downloaded.size}")
                    appendLine("CACHE: ${modelManager.formatSize(cacheSize)}")
                    appendLine("FREE: ${modelManager.formatSize(availableSpace)}")
                    appendLine()
                    models.forEach { model ->
                        val isDownloaded = modelManager.isModelDownloaded(model)
                        val icon = if (isDownloaded) "✓" else "✗"
                        appendLine("$icon ${model.name} (${modelManager.formatSize(model.size)})")
                    }
                    appendLine()
                    appendLine("PHASE 1: BACKEND READY")
                    appendLine("PHASE 3: UI PENDING")
                }
                status = if (downloaded.isEmpty()) {
                    "NO MODELS DOWNLOADED"
                } else {
                    "READY: ${downloaded.size} MODEL(S)"
                }
            }

            Log.d(TAG, "Phase 1 checkpoint: Core inference ready")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberpunkBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = status,
                color = MatrixGreen,
                style = CyberpunkTypography.terminalLarge
            )

            if (modelInfo.isNotEmpty()) {
                Text(
                    text = modelInfo,
                    color = if (status.contains("ERROR")) NeonRed else MatrixGreen,
                    style = CyberpunkTypography.terminalSmall,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

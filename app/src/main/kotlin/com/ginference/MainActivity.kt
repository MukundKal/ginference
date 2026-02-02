package com.ginference

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ginference.ui.screens.InferenceScreen
import com.ginference.ui.theme.CyberpunkBackground
import com.ginference.ui.theme.CyberpunkTheme
import com.ginference.ui.theme.CyberpunkTypography
import com.ginference.ui.theme.MatrixGreen
import com.ginference.viewmodels.InferenceViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            Log.d(TAG, "Folder selected: $uri")
            handleFolderSelection(it)
        } ?: run {
            Log.w(TAG, "No folder selected, asking again")
            showFolderPicker()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity created")

        setContent {
            CyberpunkTheme {
                val viewModel: InferenceViewModel = viewModel()
                val state by viewModel.state.collectAsState()

                if (!state.isFolderSet) {
                    FolderSetupScreen()
                } else {
                    InferenceScreen(
                        messages = state.messages,
                        currentOutput = state.currentOutput,
                        isGenerating = state.isGenerating,
                        isModelLoaded = state.isModelLoaded,
                        modelName = state.modelName,
                        onSendPrompt = { prompt -> viewModel.sendPrompt(prompt) },
                        onStopGeneration = { viewModel.stopGeneration() },
                        ttft = state.ttft,
                        tokensPerSec = state.tokensPerSec,
                        ramUsage = state.ramUsage,
                        vramUsage = state.vramUsage,
                        cpuUsage = state.cpuUsage,
                        gpuUsage = state.gpuUsage,
                        temperature = state.temperature,
                        showModelSelector = state.showModelSelector,
                        availableModels = state.availableModels,
                        onShowModelSelector = { viewModel.showModelSelector() },
                        onHideModelSelector = { viewModel.hideModelSelector() },
                        onModelSelect = { model -> viewModel.selectModel(model) },
                        storagePath = state.storagePath,
                        cacheSize = state.cacheSize,
                        freeSpace = state.freeSpace
                    )
                }
            }
        }

        checkAndRequestFolderPermission()
    }

    private fun checkAndRequestFolderPermission() {
        val viewModel = (this as ComponentActivity).let {
            androidx.lifecycle.ViewModelProvider(it)[InferenceViewModel::class.java]
        }

        if (!viewModel.hasModelFolder()) {
            Log.d(TAG, "No model folder set, showing picker")
            showFolderPicker()
        } else {
            Log.d(TAG, "Model folder already set: ${viewModel.getModelFolderPath()}")
        }
    }

    private fun showFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        folderPickerLauncher.launch(null)
    }

    private fun handleFolderSelection(uri: Uri) {
        val viewModel = (this as ComponentActivity).let {
            androidx.lifecycle.ViewModelProvider(it)[InferenceViewModel::class.java]
        }
        viewModel.setModelFolder(uri)
    }
}

@androidx.compose.runtime.Composable
private fun FolderSetupScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberpunkBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SELECT MODEL FOLDER",
                color = MatrixGreen,
                style = CyberpunkTypography.terminalLarge
            )
            Text(
                text = "Awaiting folder selection...",
                color = MatrixGreen.copy(alpha = 0.7f),
                style = CyberpunkTypography.terminal,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

package com.ginference.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ginference.inference.ModelInfo
import com.ginference.ui.theme.*

data class ModelState(
    val model: ModelInfo,
    val isDownloaded: Boolean,
    val isDownloading: Boolean,
    val downloadProgress: Float,
    val isSelected: Boolean
)

@Composable
fun ModelSelectorDialog(
    models: List<ModelState>,
    onModelSelect: (ModelInfo) -> Unit,
    onModelDownload: (ModelInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .background(CyberpunkBackground)
                .border(2.dp, MatrixGreen)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODEL SELECTOR",
                        color = MatrixGreen,
                        style = CyberpunkTypography.terminalLarge
                    )
                    Text(
                        text = "[ESC]",
                        color = CyanNeon,
                        style = CyberpunkTypography.terminalSmall,
                        modifier = Modifier.clickableNoRipple { onDismiss() }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MatrixGreen.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(models) { modelState ->
                        ModelItem(
                            modelState = modelState,
                            onSelect = { onModelSelect(modelState.model) },
                            onDownload = { onModelDownload(modelState.model) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelItem(
    modelState: ModelState,
    onSelect: () -> Unit,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (modelState.isSelected) CyanNeon else MatrixGreen.copy(alpha = 0.5f)
            )
            .background(
                if (modelState.isSelected) {
                    CyanNeon.copy(alpha = 0.1f)
                } else {
                    CyberpunkBackground
                }
            )
            .clickableNoRipple {
                if (modelState.isDownloaded) {
                    onSelect()
                } else {
                    onDownload()
                }
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modelState.model.name,
                    color = if (modelState.isSelected) CyanNeon else MatrixGreen,
                    style = CyberpunkTypography.terminal
                )
                Text(
                    text = formatSize(modelState.model.size),
                    color = MatrixGreen.copy(alpha = 0.7f),
                    style = CyberpunkTypography.terminalSmall
                )
            }

            StatusIndicator(modelState)
        }

        if (modelState.isDownloading) {
            Spacer(modifier = Modifier.height(8.dp))
            DownloadProgressBar(modelState.downloadProgress)
        }
    }
}

@Composable
private fun StatusIndicator(modelState: ModelState) {
    when {
        modelState.isDownloading -> {
            Text(
                text = "DOWNLOADING",
                color = HotPink,
                style = CyberpunkTypography.terminalSmall
            )
        }
        modelState.isDownloaded && modelState.isSelected -> {
            Text(
                text = "✓ LOADED",
                color = CyanNeon,
                style = CyberpunkTypography.terminal
            )
        }
        modelState.isDownloaded -> {
            Text(
                text = "✓ READY",
                color = MatrixGreen,
                style = CyberpunkTypography.terminal
            )
        }
        else -> {
            Text(
                text = "✗ DOWNLOAD",
                color = MatrixGreen.copy(alpha = 0.7f),
                style = CyberpunkTypography.terminal
            )
        }
    }
}

@Composable
private fun DownloadProgressBar(progress: Float) {
    Column {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = HotPink,
            trackColor = MatrixGreen.copy(alpha = 0.2f)
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            color = HotPink,
            style = CyberpunkTypography.metric,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onClick()
        }
    )
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        else -> String.format("%.2fGB", bytes / (1024f * 1024f * 1024f))
    }
}

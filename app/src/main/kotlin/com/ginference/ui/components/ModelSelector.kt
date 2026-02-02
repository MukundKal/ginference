package com.ginference.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ginference.inference.ModelInfo
import com.ginference.inference.ModelType
import com.ginference.ui.theme.*

@Composable
fun ModelSelectorDialog(
    models: List<ModelInfo>,
    selectedModelId: String?,
    onModelSelect: (ModelInfo) -> Unit,
    onDismiss: () -> Unit,
    storagePath: String,
    cacheSize: String,
    freeSpace: String
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

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MatrixGreen.copy(alpha = 0.05f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "STORAGE: $storagePath",
                        color = CyanNeon.copy(alpha = 0.7f),
                        style = CyberpunkTypography.metric
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CACHE: $cacheSize",
                            color = MatrixGreen.copy(alpha = 0.8f),
                            style = CyberpunkTypography.metric
                        )
                        Text(
                            text = "FREE: $freeSpace",
                            color = MatrixGreen.copy(alpha = 0.8f),
                            style = CyberpunkTypography.metric
                        )
                    }
                    Text(
                        text = "LLM: .task/.litertlm | WHISPER: .bin (ggml-*)",
                        color = HotPink.copy(alpha = 0.7f),
                        style = CyberpunkTypography.metric,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (models.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO MODELS FOUND",
                                color = NeonYellow,
                                style = CyberpunkTypography.terminal
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Place models in storage path above",
                                color = MatrixGreen.copy(alpha = 0.7f),
                                style = CyberpunkTypography.terminalSmall
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(models) { model ->
                            ModelItem(
                                model = model,
                                isSelected = model.id == selectedModelId,
                                onSelect = { onModelSelect(model) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelItem(
    model: ModelInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isWhisper = model.type == ModelType.WHISPER
    val accentColor = if (isWhisper) HotPink else CyanNeon

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else MatrixGreen.copy(alpha = 0.5f)
            )
            .background(
                if (isSelected) {
                    accentColor.copy(alpha = 0.1f)
                } else {
                    CyberpunkBackground
                }
            )
            .clickableNoRipple { onSelect() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Model type badge
                    ModelTypeBadge(type = model.type)

                    Text(
                        text = model.name,
                        color = if (isSelected) accentColor else MatrixGreen,
                        style = CyberpunkTypography.terminal
                    )
                }
                Text(
                    text = model.fileName,
                    color = MatrixGreen.copy(alpha = 0.5f),
                    style = CyberpunkTypography.metric
                )
            }

            Text(
                text = if (isSelected) "✓ LOADED" else formatSize(model.size),
                color = if (isSelected) accentColor else MatrixGreen.copy(alpha = 0.7f),
                style = CyberpunkTypography.terminal
            )
        }
    }
}

@Composable
private fun ModelTypeBadge(type: ModelType) {
    val (text, color) = when (type) {
        ModelType.LLM -> "LLM" to CyanNeon
        ModelType.WHISPER -> "ASR" to HotPink
    }

    Box(
        modifier = Modifier
            .border(1.dp, color)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = CyberpunkTypography.metric
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

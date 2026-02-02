package com.ginference.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ginference.inference.ModelInfo
import com.ginference.ui.components.ModelSelectorDialog
import com.ginference.ui.theme.*
import kotlinx.coroutines.delay

data class Message(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun InferenceScreen(
    messages: List<Message>,
    currentOutput: String,
    isGenerating: Boolean,
    isModelLoaded: Boolean,
    modelName: String,
    onSendPrompt: (String) -> Unit,
    onStopGeneration: () -> Unit,
    ttft: Long,
    tokensPerSec: Float,
    ramUsage: String,
    vramUsage: String,
    cpuUsage: Float,
    gpuUsage: Float,
    temperature: Float,
    showModelSelector: Boolean,
    availableModels: List<ModelInfo>,
    onShowModelSelector: () -> Unit,
    onHideModelSelector: () -> Unit,
    onModelSelect: (ModelInfo) -> Unit,
    storagePath: String,
    cacheSize: String,
    freeSpace: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberpunkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Header(
                isGenerating = isGenerating,
                modelName = modelName,
                isModelLoaded = isModelLoaded,
                onShowModelSelector = onShowModelSelector,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            MetricsBar(
                ttft = ttft,
                tokensPerSec = tokensPerSec,
                ramUsage = ramUsage,
                vramUsage = vramUsage,
                cpuUsage = cpuUsage,
                gpuUsage = gpuUsage,
                temperature = temperature,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Divider()

            OutputArea(
                messages = messages,
                currentOutput = currentOutput,
                isGenerating = isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Divider()

            InputArea(
                onSendPrompt = onSendPrompt,
                onStopGeneration = onStopGeneration,
                isGenerating = isGenerating,
                isModelLoaded = isModelLoaded,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }

        if (showModelSelector) {
            ModelSelectorDialog(
                models = availableModels,
                selectedModelId = null,
                onModelSelect = onModelSelect,
                onDismiss = onHideModelSelector,
                storagePath = storagePath,
                cacheSize = cacheSize,
                freeSpace = freeSpace
            )
        }
    }
}

@Composable
private fun Header(
    isGenerating: Boolean,
    modelName: String,
    isModelLoaded: Boolean,
    onShowModelSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "GINFERENCE",
                color = MatrixGreen,
                style = CyberpunkTypography.terminalLarge
            )
            if (isModelLoaded) {
                Text(
                    text = modelName,
                    color = CyanNeon.copy(alpha = 0.7f),
                    style = CyberpunkTypography.metric
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[MODEL]",
                color = CyanNeon,
                style = CyberpunkTypography.terminalSmall,
                modifier = Modifier.clickableNoRipple { onShowModelSelector() }
            )
            LoadingSpinner(isGenerating = isGenerating)
        }
    }
}

@Composable
private fun LoadingSpinner(isGenerating: Boolean) {
    val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    var frameIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            while (true) {
                delay(80)
                frameIndex = (frameIndex + 1) % spinnerFrames.size
            }
        }
    }

    Text(
        text = if (isGenerating) spinnerFrames[frameIndex] else "READY",
        color = if (isGenerating) HotPink else CyanNeon,
        style = CyberpunkTypography.terminalSmall
    )
}

@Composable
private fun MetricsBar(
    ttft: Long,
    tokensPerSec: Float,
    ramUsage: String,
    vramUsage: String,
    cpuUsage: Float,
    gpuUsage: Float,
    temperature: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MetricItem("TTFT", if (ttft > 0) "${ttft}ms" else "--")
        MetricItem("TOK/S", if (tokensPerSec > 0) String.format("%.1f", tokensPerSec) else "--")
        MetricItem("RAM", ramUsage)
        MetricItem("GPU", if (gpuUsage > 0) "${String.format("%.0f", gpuUsage)}%" else "--")
        MetricItem("TEMP", if (temperature > 0) "${String.format("%.0f", temperature)}°" else "--")
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = CyanNeon,
            style = CyberpunkTypography.metric
        )
        Text(
            text = value,
            color = MatrixGreen,
            style = CyberpunkTypography.metric
        )
    }
}

@Composable
private fun OutputArea(
    messages: List<Message>,
    currentOutput: String,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, currentOutput) {
        if (messages.isNotEmpty() || currentOutput.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Box(modifier = modifier) {
        if (messages.isEmpty() && currentOutput.isEmpty()) {
            Text(
                text = "> AWAITING INPUT_",
                color = MatrixGreen.copy(alpha = 0.5f),
                style = CyberpunkTypography.terminal,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }

                if (currentOutput.isNotEmpty()) {
                    item {
                        GeneratingMessage(currentOutput, isGenerating)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (message.isUser) "> USER" else "> ASSISTANT",
            color = if (message.isUser) HotPink else CyanNeon,
            style = CyberpunkTypography.terminalSmall
        )
        Text(
            text = message.content,
            color = if (message.isUser) MatrixGreen else MatrixGreen,
            style = CyberpunkTypography.terminal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun GeneratingMessage(content: String, isGenerating: Boolean) {
    val cursorFrames = listOf("█", "▓", "▒", "░", " ")
    var cursorIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            while (true) {
                delay(200)
                cursorIndex = (cursorIndex + 1) % cursorFrames.size
            }
        }
    }

    MessageBubble(
        Message(
            content = content + if (isGenerating) cursorFrames[cursorIndex] else "",
            isUser = false
        )
    )
}

@Composable
private fun InputArea(
    onSendPrompt: (String) -> Unit,
    onStopGeneration: () -> Unit,
    isGenerating: Boolean,
    isModelLoaded: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = inputText,
            onValueChange = { inputText = it },
            enabled = !isGenerating && isModelLoaded,
            textStyle = CyberpunkTypography.terminal.copy(
                color = if (isModelLoaded) MatrixGreen else MatrixGreen.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .weight(1f)
                .background(CyberpunkBackground)
                .border(
                    1.dp,
                    when {
                        !isModelLoaded -> NeonRed.copy(alpha = 0.5f)
                        isGenerating -> MatrixGreen.copy(alpha = 0.3f)
                        else -> MatrixGreen
                    }
                )
                .padding(12.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (inputText.isEmpty()) {
                        Text(
                            text = if (!isModelLoaded) "LOAD MODEL FIRST_" else "ENTER PROMPT_",
                            color = if (!isModelLoaded) NeonRed.copy(alpha = 0.7f) else MatrixGreen.copy(alpha = 0.5f),
                            style = CyberpunkTypography.terminal
                        )
                    }
                    innerTextField()
                }
            }
        )

        ActionButton(
            text = if (isGenerating) "ABORT" else "EXECUTE",
            enabled = if (isGenerating) true else (inputText.isNotBlank() && isModelLoaded),
            onClick = {
                if (isGenerating) {
                    onStopGeneration()
                } else {
                    keyboardController?.hide()
                    onSendPrompt(inputText)
                    inputText = ""
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (enabled) {
                    if (text == "ABORT") NeonRed else CyanNeon
                } else {
                    MatrixGreen.copy(alpha = 0.3f)
                }
            )
            .background(CyberpunkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(
                if (enabled) {
                    Modifier.clickableNoRipple { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) {
                if (text == "ABORT") NeonRed else CyanNeon
            } else {
                MatrixGreen.copy(alpha = 0.3f)
            },
            style = CyberpunkTypography.terminal
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MatrixGreen.copy(alpha = 0.3f))
    )
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

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ginference.inference.ModelInfo
import com.ginference.inference.ModelType
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
    isLoadingModel: Boolean,
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
    // Whisper transcription props
    isWhisperLoaded: Boolean = false,
    isLoadingWhisper: Boolean = false,
    whisperModelName: String = "",
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    transcriptionText: String = "",
    recordingDuration: String = "0.0s",
    recordingAmplitude: Float = 0f,
    hasRecordPermission: Boolean = false,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onUseTranscription: () -> Unit = {},
    onClearTranscription: () -> Unit = {},
    onRequestRecordPermission: () -> Unit = {},
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
                isLoadingModel = isLoadingModel,
                isWhisperLoaded = isWhisperLoaded,
                isLoadingWhisper = isLoadingWhisper,
                whisperModelName = whisperModelName,
                isRecording = isRecording,
                isTranscribing = isTranscribing,
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
                transcriptionText = transcriptionText,
                isTranscribing = isTranscribing,
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
                isWhisperLoaded = isWhisperLoaded,
                isRecording = isRecording,
                isTranscribing = isTranscribing,
                recordingDuration = recordingDuration,
                recordingAmplitude = recordingAmplitude,
                transcriptionText = transcriptionText,
                hasRecordPermission = hasRecordPermission,
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onUseTranscription = onUseTranscription,
                onClearTranscription = onClearTranscription,
                onRequestRecordPermission = onRequestRecordPermission,
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
    isLoadingModel: Boolean,
    isWhisperLoaded: Boolean,
    isLoadingWhisper: Boolean,
    whisperModelName: String,
    isRecording: Boolean,
    isTranscribing: Boolean,
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
            // LLM model status
            if (isLoadingModel) {
                Text(
                    text = "LOADING LLM...",
                    color = NeonYellow,
                    style = CyberpunkTypography.metric
                )
            } else if (isModelLoaded) {
                Text(
                    text = "LLM: $modelName",
                    color = CyanNeon.copy(alpha = 0.7f),
                    style = CyberpunkTypography.metric
                )
            }
            // Whisper model status
            if (isLoadingWhisper) {
                Text(
                    text = "LOADING WHISPER...",
                    color = NeonYellow,
                    style = CyberpunkTypography.metric
                )
            } else if (isWhisperLoaded) {
                Text(
                    text = "WHISPER: $whisperModelName",
                    color = HotPink.copy(alpha = 0.7f),
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
            LoadingSpinner(
                isGenerating = isGenerating || isLoadingModel,
                isRecording = isRecording,
                isTranscribing = isTranscribing || isLoadingWhisper
            )
        }
    }
}

@Composable
private fun LoadingSpinner(
    isGenerating: Boolean,
    isRecording: Boolean = false,
    isTranscribing: Boolean = false
) {
    val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")
    val recordingFrames = listOf("●", "○", "●", "○")
    var frameIndex by remember { mutableStateOf(0) }

    val isActive = isGenerating || isRecording || isTranscribing

    LaunchedEffect(isActive, isRecording) {
        if (isActive) {
            while (true) {
                delay(if (isRecording) 300 else 80)
                frameIndex = (frameIndex + 1) % if (isRecording) recordingFrames.size else spinnerFrames.size
            }
        }
    }

    val (text, color) = when {
        isRecording -> recordingFrames[frameIndex % recordingFrames.size] to NeonRed
        isTranscribing -> spinnerFrames[frameIndex] to HotPink
        isGenerating -> spinnerFrames[frameIndex] to HotPink
        else -> "READY" to CyanNeon
    }

    Text(
        text = text,
        color = color,
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
    transcriptionText: String = "",
    isTranscribing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, currentOutput, transcriptionText) {
        if (messages.isNotEmpty() || currentOutput.isNotEmpty() || transcriptionText.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Box(modifier = modifier) {
        if (messages.isEmpty() && currentOutput.isEmpty() && transcriptionText.isEmpty()) {
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

                if (transcriptionText.isNotEmpty() || isTranscribing) {
                    item {
                        TranscriptionDisplay(
                            text = transcriptionText,
                            isTranscribing = isTranscribing
                        )
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
private fun TranscriptionDisplay(
    text: String,
    isTranscribing: Boolean
) {
    val cursorFrames = listOf("█", "▓", "▒", "░", " ")
    var cursorIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isTranscribing) {
        if (isTranscribing) {
            while (true) {
                delay(200)
                cursorIndex = (cursorIndex + 1) % cursorFrames.size
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "> TRANSCRIPTION",
            color = HotPink,
            style = CyberpunkTypography.terminalSmall
        )
        Text(
            text = if (isTranscribing && text.isEmpty()) {
                "PROCESSING AUDIO${cursorFrames[cursorIndex]}"
            } else {
                text + if (isTranscribing) cursorFrames[cursorIndex] else ""
            },
            color = MatrixGreen,
            style = CyberpunkTypography.terminal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun InputArea(
    onSendPrompt: (String) -> Unit,
    onStopGeneration: () -> Unit,
    isGenerating: Boolean,
    isModelLoaded: Boolean,
    isWhisperLoaded: Boolean = false,
    isRecording: Boolean = false,
    isTranscribing: Boolean = false,
    recordingDuration: String = "0.0s",
    recordingAmplitude: Float = 0f,
    transcriptionText: String = "",
    hasRecordPermission: Boolean = false,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onUseTranscription: () -> Unit = {},
    onClearTranscription: () -> Unit = {},
    onRequestRecordPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = modifier) {
        // Recording indicator
        if (isRecording) {
            RecordingIndicator(
                duration = recordingDuration,
                amplitude = recordingAmplitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Transcription action buttons
        if (transcriptionText.isNotBlank() && !isTranscribing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "USE AS PROMPT",
                    enabled = isModelLoaded,
                    onClick = onUseTranscription,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    text = "CLEAR",
                    enabled = true,
                    onClick = onClearTranscription
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mic button (for Whisper)
            if (isWhisperLoaded || !isModelLoaded) {
                MicButton(
                    isRecording = isRecording,
                    isTranscribing = isTranscribing,
                    isWhisperLoaded = isWhisperLoaded,
                    hasPermission = hasRecordPermission,
                    onClick = {
                        when {
                            !isWhisperLoaded -> { /* Show hint via error */ }
                            !hasRecordPermission -> onRequestRecordPermission()
                            isRecording -> onStopRecording()
                            else -> onStartRecording()
                        }
                    }
                )
            }

            androidx.compose.foundation.text.BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = !isGenerating && !isRecording && isModelLoaded,
                textStyle = CyberpunkTypography.terminal.copy(
                    color = if (isModelLoaded) MatrixGreen else MatrixGreen.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .background(CyberpunkBackground)
                    .border(
                        1.dp,
                        when {
                            isRecording -> HotPink
                            !isModelLoaded && !isWhisperLoaded -> NeonRed.copy(alpha = 0.5f)
                            isGenerating -> MatrixGreen.copy(alpha = 0.3f)
                            else -> MatrixGreen
                        }
                    )
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (inputText.isEmpty()) {
                            val hintText = when {
                                isRecording -> "RECORDING..."
                                !isModelLoaded && !isWhisperLoaded -> "LOAD MODEL FIRST_"
                                !isModelLoaded && isWhisperLoaded -> "TAP MIC OR LOAD LLM_"
                                else -> "ENTER PROMPT_"
                            }
                            val hintColor = when {
                                isRecording -> HotPink
                                !isModelLoaded && !isWhisperLoaded -> NeonRed.copy(alpha = 0.7f)
                                else -> MatrixGreen.copy(alpha = 0.5f)
                            }
                            Text(
                                text = hintText,
                                color = hintColor,
                                style = CyberpunkTypography.terminal
                            )
                        }
                        innerTextField()
                    }
                }
            )

            ActionButton(
                text = when {
                    isGenerating -> "ABORT"
                    isRecording -> "STOP"
                    else -> "EXECUTE"
                },
                enabled = when {
                    isGenerating -> true
                    isRecording -> true
                    else -> inputText.isNotBlank() && isModelLoaded
                },
                onClick = {
                    when {
                        isGenerating -> onStopGeneration()
                        isRecording -> onStopRecording()
                        else -> {
                            keyboardController?.hide()
                            onSendPrompt(inputText)
                            inputText = ""
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MicButton(
    isRecording: Boolean,
    isTranscribing: Boolean,
    isWhisperLoaded: Boolean,
    hasPermission: Boolean,
    onClick: () -> Unit
) {
    val pulseAlpha by remember { mutableStateOf(1f) }
    var pulseFrame by remember { mutableStateOf(0) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(500)
                pulseFrame = (pulseFrame + 1) % 2
            }
        }
    }

    val color = when {
        !isWhisperLoaded -> MatrixGreen.copy(alpha = 0.3f)
        isTranscribing -> HotPink.copy(alpha = 0.5f)
        isRecording -> if (pulseFrame == 0) NeonRed else NeonRed.copy(alpha = 0.5f)
        !hasPermission -> NeonYellow
        else -> HotPink
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, color)
            .background(CyberpunkBackground)
            .then(
                if (isWhisperLoaded && !isTranscribing) {
                    Modifier.clickableNoRipple { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when {
                isTranscribing -> "◌"
                isRecording -> "●"
                else -> "◉"
            },
            color = color,
            style = CyberpunkTypography.terminalLarge
        )
    }
}

@Composable
private fun RecordingIndicator(
    duration: String,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(1.dp, NeonRed.copy(alpha = 0.5f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "● REC",
            color = NeonRed,
            style = CyberpunkTypography.terminalSmall
        )

        // Simple amplitude bar
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        ) {
            val bars = 20
            val activeBars = (amplitude * bars).toInt()
            repeat(bars) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(
                            if (index < activeBars) {
                                when {
                                    index > bars * 0.8 -> NeonRed
                                    index > bars * 0.6 -> NeonYellow
                                    else -> MatrixGreen
                                }
                            } else {
                                MatrixGreen.copy(alpha = 0.2f)
                            }
                        )
                )
            }
        }

        Text(
            text = duration,
            color = NeonRed,
            style = CyberpunkTypography.terminalSmall
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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

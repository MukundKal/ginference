package com.ginference.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ginference.inference.LLMEngine
import com.ginference.inference.ModelInfo
import com.ginference.inference.ModelManager
import com.ginference.metrics.InferenceMetrics
import com.ginference.metrics.SystemMetrics
import com.ginference.ui.components.ModelState
import com.ginference.ui.screens.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class InferenceState(
    val messages: List<Message> = emptyList(),
    val currentOutput: String = "",
    val isGenerating: Boolean = false,
    val isModelLoaded: Boolean = false,
    val modelName: String = "",
    val error: String? = null,
    val ttft: Long = 0L,
    val tokensPerSec: Float = 0f,
    val ramUsage: String = "--",
    val vramUsage: String = "--",
    val cpuUsage: Float = 0f,
    val gpuUsage: Float = 0f,
    val temperature: Float = 0f,
    val showModelSelector: Boolean = false,
    val availableModels: List<ModelState> = emptyList(),
    val currentModelId: String? = null
)

class InferenceViewModel(application: Application) : AndroidViewModel(application) {

    private val llmEngine = LLMEngine(application)
    private val modelManager = ModelManager(application)
    private val systemMetrics = SystemMetrics(application)
    private val inferenceMetrics = InferenceMetrics()
    private val okHttpClient = OkHttpClient()

    private val _state = MutableStateFlow(InferenceState())
    val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var generationJob: Job? = null
    private var metricsJob: Job? = null

    init {
        Log.d(TAG, "InferenceViewModel initialized")
        startMetricsCollection()
        loadAvailableModels()
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            val models = modelManager.getAllModels()
            val modelStates = models.map { model ->
                ModelState(
                    model = model,
                    isDownloaded = modelManager.isModelDownloaded(model),
                    isDownloading = false,
                    downloadProgress = 0f,
                    isSelected = false
                )
            }
            _state.value = _state.value.copy(availableModels = modelStates)
        }
    }

    fun showModelSelector() {
        _state.value = _state.value.copy(showModelSelector = true)
    }

    fun hideModelSelector() {
        _state.value = _state.value.copy(showModelSelector = false)
    }

    fun selectModel(model: ModelInfo) {
        if (!modelManager.isModelDownloaded(model)) {
            Log.w(TAG, "Model not downloaded: ${model.name}")
            return
        }

        hideModelSelector()
        loadModel(modelManager.getModelPath(model), model.name)
        _state.value = _state.value.copy(currentModelId = model.id)
        updateModelStates()
    }

    fun downloadModel(model: ModelInfo) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting download: ${model.name}")
                updateModelDownloadState(model.id, isDownloading = true, progress = 0f)

                val outputFile = modelManager.getModelFile(model)
                val request = Request.Builder().url(model.url).build()

                withContext(Dispatchers.IO) {
                    val response = okHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("Download failed: ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response body")
                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(outputFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        if (contentLength > 0) {
                            val progress = totalBytes.toFloat() / contentLength.toFloat()
                            updateModelDownloadState(model.id, isDownloading = true, progress = progress)
                        }
                    }

                    outputStream.close()
                    inputStream.close()
                }

                Log.d(TAG, "Download complete: ${model.name}")
                updateModelDownloadState(model.id, isDownloading = false, progress = 1f)
                loadAvailableModels()

            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${model.name}", e)
                updateModelDownloadState(model.id, isDownloading = false, progress = 0f)
                _state.value = _state.value.copy(error = "Download failed: ${e.message}")
            }
        }
    }

    private fun updateModelDownloadState(modelId: String, isDownloading: Boolean, progress: Float) {
        val updatedModels = _state.value.availableModels.map { modelState ->
            if (modelState.model.id == modelId) {
                modelState.copy(isDownloading = isDownloading, downloadProgress = progress)
            } else {
                modelState
            }
        }
        _state.value = _state.value.copy(availableModels = updatedModels)
    }

    private fun updateModelStates() {
        val currentId = _state.value.currentModelId
        val updatedModels = _state.value.availableModels.map { modelState ->
            modelState.copy(isSelected = modelState.model.id == currentId)
        }
        _state.value = _state.value.copy(availableModels = updatedModels)
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank() || _state.value.isGenerating) return

        Log.d(TAG, "Sending prompt: ${prompt.take(50)}...")

        val userMessage = Message(content = prompt, isUser = true)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMessage,
            currentOutput = "",
            isGenerating = true,
            error = null
        )

        generationJob = viewModelScope.launch {
            try {
                if (!llmEngine.isModelLoaded()) {
                    _state.value = _state.value.copy(
                        error = "No model loaded",
                        isGenerating = false
                    )
                    return@launch
                }

                inferenceMetrics.startSession(prompt.length)
                var fullResponse = ""

                llmEngine.generate(prompt).collect { (partialText, metrics) ->
                    if (fullResponse.isEmpty()) {
                        inferenceMetrics.recordFirstToken()
                    }
                    inferenceMetrics.recordToken()

                    fullResponse += partialText

                    _state.value = _state.value.copy(
                        currentOutput = fullResponse,
                        ttft = metrics.ttft,
                        tokensPerSec = metrics.tokensPerSecond
                    )
                }

                inferenceMetrics.endSession()

                val assistantMessage = Message(content = fullResponse, isUser = false)
                _state.value = _state.value.copy(
                    messages = _state.value.messages + assistantMessage,
                    currentOutput = "",
                    isGenerating = false
                )

                Log.d(TAG, "Generation complete: ${fullResponse.length} chars")
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                _state.value = _state.value.copy(
                    error = "Generation failed: ${e.message}",
                    isGenerating = false,
                    currentOutput = ""
                )
            }
        }
    }

    fun stopGeneration() {
        Log.d(TAG, "Stopping generation")
        generationJob?.cancel()
        generationJob = null

        if (_state.value.currentOutput.isNotEmpty()) {
            val assistantMessage = Message(
                content = _state.value.currentOutput + " [ABORTED]",
                isUser = false
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + assistantMessage,
                currentOutput = "",
                isGenerating = false
            )
        } else {
            _state.value = _state.value.copy(
                isGenerating = false,
                currentOutput = ""
            )
        }
    }

    private fun loadModel(modelPath: String, modelName: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading model: $modelName")
                _state.value = _state.value.copy(error = "Loading model...")

                val result = llmEngine.loadModel(modelPath)

                result.onSuccess {
                    _state.value = _state.value.copy(
                        isModelLoaded = true,
                        modelName = modelName,
                        error = null
                    )
                    Log.d(TAG, "Model loaded successfully")
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isModelLoaded = false,
                        error = "Failed to load model: ${e.message}"
                    )
                    Log.e(TAG, "Model load failed", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model load error", e)
                _state.value = _state.value.copy(
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(
            messages = emptyList(),
            currentOutput = "",
            error = null
        )
        inferenceMetrics.reset()
        Log.d(TAG, "Messages cleared")
    }

    private fun startMetricsCollection() {
        metricsJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val ramMetrics = systemMetrics.getRAMMetrics()
                    val vramMetrics = systemMetrics.getVRAMMetrics()
                    val cpuMetrics = systemMetrics.getCPUMetrics()
                    val gpuMetrics = systemMetrics.getGPUMetrics()
                    val thermalMetrics = systemMetrics.getThermalMetrics()

                    _state.value = _state.value.copy(
                        ramUsage = systemMetrics.formatBytes(ramMetrics.appRAM),
                        vramUsage = systemMetrics.formatBytes(vramMetrics.estimatedAppVRAM),
                        cpuUsage = cpuMetrics.usagePercent,
                        gpuUsage = gpuMetrics.usagePercent,
                        temperature = thermalMetrics.currentTemperature
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Metrics collection error", e)
                }

                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        metricsJob?.cancel()
        llmEngine.unload()
        Log.d(TAG, "ViewModel cleared")
    }

    companion object {
        private const val TAG = "InferenceViewModel"
    }
}

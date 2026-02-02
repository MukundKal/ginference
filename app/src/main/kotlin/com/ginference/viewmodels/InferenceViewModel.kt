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
import com.ginference.ui.screens.Message
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
    val availableModels: List<ModelInfo> = emptyList(),
    val currentModelId: String? = null,
    val storagePath: String = "",
    val cacheSize: String = "0B",
    val freeSpace: String = "0GB",
    val isFolderSet: Boolean = false
)

class InferenceViewModel(application: Application) : AndroidViewModel(application) {

    private val llmEngine = LLMEngine(application)
    private val modelManager = ModelManager(application)
    private val systemMetrics = SystemMetrics(application)
    private val inferenceMetrics = InferenceMetrics()

    private val _state = MutableStateFlow(InferenceState())
    val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var generationJob: Job? = null
    private var metricsJob: Job? = null

    init {
        Log.d(TAG, "InferenceViewModel initialized")
        startMetricsCollection()
        checkFolderSetup()
    }

    private fun checkFolderSetup() {
        val isFolderSet = modelManager.hasModelFolder()
        _state.value = _state.value.copy(isFolderSet = isFolderSet)
        if (isFolderSet) {
            loadAvailableModels()
        }
    }

    fun hasModelFolder(): Boolean {
        return modelManager.hasModelFolder()
    }

    fun getModelFolderPath(): String {
        return modelManager.getModelFolderPath()
    }

    fun setModelFolder(uri: android.net.Uri) {
        modelManager.saveModelFolderUri(uri)
        _state.value = _state.value.copy(isFolderSet = true)
        loadAvailableModels()
        Log.d(TAG, "Model folder set: $uri")
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            val models = modelManager.scanModels()
            val cacheSize = modelManager.getTotalCacheSize()
            val freeSpace = modelManager.getAvailableSpace()
            val storagePath = modelManager.getModelFolderPath()

            Log.d(TAG, "Found ${models.size} models in $storagePath")

            _state.value = _state.value.copy(
                availableModels = models,
                storagePath = storagePath,
                cacheSize = modelManager.formatSize(cacheSize),
                freeSpace = modelManager.formatSize(freeSpace)
            )
        }
    }

    fun showModelSelector() {
        _state.value = _state.value.copy(showModelSelector = true)
    }

    fun hideModelSelector() {
        _state.value = _state.value.copy(showModelSelector = false)
    }

    fun selectModel(model: ModelInfo) {
        hideModelSelector()
        loadModel(model.uri.toString(), model.name)
        _state.value = _state.value.copy(currentModelId = model.id)
    }

    fun refreshModels() {
        loadAvailableModels()
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

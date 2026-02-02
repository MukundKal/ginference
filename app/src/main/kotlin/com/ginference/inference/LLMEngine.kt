package com.ginference.inference

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class LLMEngine(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var isLoaded = false

    data class GenerationMetrics(
        val ttft: Long = 0L,
        val tokensGenerated: Int = 0,
        val totalTime: Long = 0L,
        val tokensPerSecond: Float = 0f
    )

    suspend fun loadModel(
        modelPath: String,
        maxTokens: Int = 512,
        temperature: Float = 0.8f,
        topK: Int = 40,
        randomSeed: Int = 0
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                throw IllegalArgumentException("Model file not found: $modelPath")
            }

            Log.d(TAG, "Loading model from: $modelPath")
            Log.d(TAG, "File size: ${modelFile.length() / 1024 / 1024} MB")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .setTemperature(temperature)
                .setTopK(topK)
                .setRandomSeed(randomSeed)
                .build()

            llmInference?.close()
            llmInference = LlmInference.createFromOptions(context, options)
            isLoaded = true

            Log.d(TAG, "Model loaded successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            isLoaded = false
            Result.failure(e)
        }
    }

    fun generate(prompt: String): Flow<Pair<String, GenerationMetrics>> = callbackFlow {
        if (!isLoaded || llmInference == null) {
            close(IllegalStateException("Model not loaded"))
            return@callbackFlow
        }

        val startTime = System.currentTimeMillis()
        var firstTokenTime = 0L
        var tokenCount = 0
        val fullResponse = StringBuilder()

        Log.d(TAG, "Generating response for prompt: ${prompt.take(50)}...")

        try {
            val inference = llmInference ?: throw IllegalStateException("Model unloaded during generation")

            inference.generateResponseAsync(prompt)?.let { result ->
                val currentTime = System.currentTimeMillis()

                if (tokenCount == 0) {
                    firstTokenTime = currentTime - startTime
                }

                tokenCount++
                fullResponse.append(result as String)

                val totalTime = currentTime - startTime
                val tokensPerSecond = if (totalTime > 0) {
                    (tokenCount * 1000f) / totalTime
                } else 0f

                val metrics = GenerationMetrics(
                    ttft = firstTokenTime,
                    tokensGenerated = tokenCount,
                    totalTime = totalTime,
                    tokensPerSecond = tokensPerSecond
                )

                trySend(Pair(result, metrics))
            }

            Log.d(TAG, "Generation complete. Tokens: $tokenCount")
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Generation error", e)
            close(e)
        }

        awaitClose {
            Log.d(TAG, "Flow closed")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateSync(prompt: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isLoaded || llmInference == null) {
                throw IllegalStateException("Model not loaded")
            }
            Log.d(TAG, "Generating sync response for: ${prompt.take(50)}...")
            llmInference?.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            null
        }
    }

    fun unload() {
        try {
            llmInference?.close()
            llmInference = null
            isLoaded = false
            Log.d(TAG, "Model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload model", e)
        }
    }

    fun isModelLoaded(): Boolean = isLoaded

    companion object {
        private const val TAG = "LLMEngine"
    }
}

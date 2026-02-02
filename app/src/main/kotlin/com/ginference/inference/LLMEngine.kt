package com.ginference.inference

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

class LLMEngine(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var isLoaded = false
    private var modelFormat: ModelFormat = ModelFormat.UNKNOWN

    enum class ModelFormat {
        UNKNOWN,
        TFLITE,  // .task files (TFL3 format)
        LITERT   // .litertlm files (RTLM format)
    }

    data class GenerationMetrics(
        val ttft: Long = 0L,
        val tokensGenerated: Int = 0,
        val totalTime: Long = 0L,
        val tokensPerSecond: Float = 0f
    )

    private fun detectModelFormat(file: File): ModelFormat {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4)
                raf.read(header)
                val magic = String(header, Charsets.US_ASCII)
                Log.d(TAG, "Model magic header: $magic")
                when (magic) {
                    "TFL3" -> ModelFormat.TFLITE
                    "RTLM" -> ModelFormat.LITERT
                    else -> {
                        when {
                            file.name.endsWith(".task") -> ModelFormat.TFLITE
                            file.name.endsWith(".litertlm") -> ModelFormat.LITERT
                            else -> ModelFormat.UNKNOWN
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting model format", e)
            ModelFormat.UNKNOWN
        }
    }

    private suspend fun copyUriToCache(uriString: String): File = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)

        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "model.task"

        val cacheFile = File(context.cacheDir, fileName)

        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "Using cached model: ${cacheFile.absolutePath}")
            return@withContext cacheFile
        }

        Log.d(TAG, "Copying model from URI to cache: $fileName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cacheFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    if (totalBytes % (10 * 1024 * 1024) == 0L) {
                        Log.d(TAG, "Copied ${totalBytes / 1024 / 1024} MB...")
                    }
                }

                Log.d(TAG, "Copy complete: ${totalBytes / 1024 / 1024} MB")
            }
        } ?: throw IllegalStateException("Failed to open input stream from URI")

        return@withContext cacheFile
    }

    suspend fun loadModel(
        modelPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val actualPath: String = if (modelPath.startsWith("content://")) {
                Log.d(TAG, "Detected content URI, copying to cache...")
                val cachedFile = copyUriToCache(modelPath)
                cachedFile.absolutePath
            } else {
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    throw IllegalArgumentException("Model file not found: $modelPath")
                }
                modelPath
            }

            Log.d(TAG, "Loading model from: $actualPath")
            val modelFile = File(actualPath)
            val fileSizeMB = modelFile.length() / 1024 / 1024
            Log.d(TAG, "File size: $fileSizeMB MB")

            if (!actualPath.endsWith(".task") && !actualPath.endsWith(".litertlm")) {
                throw IllegalArgumentException("Invalid model format. Must be .task or .litertlm file")
            }

            modelFormat = detectModelFormat(modelFile)
            Log.d(TAG, "Detected model format: $modelFormat")

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val availableMB = memInfo.availMem / 1024 / 1024

            Log.d(TAG, "Available RAM: $availableMB MB, Model size: $fileSizeMB MB")

            if (fileSizeMB > availableMB * 0.7) {
                throw IllegalStateException("Insufficient memory. Model: ${fileSizeMB}MB, Available: ${availableMB}MB. Close other apps.")
            }

            if (modelFormat == ModelFormat.LITERT) {
                Log.w(TAG, "RTLM format detected. Attempting to load with MediaPipe (may fail)...")
                Log.w(TAG, "Note: RTLM format requires LiteRT GenAI SDK. If load fails, use .task files instead.")
            }

            Log.d(TAG, "Creating LlmInference with model...")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(actualPath)
                .setMaxTokens(1024)
                .build()

            llmInference?.close()

            try {
                llmInference = LlmInference.createFromOptions(context, options)
                isLoaded = true
                Log.d(TAG, "Model loaded successfully")
                Result.success(Unit)
            } catch (oom: OutOfMemoryError) {
                isLoaded = false
                llmInference = null
                Log.e(TAG, "Out of memory loading model", oom)
                Result.failure(Exception("Out of memory - model too large"))
            } catch (e: IllegalArgumentException) {
                isLoaded = false
                llmInference = null
                Log.e(TAG, "Invalid model format", e)
                Result.failure(Exception("Invalid model: ${e.message}"))
            } catch (e: UnsatisfiedLinkError) {
                isLoaded = false
                llmInference = null
                Log.e(TAG, "Native library error", e)
                Result.failure(Exception("Native error: ${e.message}"))
            } catch (e: RuntimeException) {
                isLoaded = false
                llmInference = null
                Log.e(TAG, "Runtime error loading model", e)

                val message = e.message ?: ""
                if (message.contains("RTLM") || message.contains("TFL3")) {
                    Result.failure(Exception(
                        "Model format not supported. The .litertlm (RTLM) format requires LiteRT GenAI SDK. " +
                        "Please use .task files instead, or wait for LiteRT GenAI SDK support."
                    ))
                } else {
                    Result.failure(Exception("Load failed: ${e.message ?: "Unknown error"}"))
                }
            } catch (e: Error) {
                isLoaded = false
                llmInference = null
                Log.e(TAG, "Fatal error loading model", e)
                Result.failure(Exception("Fatal error: ${e.message ?: "Crash in native code"}"))
            } catch (e: Throwable) {
                isLoaded = false
                llmInference = null
                Log.e(TAG, "Unexpected error loading model", e)
                Result.failure(Exception("Unexpected error: ${e.message ?: "Unknown"}"))
            }
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

            // Use streaming generation with result listener callback
            inference.generateResponseAsync(prompt) { partialResult, done ->
                val currentTime = System.currentTimeMillis()

                if (tokenCount == 0 && partialResult.isNotEmpty()) {
                    firstTokenTime = currentTime - startTime
                    Log.d(TAG, "TTFT: ${firstTokenTime}ms")
                }

                if (partialResult.isNotEmpty()) {
                    tokenCount++
                    fullResponse.append(partialResult)

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

                    Log.d(TAG, "Token $tokenCount: '${partialResult.take(20)}...' @ ${tokensPerSecond}t/s")
                    trySend(Pair(partialResult, metrics))
                }

                if (done) {
                    Log.d(TAG, "Generation complete. Total tokens: $tokenCount")
                    close()
                }
            }

            // Keep the flow alive until generation completes
            awaitClose {
                Log.d(TAG, "Flow closed, total response length: ${fullResponse.length}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Generation error", e)
            close(e)
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
            modelFormat = ModelFormat.UNKNOWN
            Log.d(TAG, "Model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload model", e)
        }
    }

    fun isModelLoaded(): Boolean = isLoaded

    fun getModelFormat(): ModelFormat = modelFormat

    companion object {
        private const val TAG = "LLMEngine"
    }
}

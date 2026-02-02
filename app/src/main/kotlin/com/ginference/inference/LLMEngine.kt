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

class LLMEngine(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var isLoaded = false

    data class GenerationMetrics(
        val ttft: Long = 0L,
        val tokensGenerated: Int = 0,
        val totalTime: Long = 0L,
        val tokensPerSecond: Float = 0f
    )

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

            // Validate file extension
            if (!actualPath.endsWith(".task") && !actualPath.endsWith(".litertlm")) {
                throw IllegalArgumentException("Invalid model format. Must be .task or .litertlm file")
            }

            // Check available memory
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val availableMB = memInfo.availMem / 1024 / 1024

            Log.d(TAG, "Available RAM: $availableMB MB, Model size: $fileSizeMB MB")

            if (fileSizeMB > availableMB * 0.7) {
                throw IllegalStateException("Insufficient memory. Model: ${fileSizeMB}MB, Available: ${availableMB}MB. Close other apps.")
            }

            Log.d(TAG, "Creating LlmInference with model metadata config...")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(actualPath)
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
                Result.failure(Exception("Load failed: ${e.message ?: "Unknown error"}"))
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

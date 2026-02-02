package com.ginference.inference

import android.content.Context
import android.net.Uri
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Whisper speech-to-text engine using official whisper.cpp Android library.
 *
 * Requires libwhisper.so to be placed in app/src/main/jniLibs/{abi}/
 * Built from: https://github.com/ggerganov/whisper.cpp
 *
 * Compatible models (.bin format):
 * - ggml-tiny.en.bin (75MB) - Fast, English only
 * - ggml-base.en.bin (142MB) - Better accuracy
 * - ggml-small.en.bin (466MB) - Good balance
 *
 * Download models from: https://huggingface.co/ggerganov/whisper.cpp
 */
class WhisperEngine(private val context: Context) {

    private var whisperContext: WhisperContext? = null
    private var isLoaded = false
    private var modelName: String = ""

    data class TranscriptionResult(
        val text: String,
        val durationMs: Long,
        val processingTimeMs: Long
    )

    companion object {
        private const val TAG = "WhisperEngine"

        fun isLibraryAvailable(): Boolean = WhisperContext.isAvailable()

        fun getLibraryError(): String? = WhisperContext.getLoadError()

        fun isWhisperModel(fileName: String): Boolean {
            val lowerName = fileName.lowercase()
            return lowerName.endsWith(".bin") &&
                   (lowerName.contains("whisper") ||
                    lowerName.contains("ggml-tiny") ||
                    lowerName.contains("ggml-base") ||
                    lowerName.contains("ggml-small") ||
                    lowerName.contains("ggml-medium") ||
                    lowerName.contains("ggml-large"))
        }

        fun getSystemInfo(): String? {
            return if (WhisperContext.isAvailable()) {
                try {
                    WhisperContext.getSystemInfo()
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }

    private suspend fun copyUriToCache(uriString: String): File = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)

        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "whisper_model.bin"

        val cacheDir = File(context.cacheDir, "whisper")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val cacheFile = File(cacheDir, fileName)

        if (cacheFile.exists() && cacheFile.length() > 0) {
            Log.d(TAG, "Using cached whisper model: ${cacheFile.absolutePath}")
            return@withContext cacheFile
        }

        Log.d(TAG, "Copying whisper model to cache: $fileName")

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

    suspend fun loadModel(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Check if native library is available
        if (!WhisperContext.isAvailable()) {
            val error = WhisperContext.getLoadError() ?: "Unknown error"
            return@withContext Result.failure(
                UnsatisfiedLinkError(
                    "Whisper native library not available.\n\n" +
                    "To enable Whisper transcription:\n" +
                    "1. Download libwhisper.so from whisper.cpp releases\n" +
                    "2. Place in app/src/main/jniLibs/arm64-v8a/\n" +
                    "3. Rebuild the app\n\n" +
                    "Error: $error"
                )
            )
        }

        return@withContext try {
            val actualPath: String = if (modelPath.startsWith("content://")) {
                Log.d(TAG, "Detected content URI, copying to cache...")
                val cachedFile = copyUriToCache(modelPath)
                cachedFile.absolutePath
            } else {
                val modelFile = File(modelPath)
                if (!modelFile.exists()) {
                    throw IllegalArgumentException("Whisper model file not found: $modelPath")
                }
                modelPath
            }

            Log.d(TAG, "Loading whisper model from: $actualPath")
            val modelFile = File(actualPath)
            val fileSizeMB = modelFile.length() / 1024 / 1024
            Log.d(TAG, "Whisper model size: $fileSizeMB MB")

            if (!actualPath.endsWith(".bin")) {
                throw IllegalArgumentException(
                    "Invalid whisper model format. Expected .bin file (ggml format).\n" +
                    "Download models from: https://huggingface.co/ggerganov/whisper.cpp"
                )
            }

            // Check available memory
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val availableMB = memInfo.availMem / 1024 / 1024

            Log.d(TAG, "Available RAM: $availableMB MB, Whisper model size: $fileSizeMB MB")

            if (fileSizeMB > availableMB * 0.5) {
                throw IllegalStateException(
                    "Insufficient memory for Whisper model.\n" +
                    "Model: ${fileSizeMB}MB, Available: ${availableMB}MB\n" +
                    "Try a smaller model (e.g., ggml-tiny.en.bin)"
                )
            }

            // Unload previous model if any
            unload()

            Log.d(TAG, "Creating whisper context...")
            whisperContext = WhisperContext.createContextFromFile(actualPath)

            isLoaded = true
            modelName = modelFile.nameWithoutExtension
            Log.d(TAG, "Whisper model loaded successfully: $modelName")
            Result.success(Unit)

        } catch (e: Exception) {
            isLoaded = false
            whisperContext = null
            Log.e(TAG, "Failed to load whisper model", e)
            Result.failure(e)
        }
    }

    suspend fun transcribe(audioData: ShortArray, sampleRate: Int = 16000): Result<TranscriptionResult> =
        withContext(Dispatchers.IO) {
            if (!WhisperContext.isAvailable()) {
                return@withContext Result.failure(
                    IllegalStateException("Whisper native library not loaded")
                )
            }

            if (!isLoaded || whisperContext == null) {
                return@withContext Result.failure(
                    IllegalStateException("Whisper model not loaded. Load a model first.")
                )
            }

            if (audioData.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio data is empty")
                )
            }

            return@withContext try {
                if (sampleRate != 16000) {
                    Log.w(TAG, "Audio is ${sampleRate}Hz but Whisper expects 16000Hz. Results may vary.")
                }

                val audioDurationSec = audioData.size.toFloat() / sampleRate
                Log.d(TAG, "Transcribing ${audioData.size} samples (${String.format("%.1f", audioDurationSec)}s of audio)")

                val startTime = System.currentTimeMillis()

                // Convert to float samples normalized to [-1, 1]
                val floatData = FloatArray(audioData.size) { i ->
                    audioData[i] / 32768.0f
                }

                // Transcribe using WhisperContext
                val text = whisperContext!!.transcribeData(floatData, printTimestamp = false)
                val processingTime = System.currentTimeMillis() - startTime

                val durationMs = (audioData.size * 1000L) / sampleRate
                val rtFactor = processingTime.toFloat() / durationMs

                Log.d(TAG, "Transcription complete in ${processingTime}ms (${String.format("%.2f", rtFactor)}x realtime)")
                Log.d(TAG, "Result: ${text.take(100)}${if (text.length > 100) "..." else ""}")

                Result.success(TranscriptionResult(
                    text = text.trim(),
                    durationMs = durationMs,
                    processingTimeMs = processingTime
                ))

            } catch (e: Exception) {
                Log.e(TAG, "Transcription error", e)
                Result.failure(e)
            }
        }

    fun unload() {
        try {
            whisperContext?.releaseSync()
            whisperContext = null
            isLoaded = false
            modelName = ""
            Log.d(TAG, "Whisper model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unload whisper model", e)
            whisperContext = null
            isLoaded = false
        }
    }

    fun isModelLoaded(): Boolean = isLoaded

    fun getModelName(): String = modelName
}

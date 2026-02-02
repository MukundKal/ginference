package com.ginference.inference

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

enum class ModelType {
    LLM,      // .task, .litertlm files - text generation
    WHISPER   // .bin files (ggml format) - speech-to-text
}

data class ModelInfo(
    val id: String,
    val name: String,
    val fileName: String,
    val size: Long,
    val uri: Uri,
    val type: ModelType = ModelType.LLM
)

class ModelManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ginference_prefs", Context.MODE_PRIVATE)

    fun saveModelFolderUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit().putString(KEY_MODEL_FOLDER_URI, uri.toString()).apply()
        Log.d(TAG, "Saved model folder URI: $uri")
    }

    fun getModelFolderUri(): Uri? {
        val uriString = prefs.getString(KEY_MODEL_FOLDER_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    fun hasModelFolder(): Boolean {
        return getModelFolderUri() != null
    }

    fun getModelFolderPath(): String {
        val uri = getModelFolderUri() ?: return "Not set"
        return uri.path?.substringAfter("primary:") ?: uri.toString()
    }

    fun scanModels(): List<ModelInfo> {
        val folderUri = getModelFolderUri() ?: return emptyList()

        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()

        if (!folder.exists() || !folder.isDirectory) {
            Log.w(TAG, "Model folder does not exist or is not a directory")
            return emptyList()
        }

        val modelFiles = folder.listFiles().filter { file ->
            file.isFile && isModelFile(file.name ?: "")
        }

        Log.d(TAG, "Scanned ${modelFiles.size} models in ${folder.uri}")

        return modelFiles.mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            val modelType = getModelType(name)
            ModelInfo(
                id = name.substringBeforeLast("."),
                name = name.substringBeforeLast(".")
                    .replace("-", " ")
                    .replace("_", " "),
                fileName = name,
                size = file.length(),
                uri = file.uri,
                type = modelType
            )
        }.sortedBy { it.name }
    }

    fun scanLLMModels(): List<ModelInfo> {
        return scanModels().filter { it.type == ModelType.LLM }
    }

    fun scanWhisperModels(): List<ModelInfo> {
        return scanModels().filter { it.type == ModelType.WHISPER }
    }

    private fun isModelFile(fileName: String): Boolean {
        val lowerName = fileName.lowercase()
        return lowerName.endsWith(".task") ||
               lowerName.endsWith(".litertlm") ||
               isWhisperModelFile(lowerName)
    }

    private fun isWhisperModelFile(fileName: String): Boolean {
        val lowerName = fileName.lowercase()
        return lowerName.endsWith(".bin") &&
               (lowerName.contains("whisper") ||
                lowerName.contains("ggml-tiny") ||
                lowerName.contains("ggml-base") ||
                lowerName.contains("ggml-small") ||
                lowerName.contains("ggml-medium") ||
                lowerName.contains("ggml-large"))
    }

    fun getModelType(fileName: String): ModelType {
        val lowerName = fileName.lowercase()
        return when {
            isWhisperModelFile(lowerName) -> ModelType.WHISPER
            lowerName.endsWith(".task") -> ModelType.LLM
            lowerName.endsWith(".litertlm") -> ModelType.LLM
            else -> ModelType.LLM
        }
    }

    fun getModelByUri(uri: Uri): ModelInfo? {
        return scanModels().find { it.uri == uri }
    }

    fun deleteModel(model: ModelInfo): Boolean {
        return try {
            val file = DocumentFile.fromSingleUri(context, model.uri)
            val deleted = file?.delete() ?: false
            Log.d(TAG, "Deleted model ${model.name}: $deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete model ${model.name}", e)
            false
        }
    }

    fun getTotalCacheSize(): Long {
        return try {
            scanModels().sumOf { it.size }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate cache size", e)
            0L
        }
    }

    fun getAvailableSpace(): Long {
        return try {
            val folderUri = getModelFolderUri() ?: return 0L
            val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return 0L
            context.contentResolver.openFileDescriptor(folder.uri, "r")?.use {
                android.os.StatFs("/data").availableBytes
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available space", e)
            0L
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> String.format("%.2fGB", bytes / (1024f * 1024f * 1024f))
        }
    }

    fun clearModelFolder() {
        prefs.edit().remove(KEY_MODEL_FOLDER_URI).apply()
        Log.d(TAG, "Model folder URI cleared")
    }

    companion object {
        private const val TAG = "ModelManager"
        private const val KEY_MODEL_FOLDER_URI = "model_folder_uri"
    }
}

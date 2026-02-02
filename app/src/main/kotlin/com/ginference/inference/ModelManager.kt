package com.ginference.inference

import android.content.Context
import android.util.Log
import java.io.File

sealed class ModelInfo(
    val id: String,
    val name: String,
    val size: Long,
    val fileName: String,
    val url: String
) {
    object Gemma3_1B : ModelInfo(
        id = "gemma_3_1b",
        name = "Gemma 3 1B",
        size = 1_073_741_824L,
        fileName = "gemma-3-1b-it-gpu-int4.bin",
        url = "https://huggingface.co/google/gemma-3-1b-it/resolve/main/gemma-3-1b-it-gpu-int4.bin"
    )

    object Gemma2_2B : ModelInfo(
        id = "gemma_2_2b",
        name = "Gemma 2 2B",
        size = 2_147_483_648L,
        fileName = "gemma-2-2b-it-gpu-int4.bin",
        url = "https://huggingface.co/google/gemma-2-2b-it/resolve/main/gemma-2-2b-it-gpu-int4.bin"
    )

    object Phi2 : ModelInfo(
        id = "phi_2",
        name = "Phi-2",
        size = 2_684_354_560L,
        fileName = "phi-2-cpu-int4.bin",
        url = "https://huggingface.co/microsoft/phi-2/resolve/main/phi-2-cpu-int4.bin"
    )

    object SmolLM_1_7B : ModelInfo(
        id = "smollm_1_7b",
        name = "SmolLM 1.7B",
        size = 1_717_986_918L,
        fileName = "smollm-1.7b-instruct-gpu-int4.bin",
        url = "https://huggingface.co/HuggingFaceTB/SmolLM-1.7B-Instruct/resolve/main/smollm-1.7b-instruct-gpu-int4.bin"
    )
}

class ModelManager(private val context: Context) {

    private val modelDir: File
        get() = File(context.cacheDir, "models").apply {
            if (!exists()) mkdirs()
        }

    fun getModelPath(model: ModelInfo): String {
        return File(modelDir, model.fileName).absolutePath
    }

    fun isModelDownloaded(model: ModelInfo): Boolean {
        val file = File(modelDir, model.fileName)
        return file.exists() && file.length() > 0
    }

    fun getModelFile(model: ModelInfo): File {
        return File(modelDir, model.fileName)
    }

    fun getDownloadedModels(): List<ModelInfo> {
        return getAllModels().filter { isModelDownloaded(it) }
    }

    fun getAllModels(): List<ModelInfo> {
        return listOf(
            ModelInfo.Gemma3_1B,
            ModelInfo.Gemma2_2B,
            ModelInfo.Phi2,
            ModelInfo.SmolLM_1_7B
        )
    }

    fun getModelById(id: String): ModelInfo? {
        return getAllModels().find { it.id == id }
    }

    fun deleteModel(model: ModelInfo): Boolean {
        return try {
            val file = getModelFile(model)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted model ${model.name}: $deleted")
                deleted
            } else {
                Log.w(TAG, "Model ${model.name} not found")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete model ${model.name}", e)
            false
        }
    }

    fun deleteAllModels(): Boolean {
        return try {
            getAllModels().forEach { deleteModel(it) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all models", e)
            false
        }
    }

    fun getTotalCacheSize(): Long {
        return try {
            modelDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate cache size", e)
            0L
        }
    }

    fun getAvailableSpace(): Long {
        return try {
            modelDir.usableSpace
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available space", e)
            0L
        }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
        }
    }

    companion object {
        private const val TAG = "ModelManager"
    }
}

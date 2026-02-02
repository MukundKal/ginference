package com.whispercpp.whisper

import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors

private const val LOG_TAG = "WhisperLib"

/**
 * WhisperContext - High-level wrapper for whisper.cpp transcription.
 *
 * This matches the official whisper.cpp Android library structure exactly
 * to ensure JNI compatibility with libwhisper.so
 */
class WhisperContext private constructor(private var ptr: Long) {
    // Meet Whisper C++ constraint: Don't access from more than one thread at a time.
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    suspend fun transcribeData(data: FloatArray, printTimestamp: Boolean = false): String = withContext(scope.coroutineContext) {
        require(ptr != 0L) { "WhisperContext has been released" }
        val numThreads = WhisperCpuConfig.preferredThreadCount
        Log.d(LOG_TAG, "Transcribing with $numThreads threads, ${data.size} samples")
        WhisperLib.fullTranscribe(ptr, numThreads, data)
        val textCount = WhisperLib.getTextSegmentCount(ptr)
        return@withContext buildString {
            for (i in 0 until textCount) {
                if (printTimestamp) {
                    val textTimestamp = "[${toTimestamp(WhisperLib.getTextSegmentT0(ptr, i))} --> ${toTimestamp(WhisperLib.getTextSegmentT1(ptr, i))}]"
                    val textSegment = WhisperLib.getTextSegment(ptr, i)
                    append("$textTimestamp: $textSegment\n")
                } else {
                    append(WhisperLib.getTextSegment(ptr, i))
                }
            }
        }
    }

    suspend fun benchMemory(nthreads: Int): String = withContext(scope.coroutineContext) {
        return@withContext WhisperLib.benchMemcpy(nthreads)
    }

    suspend fun benchGgmlMulMat(nthreads: Int): String = withContext(scope.coroutineContext) {
        return@withContext WhisperLib.benchGgmlMulMat(nthreads)
    }

    suspend fun release() = withContext(scope.coroutineContext) {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    fun releaseSync() {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    protected fun finalize() {
        runBlocking {
            release()
        }
    }

    companion object {
        fun createContextFromFile(filePath: String): WhisperContext {
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create context with path $filePath")
            }
            return WhisperContext(ptr)
        }

        fun createContextFromInputStream(stream: InputStream): WhisperContext {
            val ptr = WhisperLib.initContextFromInputStream(stream)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create context from input stream")
            }
            return WhisperContext(ptr)
        }

        fun createContextFromAsset(assetManager: AssetManager, assetPath: String): WhisperContext {
            val ptr = WhisperLib.initContextFromAsset(assetManager, assetPath)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create context from asset $assetPath")
            }
            return WhisperContext(ptr)
        }

        fun getSystemInfo(): String {
            return WhisperLib.getSystemInfo()
        }

        fun isAvailable(): Boolean = WhisperLib.isLoaded

        fun getLoadError(): String? = WhisperLib.loadError
    }
}

/**
 * WhisperLib - JNI bridge to libwhisper.so
 *
 * IMPORTANT: This class MUST be private with a companion object to match
 * the JNI function name mangling in libwhisper.so:
 * Java_com_whispercpp_whisper_WhisperLib_00024Companion_*
 */
private class WhisperLib {
    companion object {
        var isLoaded: Boolean = false
            private set
        var loadError: String? = null
            private set

        init {
            try {
                Log.d(LOG_TAG, "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
                var loadVfpv4 = false
                var loadV8fp16 = false

                if (isArmEabiV7a()) {
                    // armeabi-v7a needs runtime detection support
                    val cpuInfo = cpuInfo()
                    cpuInfo?.let {
                        Log.d(LOG_TAG, "CPU info available")
                        if (cpuInfo.contains("vfpv4")) {
                            Log.d(LOG_TAG, "CPU supports vfpv4")
                            loadVfpv4 = true
                        }
                    }
                } else if (isArmEabiV8a()) {
                    // ARMv8.2a needs runtime detection support
                    val cpuInfo = cpuInfo()
                    cpuInfo?.let {
                        Log.d(LOG_TAG, "CPU info available")
                        if (cpuInfo.contains("fphp")) {
                            Log.d(LOG_TAG, "CPU supports fp16 arithmetic")
                            loadV8fp16 = true
                        }
                    }
                }

                when {
                    loadVfpv4 -> {
                        Log.d(LOG_TAG, "Loading libwhisper_vfpv4.so")
                        System.loadLibrary("whisper_vfpv4")
                    }
                    loadV8fp16 -> {
                        Log.d(LOG_TAG, "Loading libwhisper_v8fp16_va.so")
                        System.loadLibrary("whisper_v8fp16_va")
                    }
                    else -> {
                        Log.d(LOG_TAG, "Loading libwhisper.so")
                        System.loadLibrary("whisper")
                    }
                }

                isLoaded = true
                Log.d(LOG_TAG, "Whisper library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                isLoaded = false
                loadError = e.message
                Log.e(LOG_TAG, "Failed to load whisper library: ${e.message}")
            }
        }

        // JNI methods - these names are mangled by JNI to:
        // Java_com_whispercpp_whisper_WhisperLib_00024Companion_<methodName>

        external fun initContextFromInputStream(inputStream: InputStream): Long
        external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long
        external fun initContext(modelPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long
        external fun getSystemInfo(): String
        external fun benchMemcpy(nthread: Int): String
        external fun benchGgmlMulMat(nthread: Int): String
    }
}

/**
 * CPU configuration helper for optimal thread count
 */
object WhisperCpuConfig {
    val preferredThreadCount: Int
        get() = maxOf(1, Runtime.getRuntime().availableProcessors() - 2)
}

// Timestamp formatting helper
// 500 -> 00:00:05.000
// 6000 -> 00:01:00.000
private fun toTimestamp(t: Long, comma: Boolean = false): String {
    var msec = t * 10
    val hr = msec / (1000 * 60 * 60)
    msec -= hr * (1000 * 60 * 60)
    val min = msec / (1000 * 60)
    msec -= min * (1000 * 60)
    val sec = msec / 1000
    msec -= sec * 1000

    val delimiter = if (comma) "," else "."
    return String.format("%02d:%02d:%02d%s%03d", hr, min, sec, delimiter, msec)
}

private fun isArmEabiV7a(): Boolean {
    return Build.SUPPORTED_ABIS[0] == "armeabi-v7a"
}

private fun isArmEabiV8a(): Boolean {
    return Build.SUPPORTED_ABIS[0] == "arm64-v8a"
}

private fun cpuInfo(): String? {
    return try {
        File("/proc/cpuinfo").inputStream().bufferedReader().use {
            it.readText()
        }
    } catch (e: Exception) {
        Log.w(LOG_TAG, "Couldn't read /proc/cpuinfo", e)
        null
    }
}

package com.ginference.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ShortBuffer
import kotlin.coroutines.coroutineContext

class AudioRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: Flow<Boolean> = _isRecording.asStateFlow()

    private val audioBuffer = mutableListOf<Short>()
    private val bufferLock = Any()

    private var amplitudeCallback: ((Float) -> Unit)? = null

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(onAmplitude: ((Float) -> Unit)? = null): Result<Unit> {
        if (_isRecording.value) {
            return Result.failure(IllegalStateException("Already recording"))
        }

        if (!hasRecordPermission()) {
            return Result.failure(SecurityException("RECORD_AUDIO permission not granted"))
        }

        amplitudeCallback = onAmplitude

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                return Result.failure(RuntimeException("Failed to get minimum buffer size"))
            }

            val bufferSize = maxOf(minBufferSize * 2, BUFFER_SIZE_SAMPLES * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return Result.failure(RuntimeException("Failed to initialize AudioRecord"))
            }

            synchronized(bufferLock) {
                audioBuffer.clear()
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingThread = Thread {
                readAudioLoop()
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            Log.d(TAG, "Recording started at ${SAMPLE_RATE}Hz mono")
            return Result.success(Unit)

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
            return Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return Result.failure(e)
        }
    }

    private fun readAudioLoop() {
        val buffer = ShortArray(BUFFER_SIZE_SAMPLES)

        while (_isRecording.value && audioRecord != null) {
            val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1

            if (readResult > 0) {
                synchronized(bufferLock) {
                    for (i in 0 until readResult) {
                        audioBuffer.add(buffer[i])
                    }
                }

                amplitudeCallback?.let { callback ->
                    val amplitude = calculateAmplitude(buffer, readResult)
                    callback(amplitude)
                }

                if (audioBuffer.size > MAX_RECORDING_SAMPLES) {
                    Log.w(TAG, "Recording exceeded maximum length, stopping")
                    stopRecording()
                }
            } else if (readResult < 0) {
                Log.e(TAG, "AudioRecord read error: $readResult")
                break
            }
        }
    }

    private fun calculateAmplitude(buffer: ShortArray, size: Int): Float {
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        val rms = kotlin.math.sqrt(sum / size)
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    fun stopRecording(): ShortArray {
        _isRecording.value = false

        try {
            recordingThread?.join(1000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Recording thread interrupted", e)
        }
        recordingThread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val result: ShortArray
        synchronized(bufferLock) {
            result = audioBuffer.toShortArray()
            audioBuffer.clear()
        }

        amplitudeCallback = null

        val durationSeconds = result.size.toFloat() / SAMPLE_RATE
        Log.d(TAG, "Recording stopped: ${result.size} samples (${String.format("%.1f", durationSeconds)}s)")

        return result
    }

    suspend fun recordForDuration(durationMs: Long, onAmplitude: ((Float) -> Unit)? = null): Result<ShortArray> =
        withContext(Dispatchers.IO) {
            val startResult = startRecording(onAmplitude)
            if (startResult.isFailure) {
                return@withContext Result.failure(startResult.exceptionOrNull()!!)
            }

            var elapsed = 0L
            while (elapsed < durationMs && coroutineContext.isActive) {
                kotlinx.coroutines.delay(100)
                elapsed += 100
            }

            val audioData = stopRecording()
            Result.success(audioData)
        }

    fun isCurrentlyRecording(): Boolean = _isRecording.value

    fun getRecordingDurationMs(): Long {
        synchronized(bufferLock) {
            return (audioBuffer.size * 1000L) / SAMPLE_RATE
        }
    }

    fun getRecordingSamples(): Int {
        synchronized(bufferLock) {
            return audioBuffer.size
        }
    }

    private fun cleanup() {
        _isRecording.value = false
        recordingThread = null
        audioRecord?.release()
        audioRecord = null
        amplitudeCallback = null
        synchronized(bufferLock) {
            audioBuffer.clear()
        }
    }

    fun release() {
        if (_isRecording.value) {
            stopRecording()
        }
        cleanup()
        Log.d(TAG, "AudioRecorder released")
    }

    companion object {
        private const val TAG = "AudioRecorder"

        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        private const val BUFFER_SIZE_SAMPLES = 1024

        private const val MAX_RECORDING_SECONDS = 60
        private const val MAX_RECORDING_SAMPLES = SAMPLE_RATE * MAX_RECORDING_SECONDS

        fun formatDuration(samples: Int): String {
            val seconds = samples.toFloat() / SAMPLE_RATE
            return if (seconds < 60) {
                String.format("%.1fs", seconds)
            } else {
                val minutes = (seconds / 60).toInt()
                val remainingSeconds = seconds % 60
                String.format("%d:%04.1f", minutes, remainingSeconds)
            }
        }
    }
}

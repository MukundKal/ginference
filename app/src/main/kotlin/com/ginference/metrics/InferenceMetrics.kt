package com.ginference.metrics

import android.util.Log
import kotlin.math.roundToInt

data class InferenceSession(
    val startTime: Long,
    val endTime: Long,
    val ttft: Long,
    val tokensGenerated: Int,
    val totalTime: Long,
    val tokensPerSecond: Float,
    val promptLength: Int
)

data class InferenceStats(
    val currentTTFT: Long,
    val currentTokensPerSec: Float,
    val currentLatency: Long,
    val avgTTFT: Long,
    val avgTokensPerSec: Float,
    val avgLatency: Long,
    val totalSessions: Int,
    val totalTokens: Int
)

class InferenceMetrics {

    private val sessions = mutableListOf<InferenceSession>()
    private var currentSessionStart: Long = 0L
    private var currentFirstTokenTime: Long = 0L
    private var currentTokenCount: Int = 0
    private var currentPromptLength: Int = 0

    fun startSession(promptLength: Int) {
        currentSessionStart = System.currentTimeMillis()
        currentFirstTokenTime = 0L
        currentTokenCount = 0
        currentPromptLength = promptLength
        Log.d(TAG, "Inference session started, prompt length: $promptLength")
    }

    fun recordFirstToken() {
        if (currentFirstTokenTime == 0L) {
            currentFirstTokenTime = System.currentTimeMillis() - currentSessionStart
            Log.d(TAG, "First token: ${currentFirstTokenTime}ms")
        }
    }

    fun recordToken() {
        currentTokenCount++
    }

    fun endSession(): InferenceSession? {
        if (currentSessionStart == 0L) return null

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - currentSessionStart
        val tokensPerSec = if (totalTime > 0) {
            (currentTokenCount * 1000f) / totalTime
        } else 0f

        val session = InferenceSession(
            startTime = currentSessionStart,
            endTime = endTime,
            ttft = currentFirstTokenTime,
            tokensGenerated = currentTokenCount,
            totalTime = totalTime,
            tokensPerSecond = tokensPerSec,
            promptLength = currentPromptLength
        )

        sessions.add(session)
        if (sessions.size > MAX_SESSIONS) {
            sessions.removeAt(0)
        }

        Log.d(TAG, "Session ended: ${currentTokenCount} tokens in ${totalTime}ms (${String.format("%.2f", tokensPerSec)} tok/s)")

        currentSessionStart = 0L
        return session
    }

    fun getStats(): InferenceStats {
        val recent = sessions.takeLast(10)

        if (recent.isEmpty()) {
            return InferenceStats(
                currentTTFT = currentFirstTokenTime,
                currentTokensPerSec = 0f,
                currentLatency = if (currentSessionStart > 0L) {
                    System.currentTimeMillis() - currentSessionStart
                } else 0L,
                avgTTFT = 0L,
                avgTokensPerSec = 0f,
                avgLatency = 0L,
                totalSessions = 0,
                totalTokens = 0
            )
        }

        val avgTTFT = recent.map { it.ttft }.average().toLong()
        val avgTokensPerSec = recent.map { it.tokensPerSecond }.average().toFloat()
        val avgLatency = recent.map { it.totalTime }.average().toLong()
        val totalTokens = sessions.sumOf { it.tokensGenerated }

        val currentLatency = if (currentSessionStart > 0L) {
            System.currentTimeMillis() - currentSessionStart
        } else {
            sessions.lastOrNull()?.totalTime ?: 0L
        }

        val currentTokensPerSec = sessions.lastOrNull()?.tokensPerSecond ?: 0f

        return InferenceStats(
            currentTTFT = currentFirstTokenTime.takeIf { it > 0L } ?: sessions.lastOrNull()?.ttft ?: 0L,
            currentTokensPerSec = currentTokensPerSec,
            currentLatency = currentLatency,
            avgTTFT = avgTTFT,
            avgTokensPerSec = avgTokensPerSec,
            avgLatency = avgLatency,
            totalSessions = sessions.size,
            totalTokens = totalTokens
        )
    }

    fun getCurrentTTFT(): Long = currentFirstTokenTime

    fun getCurrentTokensPerSecond(): Float {
        if (currentSessionStart == 0L || currentTokenCount == 0) return 0f
        val elapsed = System.currentTimeMillis() - currentSessionStart
        return if (elapsed > 0) (currentTokenCount * 1000f) / elapsed else 0f
    }

    fun getCurrentLatency(): Long {
        return if (currentSessionStart > 0L) {
            System.currentTimeMillis() - currentSessionStart
        } else 0L
    }

    fun getAverageTTFT(): Long {
        if (sessions.isEmpty()) return 0L
        return sessions.map { it.ttft }.average().toLong()
    }

    fun getAverageTokensPerSecond(): Float {
        if (sessions.isEmpty()) return 0f
        return sessions.map { it.tokensPerSecond }.average().toFloat()
    }

    fun getTotalTokens(): Int {
        return sessions.sumOf { it.tokensGenerated }
    }

    fun getSessionCount(): Int = sessions.size

    fun reset() {
        sessions.clear()
        currentSessionStart = 0L
        currentFirstTokenTime = 0L
        currentTokenCount = 0
        currentPromptLength = 0
        Log.d(TAG, "Metrics reset")
    }

    fun formatMetric(label: String, value: String): String {
        return "$label: $value"
    }

    fun formatTime(ms: Long): String {
        return when {
            ms < 1000 -> "${ms}ms"
            ms < 60000 -> "${String.format("%.2f", ms / 1000f)}s"
            else -> "${(ms / 60000).toInt()}m ${((ms % 60000) / 1000).toInt()}s"
        }
    }

    fun formatTokensPerSecond(tps: Float): String {
        return String.format("%.2f", tps)
    }

    companion object {
        private const val TAG = "InferenceMetrics"
        private const val MAX_SESSIONS = 100
    }
}

package com.ginference.metrics

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log

data class MemoryMetrics(
    val totalRAM: Long,
    val availableRAM: Long,
    val usedRAM: Long,
    val appRAM: Long,
    val appNativeHeap: Long,
    val appJavaHeap: Long,
    val percentUsed: Float
)

data class VRAMMetrics(
    val totalVRAM: Long,
    val usedVRAM: Long,
    val estimatedAppVRAM: Long,
    val percentUsed: Float
)

data class CPUMetrics(
    val totalCores: Int,
    val usagePercent: Float,
    val userPercent: Float,
    val systemPercent: Float,
    val idlePercent: Float
)

data class GPUMetrics(
    val usagePercent: Float,
    val memoryUsed: Long,
    val frequencyMHz: Int,
    val temperature: Float
)

data class ThermalMetrics(
    val currentTemperature: Float,
    val thermalStatus: Int,
    val statusName: String,
    val isThrottling: Boolean
)

class SystemMetrics(private val context: Context) {

    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    fun getRAMMetrics(): MemoryMetrics {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRAM = memInfo.totalMem
        val availableRAM = memInfo.availMem
        val usedRAM = totalRAM - availableRAM
        val percentUsed = (usedRAM.toFloat() / totalRAM.toFloat()) * 100f

        val debugMemInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMemInfo)

        val appRAM = debugMemInfo.totalPss * 1024L
        val appNativeHeap = debugMemInfo.nativePss * 1024L
        val appJavaHeap = debugMemInfo.dalvikPss * 1024L

        Log.v(TAG, "RAM - Total: ${formatBytes(totalRAM)}, Used: ${formatBytes(usedRAM)} (${String.format("%.1f", percentUsed)}%)")
        Log.v(TAG, "App RAM: ${formatBytes(appRAM)}, Native: ${formatBytes(appNativeHeap)}, Java: ${formatBytes(appJavaHeap)}")

        return MemoryMetrics(
            totalRAM = totalRAM,
            availableRAM = availableRAM,
            usedRAM = usedRAM,
            appRAM = appRAM,
            appNativeHeap = appNativeHeap,
            appJavaHeap = appJavaHeap,
            percentUsed = percentUsed
        )
    }

    fun getTotalRAM(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    fun getAvailableRAM(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }

    fun getUsedRAM(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem - memInfo.availMem
    }

    fun getAppRAMUsage(): Long {
        val debugMemInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMemInfo)
        return debugMemInfo.totalPss * 1024L
    }

    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    fun getMemoryThreshold(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.threshold
    }

    fun getVRAMMetrics(): VRAMMetrics {
        val debugMemInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMemInfo)

        val totalRAM = getTotalRAM()
        val gpuMemory = debugMemInfo.getMemoryStat("summary.graphics")?.toLongOrNull() ?: 0L
        val gpuMemoryBytes = gpuMemory * 1024L

        val estimatedTotalVRAM = totalRAM / 4
        val estimatedUsedVRAM = (estimatedTotalVRAM * 0.3).toLong()
        val percentUsed = (gpuMemoryBytes.toFloat() / estimatedTotalVRAM.toFloat()) * 100f

        Log.v(TAG, "VRAM - Estimated Total: ${formatBytes(estimatedTotalVRAM)}, GPU Graphics: ${formatBytes(gpuMemoryBytes)}")

        return VRAMMetrics(
            totalVRAM = estimatedTotalVRAM,
            usedVRAM = estimatedUsedVRAM,
            estimatedAppVRAM = gpuMemoryBytes,
            percentUsed = percentUsed.coerceIn(0f, 100f)
        )
    }

    fun getGPUMemoryUsage(): Long {
        val debugMemInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMemInfo)
        val gpuMemory = debugMemInfo.getMemoryStat("summary.graphics")?.toLongOrNull() ?: 0L
        return gpuMemory * 1024L
    }

    fun getCPUMetrics(): CPUMetrics {
        val cores = Runtime.getRuntime().availableProcessors()
        val cpuUsage = getCurrentCPUUsage()

        Log.v(TAG, "CPU - Cores: $cores, Usage: ${String.format("%.1f", cpuUsage)}%")

        return CPUMetrics(
            totalCores = cores,
            usagePercent = cpuUsage,
            userPercent = 0f,
            systemPercent = 0f,
            idlePercent = 100f - cpuUsage
        )
    }

    private fun getCurrentCPUUsage(): Float {
        return try {
            val statFile = java.io.File("/proc/stat")
            if (!statFile.exists()) return 0f

            val reader = statFile.bufferedReader()
            val cpuLine = reader.readLine() ?: return 0f
            reader.close()

            val tokens = cpuLine.split("\\s+".toRegex())
            if (tokens.size < 5) return 0f

            val user = tokens[1].toLongOrNull() ?: 0L
            val nice = tokens[2].toLongOrNull() ?: 0L
            val system = tokens[3].toLongOrNull() ?: 0L
            val idle = tokens[4].toLongOrNull() ?: 0L

            val total = user + nice + system + idle

            if (lastCpuTotal == 0L) {
                lastCpuTotal = total
                lastCpuIdle = idle
                return 0f
            }

            val totalDelta = total - lastCpuTotal
            val idleDelta = idle - lastCpuIdle

            lastCpuTotal = total
            lastCpuIdle = idle

            if (totalDelta == 0L) return 0f

            val usage = ((totalDelta - idleDelta).toFloat() / totalDelta.toFloat()) * 100f
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read CPU usage", e)
            0f
        }
    }

    fun getCPUCoreCount(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    fun getGPUMetrics(): GPUMetrics {
        val gpuUsage = getGPUUsagePercent()
        val gpuMemory = getGPUMemoryUsage()
        val gpuFreq = getGPUFrequency()
        val gpuTemp = getGPUTemperature()

        Log.v(TAG, "GPU - Usage: ${String.format("%.1f", gpuUsage)}%, Memory: ${formatBytes(gpuMemory)}, Freq: ${gpuFreq}MHz, Temp: ${String.format("%.1f", gpuTemp)}°C")

        return GPUMetrics(
            usagePercent = gpuUsage,
            memoryUsed = gpuMemory,
            frequencyMHz = gpuFreq,
            temperature = gpuTemp
        )
    }

    private fun getGPUUsagePercent(): Float {
        return try {
            val gpuLoadFile = java.io.File("/sys/class/kgsl/kgsl-3d0/gpubusy")
            if (!gpuLoadFile.exists()) return 0f

            val busyLine = gpuLoadFile.readText().trim()
            val parts = busyLine.split(" ")
            if (parts.size >= 2) {
                val busy = parts[0].toLongOrNull() ?: 0L
                val total = parts[1].toLongOrNull() ?: 1L
                if (total > 0) {
                    return ((busy.toFloat() / total.toFloat()) * 100f).coerceIn(0f, 100f)
                }
            }
            0f
        } catch (e: Exception) {
            Log.v(TAG, "GPU usage via kgsl not available")
            0f
        }
    }

    private fun getGPUFrequency(): Int {
        return try {
            val freqFile = java.io.File("/sys/class/kgsl/kgsl-3d0/gpuclk")
            if (freqFile.exists()) {
                val freqHz = freqFile.readText().trim().toLongOrNull() ?: 0L
                (freqHz / 1_000_000).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            Log.v(TAG, "GPU frequency not available")
            0
        }
    }

    private fun getGPUTemperature(): Float {
        return try {
            val tempFile = java.io.File("/sys/class/kgsl/kgsl-3d0/temp")
            if (tempFile.exists()) {
                val temp = tempFile.readText().trim().toFloatOrNull() ?: 0f
                temp
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.v(TAG, "GPU temperature not available")
            0f
        }
    }

    fun getThermalMetrics(): ThermalMetrics {
        val temp = getCurrentTemperature()
        val status = getThermalStatus()
        val statusName = getThermalStatusName(status)
        val isThrottling = status >= 3

        Log.v(TAG, "Thermal - Temp: ${String.format("%.1f", temp)}°C, Status: $statusName, Throttling: $isThrottling")

        return ThermalMetrics(
            currentTemperature = temp,
            thermalStatus = status,
            statusName = statusName,
            isThrottling = isThrottling
        )
    }

    private fun getCurrentTemperature(): Float {
        return try {
            val thermalZones = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )

            for (zonePath in thermalZones) {
                val file = java.io.File(zonePath)
                if (file.exists()) {
                    val temp = file.readText().trim().toFloatOrNull() ?: 0f
                    return if (temp > 1000f) temp / 1000f else temp
                }
            }
            0f
        } catch (e: Exception) {
            Log.v(TAG, "Temperature reading not available")
            0f
        }
    }

    private fun getThermalStatus(): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                powerManager.currentThermalStatus
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun getThermalStatusName(status: Int): String {
        return when (status) {
            0 -> "NONE"
            1 -> "LIGHT"
            2 -> "MODERATE"
            3 -> "SEVERE"
            4 -> "CRITICAL"
            5 -> "EMERGENCY"
            6 -> "SHUTDOWN"
            else -> "UNKNOWN"
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> String.format("%.2fGB", bytes / (1024f * 1024f * 1024f))
        }
    }

    companion object {
        private const val TAG = "SystemMetrics"
    }
}

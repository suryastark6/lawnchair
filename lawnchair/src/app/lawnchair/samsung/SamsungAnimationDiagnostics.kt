package app.lawnchair.samsung

import android.util.Log

/**
 * Diagnostic logger and runtime telemetry for Samsung One UI 8 transition execution.
 */
object SamsungAnimationDiagnostics {
    private const val TAG = "SamsungDiagnostics"

    private var isDebugLoggingEnabled = true

    fun setDebugLogging(enabled: Boolean) {
        isDebugLoggingEnabled = enabled
    }

    private var totalFrameDrops = 0
    private var totalTransitionsRecorded = 0

    fun logTransitionStart(transitionType: String, targetInfo: String) {
        totalTransitionsRecorded++
        if (!isDebugLoggingEnabled) return
        Log.i(TAG, "[TRANSITION START] Type: $transitionType | Target: $targetInfo | OneUI8: ${SamsungTransitionCapabilities.isOneUi8OrHigher}")
    }

    fun logTransitionEnd(transitionType: String, durationMs: Long) {
        if (!isDebugLoggingEnabled) return
        Log.i(TAG, "[TRANSITION END] Type: $transitionType | Elapsed: ${durationMs}ms")
    }

    fun logFrameDrop(type: String, missedFrames: Int, frameDurationMs: Float) {
        totalFrameDrops += missedFrames
        if (!isDebugLoggingEnabled) return
        Log.w(TAG, "[FRAME DROP] Type: $type | Missed: $missedFrames frames | Duration: ${frameDurationMs}ms")
    }

    fun getPerformanceSummary(): Map<String, Any> {
        return mapOf(
            "totalTransitions" to totalTransitionsRecorded,
            "totalFrameDrops" to totalFrameDrops,
            "isOneUi8" to SamsungTransitionCapabilities.isOneUi8OrHigher
        )
    }

    fun logException(contextTag: String, throwable: Throwable) {
        if (!isDebugLoggingEnabled) return
        Log.w(TAG, "[DIAGNOSTIC WARN] Context: $contextTag | Error: ${throwable.message}")
    }
}

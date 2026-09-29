package app.lawnchair.samsung

import android.content.Context
import android.util.Log

/**
 * Isolated bridge for Samsung platform-specific hooks, reflection, and hidden API calls.
 * All invocations fail safely to avoid crashing the launcher process.
 */
object SamsungFrameworkBridge {
    private const val TAG = "SamsungBridge"

    private val methodCache = mutableMapOf<String, Any?>()

    /**
     * Attempts to query Samsung custom display refresh rate policy if accessible.
     */
    fun getSamsungPreferredRefreshRate(context: Context): Float? {
        if (!SamsungTransitionCapabilities.isSamsungDevice) return null

        return runCatching {
            val display = context.display ?: return null
            val supportedModes = display.supportedModes
            // Identify peak refresh rate available on device (e.g. 120Hz on Samsung AMOLED)
            supportedModes.maxOfOrNull { it.refreshRate }
        }.getOrElse {
            Log.d(TAG, "Refresh rate query skipped: ${it.message}")
            null
        }
    }

    /**
     * Safely executes an isolated reflection call against system components.
     */
    inline fun <T> safeInvoke(tag: String, fallback: T, block: () -> T): T {
        return try {
            block()
        } catch (t: Throwable) {
            SamsungAnimationDiagnostics.logException(tag, t)
            fallback
        }
    }
}

package app.lawnchair.samsung

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import android.provider.Settings
import android.util.Log

/**
 * Detects device capabilities, platform versions, and navigation modes
 * specific to Samsung One UI environments on Android 16+.
 */
object SamsungTransitionCapabilities {
    private const val TAG = "SamsungCapabilities"

    private const val PROP_ONEUI_VERSION = "ro.build.version.oneui"
    private const val PROP_SEP_VERSION = "ro.build.version.sep"
    private const val PROP_SEM_PLATFORM = "ro.build.version.sem"

    /**
     * Whether the current device manufacturer or brand is Samsung.
     */
    val isSamsungDevice: Boolean by lazy {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        manufacturer.contains("samsung") || brand.contains("samsung")
    }

    /**
     * Resolves the detected One UI version as an integer (e.g. 80000 for One UI 8.0, 70000 for 7.0).
     */
    val oneUiVersionCode: Int by lazy {
        if (!isSamsungDevice) return@lazy 0

        runCatching {
            val semVersionStr = getSystemProperty(PROP_ONEUI_VERSION)
                .ifEmpty { getSystemProperty(PROP_SEP_VERSION) }
                .ifEmpty { getSystemProperty(PROP_SEM_PLATFORM) }

            if (semVersionStr.isNotEmpty()) {
                semVersionStr.toIntOrNull() ?: parseVersionString(semVersionStr)
            } else {
                // Infer from Android SDK version if property is masked
                if (Build.VERSION.SDK_INT >= 36) 80000 else if (Build.VERSION.SDK_INT == 35) 70000 else 60000
            }
        }.getOrElse {
            Log.w(TAG, "Failed to resolve One UI version: ${it.message}")
            if (Build.VERSION.SDK_INT >= 36) 80000 else 0
        }
    }

    /**
     * True if the device is running Samsung One UI 8.0 or higher on Android 16+.
     */
    val isOneUi8OrHigher: Boolean by lazy {
        isSamsungDevice && (oneUiVersionCode >= 80000 || Build.VERSION.SDK_INT >= 36)
    }

    /**
     * Checks if gesture navigation is currently active in system settings.
     */
    fun isGestureNavEnabled(context: Context): Boolean {
        return runCatching {
            // Samsung uses navigation_mode setting: 2 = full gesture, 1 = 2-button (legacy), 0 = 3-button
            val navMode = Settings.Secure.getInt(
                context.contentResolver,
                "navigation_mode",
                -1,
            )
            navMode == 2
        }.getOrElse { false }
    }

    /**
     * Checks if SurfaceControl operations are supported on this platform.
     */
    val supportsSurfaceControl: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun parseVersionString(version: String): Int {
        val clean = version.filter { it.isDigit() }
        return clean.toIntOrNull() ?: 0
    }

    private fun getSystemProperty(key: String): String {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, key, "") as? String ?: ""
        }.getOrElse { "" }
    }
}

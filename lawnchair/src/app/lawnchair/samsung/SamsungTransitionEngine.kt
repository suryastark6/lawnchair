package app.lawnchair.samsung

import android.content.Intent
import android.view.View
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ActivityOptionsWrapper

/**
 * Main entry point for Samsung One UI 8 transition management in Lawnchair.
 * Coordinates launch, return, and gesture transitions.
 */
class SamsungTransitionEngine(private val launcher: LawnchairLauncher) {

    val capabilities: SamsungTransitionCapabilities = SamsungTransitionCapabilities
    val launchController by lazy { SamsungLaunchAnimationController(launcher) }
    val returnController by lazy { SamsungReturnAnimationController(launcher) }

    /**
     * True if Samsung-specific animation pipeline is enabled and supported on this hardware.
     */
    val isEnabled: Boolean
        get() = capabilities.isSamsungDevice

    /**
     * Intercepts and produces optimized ActivityOptions for app launching on Samsung devices.
     */
    fun getActivityLaunchOptions(v: View?, item: ItemInfo?): ActivityOptionsWrapper? {
        if (!isEnabled) return null
        return launchController.createLaunchOptions(v, item)
    }

    /**
     * Intercepts and processes GestureNavContract intents returning to home.
     */
    fun onNewIntent(intent: Intent): Boolean {
        if (!isEnabled) return false
        return returnController.handleGestureContract(intent)
    }
}

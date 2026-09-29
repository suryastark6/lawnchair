package app.lawnchair.samsung

import android.content.Intent
import android.util.Log
import android.view.View
import app.lawnchair.LawnchairLauncher
import app.lawnchair.views.LawnchairFloatingSurfaceView
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.GestureNavContract

/**
 * Controller responsible for synchronizing GestureNavContract return animations
 * with Samsung One UI 8 home transitions.
 */
class SamsungReturnAnimationController(private val launcher: LawnchairLauncher) {
    private val TAG = "SamsungReturnAnim"

    /**
     * Handles incoming GestureNavContract intents from Samsung SystemUI.
     */
    fun handleGestureContract(intent: Intent): Boolean {
        if (!SamsungTransitionCapabilities.isSamsungDevice) return false

        return runCatching {
            val gnc = GestureNavContract.fromIntent(intent) ?: return false

            SamsungAnimationDiagnostics.logTransitionStart(
                "GESTURE_NAV_RETURN",
                gnc.componentName.flattenToShortString(),
            )

            AbstractFloatingView.closeOpenViews(
                launcher,
                false,
                AbstractFloatingView.TYPE_ICON_SURFACE,
            )

            LawnchairFloatingSurfaceView.show(launcher, gnc)
            true
        }.getOrElse {
            Log.e(TAG, "Error handling Samsung return gesture contract: ${it.message}", it)
            false
        }
    }
}

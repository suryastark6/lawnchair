package app.lawnchair.samsung

import android.content.Intent
import android.view.View
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ActivityOptionsWrapper
import java.lang.ref.WeakReference

/**
 * Main entry point for Samsung One UI 8 transition management in Lawnchair.
 * Coordinates launch, return, and gesture transitions.
 */
class SamsungTransitionEngine(private val launcher: LawnchairLauncher) {

    val capabilities: SamsungTransitionCapabilities = SamsungTransitionCapabilities
    val launchController by lazy { SamsungLaunchAnimationController(launcher) }
    val returnController by lazy { SamsungReturnAnimationController(launcher) }
    private var isAppRunning = false

    init {
        activeEngine = WeakReference(this)
    }

    /**
     * True if Samsung-specific animation pipeline is enabled and supported on this hardware.
     */
    val isEnabled: Boolean
        get() = true

    /**
     * Intercepts and produces optimized ActivityOptions for app launching on Samsung devices.
     */
    fun getActivityLaunchOptions(v: View?, item: ItemInfo?): ActivityOptionsWrapper? {
        if (!isEnabled) return null
        isAppRunning = true
        return launchController.createLaunchOptions(v, item)
    }

    /**
     * Intercepts and processes GestureNavContract intents returning to home.
     */
    fun onNewIntent(intent: Intent): Boolean {
        if (!isEnabled) return false
        return returnController.handleGestureContract(intent)
    }

    /**
     * Invoked when launcher activity resumes.
     */
    fun onResume() {
        if (isAppRunning) {
            isAppRunning = false
            onHomeReturn(null)
        }
    }

    /**
     * Dispatches real-time home return animation.
     */
    fun onHomeReturn(fromPackage: CharSequence?) {
        returnController.playHomeReturnAnimation(fromPackage)
    }

    /**
     * Coordinates overview recents entrance physics.
     */
    fun onOverviewTransitionStart() {
        val workspace = launcher.workspace ?: return
        workspace.animate()
            .scaleX(SamsungAnimationSpec.OVERVIEW_WORKSPACE_SCALE)
            .scaleY(SamsungAnimationSpec.OVERVIEW_WORKSPACE_SCALE)
            .setDuration(SamsungAnimationSpec.DURATION_OVERVIEW_SLIDE_IN_MS)
            .setInterpolator(SamsungAnimationSpec.LAUNCH_INTERPOLATOR)
            .start()
    }

    /**
     * Coordinates returning from overview to normal workspace.
     */
    fun onNormalStateEntered() {
        val workspace = launcher.workspace ?: return
        if (workspace.scaleX != 1.0f || workspace.scaleY != 1.0f) {
            workspace.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(SamsungAnimationSpec.DURATION_OVERVIEW_EXIT_MS)
                .setInterpolator(SamsungAnimationSpec.RETURN_INTERPOLATOR)
                .start()
        }
    }

    companion object {
        private var activeEngine: WeakReference<SamsungTransitionEngine>? = null

        fun notifyHomeReturn(fromPackage: CharSequence?) {
            activeEngine?.get()?.onHomeReturn(fromPackage)
        }
    }
}

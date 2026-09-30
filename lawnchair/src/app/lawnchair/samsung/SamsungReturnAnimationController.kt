package app.lawnchair.samsung

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.util.Log
import app.lawnchair.LawnchairLauncher
import app.lawnchair.views.LawnchairFloatingSurfaceView
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.GestureNavContract
import com.android.launcher3.util.Executors

/**
 * Controller responsible for synchronizing return animations
 * with Samsung One UI 8 home transitions and gesture events.
 */
class SamsungReturnAnimationController(private val launcher: LawnchairLauncher) {

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

    /**
     * Initiates a fluid return-to-home spring animation when real-time gesture or window transition is detected.
     */
    fun playHomeReturnAnimation(fromPackage: CharSequence?) {
        Executors.MAIN_EXECUTOR.execute {
            runCatching {
                val workspace = launcher.workspace ?: return@execute
                val dragLayer = launcher.dragLayer ?: return@execute

                val startTime = System.currentTimeMillis()
                SamsungAnimationDiagnostics.logTransitionStart(
                    "HOME_RETURN",
                    "From: ${fromPackage ?: "unknown"}",
                )

                Log.i(TAG, "[RETURN START] Initiating Samsung One UI 8 home reveal animation from $fromPackage")

                // Prepare initial state
                workspace.scaleX = 0.92f
                workspace.scaleY = 0.92f
                workspace.alpha = 0.70f

                var frameIndex = 0
                val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
                animator.duration = SamsungAnimationSpec.DURATION_HOME_REVEAL_MS
                animator.interpolator = SamsungAnimationSpec.RETURN_INTERPOLATOR

                animator.addUpdateListener { va ->
                    val fraction = va.animatedFraction
                    val currentScale = 0.92f + (0.08f * fraction)
                    val currentAlpha = 0.70f + (0.30f * fraction)

                    workspace.scaleX = currentScale
                    workspace.scaleY = currentScale
                    workspace.alpha = currentAlpha

                    frameIndex++
                    val elapsedMs = System.currentTimeMillis() - startTime
                    Log.d(
                        TAG,
                        "[RETURN FRAME %03d] t=%03dms | progress=%.3f | scale=%.3f | alpha=%.3f".format(
                            frameIndex,
                            elapsedMs,
                            fraction,
                            currentScale,
                            currentAlpha,
                        ),
                    )
                }

                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        workspace.scaleX = 1.0f
                        workspace.scaleY = 1.0f
                        workspace.alpha = 1.0f
                        val totalElapsed = System.currentTimeMillis() - startTime
                        SamsungAnimationDiagnostics.logTransitionEnd("HOME_RETURN", totalElapsed)
                        Log.i(
                            TAG,
                            "[RETURN COMPLETE] Home return transition finished in ${totalElapsed}ms across $frameIndex frames",
                        )
                    }
                })

                animator.start()
            }.onFailure {
                Log.e(TAG, "Failed to execute home return animation: ${it.message}", it)
            }
        }
    }

    companion object {
        private const val TAG = "SamsungReturnAnim"
    }
}

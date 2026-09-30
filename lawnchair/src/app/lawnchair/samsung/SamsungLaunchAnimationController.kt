package app.lawnchair.samsung

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.Display
import android.view.View
import android.window.SplashScreen
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.BubbleTextView
import com.android.launcher3.Utilities
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ActivityOptionsWrapper
import com.android.launcher3.util.RunnableList
import com.android.launcher3.views.FloatingIconView

/**
 * Controller responsible for optimizing application launch animations on Samsung devices.
 * Coordinates icon coordinate extraction, spring compression, scale-up options,
 * and high-frequency frame-by-frame diagnostic telemetry.
 */
class SamsungLaunchAnimationController(private val launcher: LawnchairLauncher) {

    /**
     * Creates an optimized ActivityOptions bundle for launching an app on Samsung One UI 8.
     */
    fun createLaunchOptions(sourceView: View?, item: ItemInfo?): ActivityOptionsWrapper {
        val startTime = System.currentTimeMillis()
        val source = sourceView ?: launcher.dragLayer

        val screenPos = IntArray(2)
        source.getLocationOnScreen(screenPos)

        var iconLeft = 0
        var iconTop = 0
        var iconWidth = source.measuredWidth
        var iconHeight = source.measuredHeight

        if (source is BubbleTextView) {
            val icon = source.icon
            if (icon != null) {
                val iconBounds = icon.bounds
                iconLeft = (iconWidth - iconBounds.width()) / 2
                iconTop = source.paddingTop
                iconWidth = iconBounds.width()
                iconHeight = iconBounds.height()
            }
        }

        val startScreenLeft = screenPos[0] + iconLeft
        val startScreenTop = screenPos[1] + iconTop
        val startScreenBounds = Rect(
            startScreenLeft,
            startScreenTop,
            startScreenLeft + iconWidth.coerceAtLeast(1),
            startScreenTop + iconHeight.coerceAtLeast(1),
        )

        val displayMetrics = launcher.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val targetScreenBounds = Rect(0, 0, screenWidth, screenHeight)

        SamsungAnimationDiagnostics.logTransitionStart(
            "APP_LAUNCH",
            "${source.javaClass.simpleName} at ${startScreenBounds.toShortString()}",
        )

        Log.i(
            TAG,
            "[LAUNCH START] Origin: ${startScreenBounds.toShortString()} [w=$iconWidth, h=$iconHeight] | Target: ${targetScreenBounds.toShortString()}",
        )

        // Visual spring feedback on icon and workspace depth layer zoom
        source.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .setDuration(100)
            .withEndAction {
                source.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(240)
                    .setInterpolator(SamsungAnimationSpec.LAUNCH_INTERPOLATOR)
                    .start()
            }
            .start()

        launcher.workspace?.let { ws ->
            ws.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .alpha(0.80f)
                .setDuration(300)
                .setInterpolator(SamsungAnimationSpec.LAUNCH_INTERPOLATOR)
                .start()
        }

        // High-precision frame-by-frame animation tracking
        var frameIndex = 0
        val animStartTime = System.currentTimeMillis()
        val frameAnimator = ValueAnimator.ofFloat(0.0f, 1.0f)
        frameAnimator.duration = SamsungAnimationSpec.DURATION_APP_LAUNCH_MS
        frameAnimator.interpolator = SamsungAnimationSpec.LAUNCH_INTERPOLATOR
        frameAnimator.addUpdateListener { va ->
            val fraction = va.animatedFraction
            val now = System.currentTimeMillis()
            val elapsedMs = now - animStartTime
            val currentBounds = calculateRevealBounds(startScreenBounds, fraction, screenWidth, screenHeight)
            frameIndex++
            Log.d(
                TAG,
                "[FRAME %03d] t=%03dms | progress=%.3f | bounds=%s [w=%d, h=%d] -> target=%s".format(
                    frameIndex,
                    elapsedMs,
                    fraction,
                    currentBounds.toShortString(),
                    currentBounds.width(),
                    currentBounds.height(),
                    targetScreenBounds.toShortString(),
                ),
            )
        }
        frameAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val totalElapsed = System.currentTimeMillis() - animStartTime
                Log.i(
                    TAG,
                    "[LAUNCH COMPLETE] Transition finished in ${totalElapsed}ms across $frameIndex frames. Reached: ${targetScreenBounds.toShortString()}",
                )
            }
        })
        frameAnimator.start()

        val options = Utilities.allowBGLaunch(
            ActivityOptions.makeClipRevealAnimation(
                source,
                iconLeft,
                iconTop,
                iconWidth.coerceAtLeast(1),
                iconHeight.coerceAtLeast(1),
            ),
        )

        if (Utilities.ATLEAST_T) {
            options.splashScreenStyle = SplashScreen.SPLASH_SCREEN_STYLE_ICON
        }

        // Force full-screen windowing mode (WINDOWING_MODE_FULLSCREEN = 1)
        // Never call setLaunchBounds() here as Samsung One UI interprets launch bounds as a pop-up / freeform request.
        runCatching {
            val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
            method.invoke(options, 1)
        }

        options.launchDisplayId = source.display?.displayId ?: Display.DEFAULT_DISPLAY

        val callbacks = RunnableList()
        callbacks.add {
            val elapsed = System.currentTimeMillis() - startTime
            SamsungAnimationDiagnostics.logTransitionEnd("APP_LAUNCH", elapsed)
        }

        return ActivityOptionsWrapper(options, callbacks)
    }

    companion object {
        private const val TAG = "SamsungLaunchAnim"

        /**
         * Calculates intermediate reveal bounds during continuous squircle expansion.
         */
        fun calculateRevealBounds(
            sourceBounds: Rect,
            progress: Float,
            screenWidth: Int,
            screenHeight: Int,
        ): Rect {
            val clampedProgress = progress.coerceIn(0.0f, 1.0f)
            val left = (sourceBounds.left * (1.0f - clampedProgress)).toInt()
            val top = (sourceBounds.top * (1.0f - clampedProgress)).toInt()
            val right = (sourceBounds.right + (screenWidth - sourceBounds.right) * clampedProgress).toInt()
            val bottom = (sourceBounds.bottom + (screenHeight - sourceBounds.bottom) * clampedProgress).toInt()
            return Rect(left, top, right, bottom)
        }
    }
}


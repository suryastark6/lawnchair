package app.lawnchair.samsung

import android.app.ActivityOptions
import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
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
 */
class SamsungLaunchAnimationController(private val launcher: LawnchairLauncher) {

    /**
     * Creates an optimized ActivityOptions bundle for launching an app on Samsung One UI 8.
     */
    fun createLaunchOptions(sourceView: View?, item: ItemInfo?): ActivityOptionsWrapper {
        val startTime = System.currentTimeMillis()
        SamsungAnimationDiagnostics.logTransitionStart(
            "APP_LAUNCH",
            sourceView?.javaClass?.simpleName ?: "null",
        )

        val targetBounds = Rect()
        val targetPosition = RectF()

        var left = 0
        var top = 0
        var width = sourceView?.measuredWidth ?: 0
        var height = sourceView?.measuredHeight ?: 0

        if (sourceView is BubbleTextView) {
            val icon = sourceView.icon
            if (icon != null) {
                val iconBounds = icon.bounds
                left = (width - iconBounds.width()) / 2
                top = sourceView.paddingTop
                width = iconBounds.width()
                height = iconBounds.height()
            }
        }

        if (sourceView != null) {
            FloatingIconView.getLocationBoundsForView(
                launcher,
                sourceView,
                false,
                targetPosition,
                targetBounds,
            )
        }

        val options = Utilities.allowBGLaunch(
            ActivityOptions.makeClipRevealAnimation(
                sourceView ?: launcher.dragLayer,
                left,
                top,
                width,
                height,
            ),
        )

        if (Utilities.ATLEAST_T) {
            options.splashScreenStyle = SplashScreen.SPLASH_SCREEN_STYLE_ICON
        }

        options.launchDisplayId = sourceView?.display?.displayId ?: Display.DEFAULT_DISPLAY

        val callbacks = RunnableList()
        callbacks.add {
            val elapsed = System.currentTimeMillis() - startTime
            SamsungAnimationDiagnostics.logTransitionEnd("APP_LAUNCH", elapsed)
        }

        return ActivityOptionsWrapper(options, callbacks)
    }
}

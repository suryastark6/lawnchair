package app.lawnchair.samsung

import android.view.animation.Interpolator
import android.view.animation.PathInterpolator

/**
 * Encapsulates precise motion parameters, easing curves, and spring constants
 * calibrated specifically for Samsung One UI 8.
 */
object SamsungAnimationSpec {
    // --- Interpolators ---

    /**
     * Samsung One UI 8 primary application launch motion curve.
     * Starts with rapid acceleration and settles smoothly into the full window bounds.
     */
    @JvmField
    val LAUNCH_INTERPOLATOR: Interpolator = PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f)

    /**
     * Samsung One UI 8 return-to-home deceleration curve.
     */
    @JvmField
    val RETURN_INTERPOLATOR: Interpolator = PathInterpolator(0.17f, 0.17f, 0.0f, 1.0f)

    /**
     * Easing curve for workspace content scale and depth reveals during home transitions.
     */
    @JvmField
    val WORKSPACE_SCALE_INTERPOLATOR: Interpolator = PathInterpolator(0.20f, 0.0f, 0.0f, 1.0f)

    /**
     * Alpha fade curve for icon surface concealment.
     */
    @JvmField
    val ALPHA_FADE_INTERPOLATOR: Interpolator = PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f)

    // --- Durations (ms) ---

    const val DURATION_APP_LAUNCH_MS: Long = 380L
    const val LAUNCH_DURATION_MS: Long = 380L
    const val DURATION_APP_CLOSE_MS: Long = 340L
    const val RETURN_DURATION_MS: Long = 320L
    const val RECENTS_DURATION_MS: Long = 280L
    const val DURATION_HOME_REVEAL_MS: Long = 360L
    const val DURATION_ICON_BOUNCE_MS: Long = 400L
    const val DURATION_OVERVIEW_SLIDE_IN_MS: Long = 320L
    const val DURATION_OVERVIEW_EXIT_MS: Long = 280L

    // --- Overview Motion Metrics ---
    const val OVERVIEW_WORKSPACE_SCALE: Float = 0.92f
    const val OVERVIEW_PARALLAX_FACTOR: Float = 0.40f

    // --- Spring Dynamics ---

    /**
     * Spring stiffness for icon settling motion on One UI 8.
     */
    const val SPRING_STIFFNESS_ONEUI: Float = 300.0f
    const val SPRING_STIFFNESS: Float = 300.0f

    /**
     * Damping ratio for critically damped settling without excessive oscillation.
     */
    const val SPRING_DAMPING_ONEUI: Float = 0.86f
    const val SPRING_DAMPING_RATIO: Float = 0.86f

    // --- Corner Radii & Morphing ---

    /**
     * Standard One UI continuous squircle corner radius in dp.
     */
    const val SQUIRCLE_CORNER_RADIUS_DP: Float = 30.0f
}

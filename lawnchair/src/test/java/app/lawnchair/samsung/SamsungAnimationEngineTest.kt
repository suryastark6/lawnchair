package app.lawnchair.samsung

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungAnimationEngineTest {

    @Test
    fun testAnimationSpecConstants() {
        assertEquals(380L, SamsungAnimationSpec.LAUNCH_DURATION_MS)
        assertEquals(320L, SamsungAnimationSpec.RETURN_DURATION_MS)
        assertEquals(280L, SamsungAnimationSpec.RECENTS_DURATION_MS)
        assertEquals(300.0f, SamsungAnimationSpec.SPRING_STIFFNESS, 0.001f)
        assertEquals(0.86f, SamsungAnimationSpec.SPRING_DAMPING_RATIO, 0.001f)
    }

    @Test
    fun testLaunchInterpolatorBounds() {
        val launchInterpolator = SamsungAnimationSpec.LAUNCH_INTERPOLATOR
        assertNotNull(launchInterpolator)
        assertEquals(0.0f, launchInterpolator.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, launchInterpolator.getInterpolation(1.0f), 0.001f)
        
        // Midpoint should follow cubic bezier progression
        val mid = launchInterpolator.getInterpolation(0.5f)
        assertTrue(mid in 0.4f..0.95f)
    }

    @Test
    fun testReturnInterpolatorBounds() {
        val returnInterpolator = SamsungAnimationSpec.RETURN_INTERPOLATOR
        assertNotNull(returnInterpolator)
        assertEquals(0.0f, returnInterpolator.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, returnInterpolator.getInterpolation(1.0f), 0.001f)
    }

    @Test
    fun testDiagnosticsEventLogging() {
        SamsungAnimationDiagnostics.logTransitionStart(
            type = "LAUNCH",
            source = "workspace_icon",
            target = "com.sec.android.app.camera"
        )

        SamsungAnimationDiagnostics.logFrameDrop(
            type = "LAUNCH",
            missedFrames = 2,
            frameDurationMs = 33.4f
        )

        SamsungAnimationDiagnostics.logTransitionEnd(
            type = "LAUNCH",
            success = true,
            totalDurationMs = 380L
        )

        val stats = SamsungAnimationDiagnostics.getPerformanceSummary()
        assertNotNull(stats)
    }

    @Test
    fun testLaunchRevealBoundsCalculation() {
        val sourceRect = Rect(100, 200, 200, 300)
        val displayWidth = 1080
        val displayHeight = 2400

        val revealRect = SamsungLaunchAnimationController.calculateRevealBounds(
            sourceBounds = sourceRect,
            progress = 0.5f,
            screenWidth = displayWidth,
            screenHeight = displayHeight
        )

        assertNotNull(revealRect)
        assertTrue(revealRect.left <= sourceRect.left)
        assertTrue(revealRect.top <= sourceRect.top)
        assertTrue(revealRect.right >= sourceRect.right)
        assertTrue(revealRect.bottom >= sourceRect.bottom)
    }
}

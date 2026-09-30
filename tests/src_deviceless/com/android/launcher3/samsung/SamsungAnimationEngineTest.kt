package com.android.launcher3.samsung

import android.graphics.Rect
import app.lawnchair.samsung.SamsungAnimationDiagnostics
import app.lawnchair.samsung.SamsungAnimationSpec
import app.lawnchair.samsung.SamsungLaunchAnimationController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungAnimationEngineTest {

    @Test
    fun testAnimationSpecConstants() {
        assertEquals(380L, SamsungAnimationSpec.LAUNCH_DURATION_MS)
        assertEquals(320L, SamsungAnimationSpec.RETURN_DURATION_MS)
        assertEquals(280L, SamsungAnimationSpec.RECENTS_DURATION_MS)
        assertEquals(320L, SamsungAnimationSpec.DURATION_OVERVIEW_SLIDE_IN_MS)
        assertEquals(280L, SamsungAnimationSpec.DURATION_OVERVIEW_EXIT_MS)
        assertEquals(0.92f, SamsungAnimationSpec.OVERVIEW_WORKSPACE_SCALE, 0.001f)
        assertEquals(0.40f, SamsungAnimationSpec.OVERVIEW_PARALLAX_FACTOR, 0.001f)
        assertEquals(300.0f, SamsungAnimationSpec.SPRING_STIFFNESS, 0.001f)
        assertEquals(0.86f, SamsungAnimationSpec.SPRING_DAMPING_RATIO, 0.001f)
    }

    @Test
    fun testWorkspaceScaleAndAlphaInterpolators() {
        val workspaceScaleInterpolator = SamsungAnimationSpec.WORKSPACE_SCALE_INTERPOLATOR
        assertNotNull(workspaceScaleInterpolator)
        assertEquals(0.0f, workspaceScaleInterpolator.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, workspaceScaleInterpolator.getInterpolation(1.0f), 0.001f)

        val alphaFadeInterpolator = SamsungAnimationSpec.ALPHA_FADE_INTERPOLATOR
        assertNotNull(alphaFadeInterpolator)
        assertEquals(0.0f, alphaFadeInterpolator.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, alphaFadeInterpolator.getInterpolation(1.0f), 0.001f)
    }

    @Test
    fun testLaunchInterpolatorBounds() {
        val launchInterpolator = SamsungAnimationSpec.LAUNCH_INTERPOLATOR
        assertNotNull(launchInterpolator)
        assertEquals(0.0f, launchInterpolator.getInterpolation(0.0f), 0.001f)
        assertEquals(1.0f, launchInterpolator.getInterpolation(1.0f), 0.001f)
        
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
            transitionType = "LAUNCH",
            targetInfo = "com.sec.android.app.camera"
        )

        SamsungAnimationDiagnostics.logFrameDrop(
            type = "LAUNCH",
            missedFrames = 2,
            frameDurationMs = 33.4f
        )

        SamsungAnimationDiagnostics.logTransitionEnd(
            transitionType = "LAUNCH",
            durationMs = 380L
        )

        val stats = SamsungAnimationDiagnostics.getPerformanceSummary()
        assertNotNull(stats)
        assertTrue((stats["totalTransitions"] as? Int ?: 0) >= 1)
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

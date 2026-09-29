# Samsung One UI 8 / Android 16 Technical Transition Research

## 1. System Components & Device Integration

Samsung One UI 8 represents a major evolution in Samsung's user experience stack on Android 16. The visual transition pipeline involves several key system components:

- **Stock Launcher:** `com.sec.android.app.launcher` (One UI Home)
- **System UI:** `com.android.systemui` (Samsung SystemUI extension with custom gesture navigation and recents providers)
- **Window Management & Shell Transitions:** `com.android.wm.shell` integrated into system server and SystemUI
- **Display Compositor:** `SurfaceFlinger` with Samsung-specific hardware overlay planes and dynamic refresh rate handling (60Hz / 90Hz / 120Hz LTPO)

---

## 2. Technical Analysis: Stock One UI Home vs. Third-Party Launchers

### 2.1 Privileged System Integration of One UI Home
One UI Home operates as a privileged system application with `signatureOrSystem` permissions, giving it direct access to:
- `android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS`
- Direct SurfaceControl leash acquisition during gesture navigation via binder calls
- Instantaneous input channel switching without transition completion timeouts
- Internal WindowContainer transaction locks

### 2.2 Third-Party Launcher Boundaries & Accessibility
For user-installed third-party launchers without root or QuickSwitch:
1. **Direct SurfaceControl Leash Sharing:** Restricted by Android framework security policies.
2. **GestureNavContract Protocol:** Exposed via system intents with `android.intent.extra.COMPONENT_NAME`, `android.intent.extra.USER`, and `android.view.SurfaceControl` handoff tokens.
3. **ClipReveal & Custom ActivityOptions:** Fully permitted through public and hidden `ActivityOptions` APIs (`makeClipRevealAnimation`, `makeCustomAnimation`, `setSplashScreenStyle`).
4. **Hardware Overlay Rendering:** Third-party launchers can spawn transparent hardware-accelerated `SurfaceView` / `SurfaceControl` layers to render intermediate motion states without depending on SystemUI callbacks.

---

## 3. Investigated Animation Pathways

### Path 1: App Launch Transition Optimization
- **Standard Behavior:** Third-party launch calls `ActivityOptions.makeClipRevealAnimation`. On One UI 8, this default reveals the app from a generic rectangle with standard AOSP easing, creating visual mismatch with Samsung's rounded corners.
- **Improved Samsung Path:**
  1. Accurately measure icon bounds in display space including icon drawable offsets.
  2. Compute target window aspect ratio and apply One UI 8 deceleration curve: `PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f)`.
  3. Pre-render launch ripple/expansion on a hardware-accelerated overlay surface.
  4. Synchronize wallpaper scale factor so the home screen wallpaper smoothly settles as the application surface takes focus.

### Path 2: Gesture Home Return & Icon Restoration
- **Root Cause of Icon Teleportation:** When SystemUI finishes a swipe-to-home gesture, it queries the launcher for the destination bounds. If the launcher fails to reply synchronously with accurate screen-space bounds and a valid hardware surface token, SystemUI falls back to centering the contracting task leash.
- **Samsung Specialized Solution:**
  1. Pre-cache screen coordinates for all items across workspace pages, hotseat, and all-apps containers.
  2. Implement `SamsungReturnAnimationController` that instantly resolves the target element upon `handleGestureContract`.
  3. Use double-buffered canvas rendering on a dedicated translucent `SurfaceView` with `setZOrderOnTop(true)`.
  4. Apply critically damped spring motion (`stiffness = 300.0f`, `dampingRatio = 0.86f`) to match Samsung's native icon bounce settling physics.

### Path 3: Predictive Back & Task Switching
- Integrate `OnBackAnimationCallback` with Android 16 predictive back APIs.
- Scale workspace drag layer proportionally during the swipe gesture with One UI 8 resistance curves.

---

## 4. Diagnostics & Logging
To facilitate real-time performance and behavior verification, a structured diagnostic logger (`SamsungAnimationDiagnostics`) records:
- Device model and detected One UI platform version.
- Animation path execution timestamps and frame durations.
- Target coordinate accuracy (difference between expected icon center and actual surface center).
- Fallback invocations and exception handling metrics.

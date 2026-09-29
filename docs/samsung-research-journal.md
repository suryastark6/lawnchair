# Samsung One UI 8 Transition & Animation Research Journal

## Research Entry 001: Baseline Environment & Architecture Mapping
- **Target Platform:** Samsung One UI 8 (Android 16 / API 36-37)
- **Baseline Source:** Lawnchair 16 Development Branch (`16-dev`, commit `494399a69904fb6f3d1201d2878a6c1bfb884d64`)
- **Focus Area:** Third-party launcher animation parity, gesture return synchronization, app-launch visual bridge, and SurfaceControl layer management without requiring root or QuickSwitch.

### 1. Problem Statement
On Samsung One UI 8 running Android 16, non-system launchers (third-party launchers installed by normal users) encounter distinct visual and behavioral friction points compared to the stock Samsung One UI Home (`com.sec.android.app.launcher`):
1. **App Launch Glitch / Flash:** Third-party launchers trigger standard window transitions (`TRANSIT_OPEN` / `makeClipRevealAnimation`) which exhibit a perceptible delay between the launcher icon press and the initial surface draw of the launching target, often causing a jarring scale/alpha pop.
2. **Gesture Home Return Teleportation:** Returning to home via swipe-up gestures or back navigation frequently returns to the center or top of the screen before jumping/teleporting to the originating icon's coordinates on the workspace.
3. **Interactivity Lockout Latency:** A 200–400ms gesture handoff freeze occurs where the launcher UI is visible on screen but touches are ignored until SystemUI/WindowManager finishes relinquishing the gesture leash.
4. **Task Close Bounds Mismatch:** When returning from an application, the closing window leash contracts toward fallback display coordinates rather than the exact bounding box of the active `BubbleTextView` or `FolderIcon`.

---

## Research Entry 002: Transition Call Graph & System Boundaries

### Path A: HOME -> TAP APP ICON -> APP LAUNCH
```
[User Touch on BubbleTextView / FolderIcon / Widget]
   │
   ▼
[LawnchairLauncher.onClick / onItemClicked]
   │
   ▼
[LawnchairLauncher.getActivityLaunchOptions(View, ItemInfo)]
   │
   ├── Generic Fallback: ActivityOptions.makeClipRevealAnimation(v, left, top, w, h)
   │
   └── Samsung Specialized Path:
         ├── Measure exact screen-space coordinates of icon via FloatingIconView / getLocationBoundsForView
         ├── Build SamsungLaunchAnimationController with continuous squircle morph curve
         ├── Prepare launcher surface concealment / wallpaper synchronization
         └── Pass configured ActivityOptionsWrapper with allowBGLaunch & SplashScreen icon metadata
   │
   ▼
[Context.startActivity(Intent, ActivityOptions.toBundle())]
   │
   ▼ (IPC: Binder to system_server / ActivityTaskManagerService)
[ActivityTaskManagerService.startActivityAsUser]
   │
   ▼
[WindowManagerService / WindowManager-Shell RemoteTransition / TransitionController]
   │
   ▼
[Target App Surface Created -> SurfaceFlinger composited]
```

### Path B: APP -> SWIPE HOME -> LAUNCHER WORKSPACE RESTORATION
```
[User performs swipe-up gesture from bottom navigation bar / gesture area]
   │
   ▼
[Samsung SystemUI Gesture Monitor / InputConsumer]
   │
   ▼ (Broadcast / Intent / GestureNavContract handoff)
[Intent: android.intent.action.MAIN + GestureNavContract extras]
   │
   ▼
[LawnchairLauncher.onNewIntent / handleGestureContract]
   │
   ▼
[SamsungReturnAnimationController / SamsungFloatingSurfaceView]
   │
   ├── Match target ComponentName & UserHandle against workspace model
   ├── Identify exact originating item: Workspace Page / Hotseat / AllApps / Folder
   ├── Calculate screen-space bounding box and target corner radius
   ├── Bind hardware-accelerated SurfaceView / SurfaceControl leash
   ├── Send end position & SurfaceControl to GestureNavContract callback
   ├── Execute synchronized Samsung motion curve (PathInterpolator + Damped Spring)
   └── Restore icon view visibility upon animation end without one-frame flicker
```

### Path C: APP -> BACK GESTURE / 3-BUTTON BACK -> LAUNCHER
```
[Back gesture triggered or 3-Button Back pressed]
   │
   ▼
[WindowManager / PredictiveBackHandler / OnBackPressedDispatcher]
   │
   ▼
[QuickstepTransitionManager / LauncherBackAnimationController]
   │
   ├── Determine if target is returning to Home
   ├── Configure PredictiveBack progress tracking
   └── Seamlessly scale dragLayer & reveal workspace elements
```

---

## Research Entry 003: Samsung One UI 8 Motion Curve Analysis

Samsung One UI 8 utilizes distinct Bezier easing curves and spring physics that deviate significantly from stock AOSP Material curves:

| Parameter | AOSP Material 3 | Samsung One UI 8 Spec |
|---|---|---|
| Launch Easing | `CubicBezier(0.2, 0.0, 0.0, 1.0)` | `PathInterpolator(0.22, 0.25, 0.0, 1.0)` |
| Launch Scale Duration | 350ms | 380ms – 420ms |
| Launch Alpha Curve | Linear 0ms–150ms | Rapid exponential ease-in 0ms–100ms |
| Return Easing | `CubicBezier(0.0, 0.0, 0.2, 1.0)` | `PathInterpolator(0.17, 0.17, 0.0, 1.0)` |
| Return Spring Stiffness | 200.0f | 280.0f – 320.0f |
| Return Spring Damping | 0.75f (underdamped bounce) | 0.86f (critically damped smooth settle) |
| Corner Radius Morph | Linear radius interpolation | Continuous squircle $n \approx 3.2$ polynomial |

---

## Research Entry 004: Engine Architecture & Capability Detection

To prevent regressions across other OEM devices (Pixel, OnePlus, Xiaomi, Motorola), the Samsung One UI 8 animation subsystem is encapsulated behind a capability-driven modular architecture:

```
                          Launcher / Quickstep
                                   │
                                   ▼
                       SamsungTransitionEngine
                                   │
      ┌────────────────────────────┼────────────────────────────┐
      ▼                            ▼                            ▼
SamsungTransitionCapabilities  SamsungFrameworkBridge   SamsungAnimationSpec
      │                            │                            │
      │ (Feature flags &           │ (Isolated reflection &     │ (One UI 8 curves,
      │  runtime checks)           │  hidden API bridges)       │  timings & springs)
      ▼                            ▼                            ▼
  Launch Controller            Return Controller            Gesture Controller
```

### Key Detection Probes:
1. `Build.MANUFACTURER` / `Build.BRAND` matching "samsung" (case-insensitive).
2. SemPlatform / One UI version resolution via system properties (`ro.build.version.oneui`, `ro.build.version.sep`).
3. Android 16 API level capability (`Build.VERSION.SDK_INT >= 36`).
4. SystemUI / Shell transition support and `GestureNavContract` availability.
5. Runtime reflection probe for Samsung WindowManager extensions without causing class loader crashes.

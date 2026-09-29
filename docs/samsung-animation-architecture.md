# Samsung One UI 8 Native Animation Architecture

## 1. System Overview

The Samsung One UI 8 Animation Architecture introduces a modular, capability-driven animation pipeline designed specifically for Samsung devices running Android 16. It ensures full compatibility with stock Samsung gesture navigation and window transitions while cleanly falling back to generic Android / AOSP implementations on other devices.

```
+-------------------------------------------------------------------------+
|                            LawnchairLauncher                            |
+------------------------------------+------------------------------------+
                                     |
                                     v
                       +----------------------------+
                       |   SamsungTransitionEngine  |
                       +--------------+-------------+
                                      |
         +----------------------------+----------------------------+
         |                            |                            |
         v                            v                            v
+------------------+         +------------------+         +------------------+
| SamsungLaunch-   |         | SamsungReturn-   |         | SamsungGesture-  |
| Controller       |         | Controller       |         | Controller       |
+--------+---------+         +--------+---------+         +--------+---------+
         |                            |                            |
         +----------------------------+----------------------------+
                                      |
                                      v
                       +----------------------------+
                       |   SamsungAnimationSpec     |
                       | - One UI 8 Easing Curves   |
                       | - Spring Physics Params    |
                       | - Corner Radius Morphing   |
                       +--------------+-------------+
                                      |
                                      v
                       +----------------------------+
                       |   SamsungFrameworkBridge   |
                       | - Safe Reflection Stubs    |
                       | - SurfaceControl Wrappers  |
                       | - Capability Detection     |
                       +----------------------------+
```

---

## 2. Core Architecture Modules

### 2.1 `SamsungTransitionCapabilities`
Detects device vendor, One UI version, available WindowManager extensions, and gesture navigation modes at runtime:
- `isSamsungDevice`: Evaluates `Build.MANUFACTURER` and `Build.BRAND`.
- `oneUiVersion`: Parses `ro.build.version.oneui` and `ro.build.version.sep`.
- `isOneUi8OrHigher`: Checks for One UI 8.0+ platform capabilities.
- `isGestureNavEnabled`: Queries navigation mode from system settings.
- `supportsSurfaceControl`: Verifies SurfaceControl API accessibility.

### 2.2 `SamsungAnimationSpec`
Defines motion primitives matching Samsung One UI 8 design specifications:
- **Launch Interpolator:** `PathInterpolator(0.22f, 0.25f, 0.0f, 1.0f)`
- **Return Deceleration Interpolator:** `PathInterpolator(0.17f, 0.17f, 0.0f, 1.0f)`
- **Content Scale Interpolator:** `PathInterpolator(0.20f, 0.0f, 0.0f, 1.0f)`
- **Spring Physics:**
  - `STIFFNESS_ONEUI_RETURN`: `300.0f`
  - `DAMPING_ONEUI_RETURN`: `0.86f`
  - `CORNER_RADIUS_DP`: `30.0f`

### 2.3 `SamsungLaunchAnimationController`
Orchestrates application launch transitions:
- Computes exact bounds of source view (`BubbleTextView`, `FolderIcon`, or `LauncherAppWidgetHostView`).
- Generates launch animation with continuous aspect-ratio morphing.
- Prepares wallpaper scale and dim transitions.
- Yields configured `ActivityOptionsWrapper` with optimized splash screen flags.

### 2.4 `SamsungReturnAnimationController`
Handles the return-to-home sequence via gestures or back actions:
- Resolves destination item on the active workspace or hotseat.
- Spawns hardware-accelerated translucent surface overlay.
- Synchronously transfers end position and SurfaceControl token to `GestureNavContract`.
- Animates workspace reveal and icon bounce with critically damped spring dynamics.

### 2.5 `SamsungFrameworkBridge`
Centralizes and protects all hidden API access and reflection calls:
- Encapsulates WindowManager and SystemUI internal hooks.
- Gracefully handles missing methods or security restrictions without throwing unhandled exceptions.
- Provides fallback standard implementations when proprietary interfaces are unavailable.

### 2.6 `SamsungAnimationDiagnostics`
Maintains in-memory and logcat telemetry for transition performance:
- Records transition start and end latency.
- Tracks frame drop / jank events.
- Reports active animation backend status for developer debugging.

---

## 3. Fallback & Safety Strategy

1. **Strict OEM Isolation:** On non-Samsung hardware (Google Pixel, OnePlus, Xiaomi, Nothing), the engine disables Samsung backends and routes all transitions to standard AOSP Launcher3 handlers.
2. **Graceful Degradation:** If an individual Samsung capability is denied (e.g., restricted permission), only that specific controller falls back to standard behavior while maintaining stability.
3. **No Unhandled Crashes:** All framework interactions are wrapped in structured `runCatching` blocks with diagnostic logging.

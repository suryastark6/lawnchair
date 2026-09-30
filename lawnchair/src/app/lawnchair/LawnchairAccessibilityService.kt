/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

import android.util.Log
import app.lawnchair.samsung.SamsungAnimationDiagnostics
import app.lawnchair.samsung.SamsungTransitionEngine

class LawnchairAccessibilityService : AccessibilityService() {

    private var lastPackageName: CharSequence? = null

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        lawnchairApp.accessibilityService = this
        Log.i(TAG, "LawnchairAccessibilityService connected for real-time window & gesture coordination")
    }

    override fun onDestroy() {
        lawnchairApp.accessibilityService = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val currentPackage = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val launcherPackage = packageName
                val isToLauncher = currentPackage == launcherPackage
                val isFromExternal = lastPackageName != null && lastPackageName != launcherPackage

                if (isToLauncher && isFromExternal) {
                    Log.d(TAG, "Real-time window transition to launcher detected from: $lastPackageName")
                    SamsungAnimationDiagnostics.logTransitionStart(
                        "A11Y_HOME_RETURN",
                        "From: $lastPackageName -> To: $currentPackage",
                    )
                    SamsungTransitionEngine.notifyHomeReturn(lastPackageName)
                }

                lastPackageName = currentPackage
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Window hierarchy change event for low-latency transition preparation
            }
        }
    }

    companion object {
        private const val TAG = "LawnchairA11yService"
    }
}

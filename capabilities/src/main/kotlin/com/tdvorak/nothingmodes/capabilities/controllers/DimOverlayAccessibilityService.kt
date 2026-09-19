package com.tdvorak.nothingmodes.capabilities.controllers

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Exists only to host the ultra-dim window as TYPE_ACCESSIBILITY_OVERLAY.
 * That window type is a trusted overlay: touches pass through unflagged, so
 * secure UIs (Google sign-in, permission dialogs, APK install prompts) keep
 * working while the screen is dimmed, and the API-31 obscured-opacity touch
 * cutoff does not apply. No accessibility events are consumed — the service
 * just stays bound so its WindowManager can create trusted windows.
 */
class DimOverlayAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        UltraDimController.attachTrustedHost(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        UltraDimController.detachTrustedHost(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        UltraDimController.detachTrustedHost(this)
        super.onDestroy()
    }
}

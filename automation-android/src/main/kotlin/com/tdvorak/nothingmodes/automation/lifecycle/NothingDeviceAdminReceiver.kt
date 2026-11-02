package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log

/**
 * DeviceAdminReceiver required for Action.LockScreen.
 * The user must activate device admin via the activation flow in Settings.
 *
 * While active it also receives lock-screen password failures — the only
 * edge the system reports to third parties — which feeds the Failed login
 * trigger. No explicit uses-policy is required to receive the callbacks.
 */
class NothingDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "Device admin enabled")
    }

    override fun onDisabled(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "Device admin disabled")
    }

    override fun onPasswordFailed(
        context: Context,
        intent: Intent,
        user: UserHandle,
    ) {
        context.startForegroundServiceSafely(
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_LOGIN_FAILED
            },
        )
    }

    companion object {
        private const val TAG = "NothingDeviceAdmin"
    }
}

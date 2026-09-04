package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * DeviceAdminReceiver required for Action.LockScreen.
 * The user must activate device admin via the activation flow in Settings.
 */
class NothingDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.d(TAG, "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d(TAG, "Device admin disabled")
    }

    companion object {
        private const val TAG = "NothingDeviceAdmin"
    }
}

package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives flip-to-glyph events from Nothing OS.
 *
 * On Nothing Phone (2) and later, the system sends a broadcast when the user
 * places the phone face-down on a surface. This receiver dispatches a
 * ScreenState OFF event to trigger "flip to glyph" automations.
 *
 * Manifest:
 * <receiver android:name=".automation.lifecycle.FlipReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.nothing.action.FLIP_TO_GLYPH"/>
 *     </intent-filter>
 * </receiver>
 */
class FlipReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FLIP_TO_GLYPH) return
        val isFlipped = intent.getBooleanExtra(EXTRA_FLIPPED, false)
        Log.d(TAG, "Flip state: flipped=$isFlipped")

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_SCREEN_STATE
            putExtra(DeviceStateReceiver.EXTRA_SCREEN_STATE, if (isFlipped) "OFF" else "ON")
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "FlipReceiver"
        const val ACTION_FLIP_TO_GLYPH = "com.nothing.action.FLIP_TO_GLYPH"
        const val EXTRA_FLIPPED = "flipped"
    }
}

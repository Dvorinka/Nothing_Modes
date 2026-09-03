package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tdvorak.nothingmodes.engine.model.Transition

/**
 * Receives geofence transition events from Google Play Services
 * and dispatches GeofenceTriggered events to AutomationService.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GEOFENCE_TRIGGERED) return

        val id = intent.getStringExtra(EXTRA_GEOFENCE_ID) ?: return
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        val transitionStr = intent.getStringExtra(EXTRA_TRANSITION) ?: return
        val transition = runCatching { Transition.valueOf(transitionStr) }.getOrNull() ?: return

        Log.d(TAG, "Geofence triggered: id=$id transition=$transition")

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_GEOFENCE
            putExtra(EXTRA_GEOFENCE_ID, id)
            putExtra(EXTRA_LAT, lat)
            putExtra(EXTRA_LNG, lng)
            putExtra(EXTRA_TRANSITION, transition.name)
        }
        context.startService(serviceIntent)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val ACTION_GEOFENCE_TRIGGERED = "com.tdvorak.nothingmodes.GEOFENCE_TRIGGERED"
        const val EXTRA_GEOFENCE_ID = "geofence_id"
        const val EXTRA_LAT = "geofence_lat"
        const val EXTRA_LNG = "geofence_lng"
        const val EXTRA_TRANSITION = "geofence_transition"
    }
}

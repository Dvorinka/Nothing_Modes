package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.tdvorak.nothingmodes.engine.model.Transition

/**
 * Receives geofence transition events from Google Play Services
 * and dispatches GeofenceTriggered events to AutomationService.
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GEOFENCE_TRIGGERED) return

        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.w(TAG, "Geofence broadcast without GeofencingEvent, ignoring")
            return
        }
        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${event.errorCode}")
            return
        }

        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> Transition.ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> Transition.EXIT
            Geofence.GEOFENCE_TRANSITION_DWELL -> Transition.DWELL
            else -> return
        }
        val location = event.triggeringLocation
        val lat = location?.latitude ?: 0.0
        val lng = location?.longitude ?: 0.0
        val geofenceIds = event.triggeringGeofences?.mapNotNull { it.requestId } ?: emptyList()

        if (geofenceIds.isEmpty()) {
            // Fall back to the baked-in ID so a single-geofence event still dispatches.
            intent.getStringExtra(EXTRA_GEOFENCE_ID)?.let { dispatch(context, it, lat, lng, transition) }
            return
        }
        geofenceIds.forEach { dispatch(context, it, lat, lng, transition) }
    }

    private fun dispatch(context: Context, geofenceId: String, lat: Double, lng: Double, transition: Transition) {
        Log.d(TAG, "Geofence triggered: id=$geofenceId transition=$transition")
        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_GEOFENCE
            putExtra(EXTRA_GEOFENCE_ID, geofenceId)
            putExtra(EXTRA_LAT, lat)
            putExtra(EXTRA_LNG, lng)
            putExtra(EXTRA_TRANSITION, transition.name)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
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

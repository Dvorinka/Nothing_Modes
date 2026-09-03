package com.tdvorak.nothingmodes.automation.lifecycle

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.tdvorak.nothingmodes.engine.model.Transition

/**
 * Monitors geofence triggers using Google Play Services Location API.
 *
 * Requires ACCESS_FINE_LOCATION permission.
 * Dispatches GeofenceTriggered events to AutomationService.
 *
 * Usage:
 *   val monitor = GeofenceMonitor(context)
 *   monitor.addGeofence("work", 50.0, 14.4, 150.0, Transition.ENTER)
 *   monitor.start()
 */
class GeofenceMonitor(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val pendingIntents = mutableMapOf<String, android.app.PendingIntent>()

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun addGeofence(
        id: String,
        lat: Double,
        lng: Double,
        radiusM: Float,
        transition: Transition,
    ) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission, skipping geofence $id")
            return
        }

        val geofenceTransition = when (transition) {
            Transition.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
            Transition.EXIT -> Geofence.GEOFENCE_TRANSITION_EXIT
            Transition.DWELL -> Geofence.GEOFENCE_TRANSITION_DWELL
        }

        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(lat, lng, radiusM)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(geofenceTransition)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = GeofenceReceiver.ACTION_GEOFENCE_TRIGGERED
            putExtra(GeofenceReceiver.EXTRA_GEOFENCE_ID, id)
            putExtra(GeofenceReceiver.EXTRA_LAT, lat)
            putExtra(GeofenceReceiver.EXTRA_LNG, lng)
            putExtra(GeofenceReceiver.EXTRA_TRANSITION, transition.name)
        }

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            geofencingClient.addGeofences(request, pendingIntent)
            pendingIntents[id] = pendingIntent
            Log.i(TAG, "Geofence added: $id at ($lat, $lng) radius=${radiusM}m")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to add geofence $id", e)
        }
    }

    fun removeGeofence(id: String) {
        pendingIntents[id]?.let { pi ->
            try {
                geofencingClient.removeGeofences(pi)
                Log.i(TAG, "Geofence removed: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove geofence $id", e)
            }
        }
        pendingIntents.remove(id)
    }

    fun removeAll() {
        pendingIntents.keys.toList().forEach { removeGeofence(it) }
    }

    companion object {
        private const val TAG = "GeofenceMonitor"
    }
}

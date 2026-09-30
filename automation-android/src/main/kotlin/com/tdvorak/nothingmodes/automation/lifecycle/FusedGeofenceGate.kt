package com.tdvorak.nothingmodes.automation.lifecycle

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tdvorak.nothingmodes.engine.runtime.GeofenceGate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Live position check for gated geofence ends. Tries a fresh balanced fix,
 * falls back to last-known. Returns null when location is unavailable or
 * permission is missing — callers treat null as "gate unmet", so an
 * unreliable fix defers the end instead of guessing.
 */
class FusedGeofenceGate(
    context: Context,
) : GeofenceGate {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun isInside(
        lat: Double,
        lng: Double,
        radiusM: Double,
    ): Boolean? {
        if (!hasPermission()) return null
        val location = currentLocation() ?: lastLocation() ?: return null
        val distance =
            FloatArray(1).also {
                Location.distanceBetween(location.latitude, location.longitude, lat, lng, it)
            }[0]
        // Fold fix accuracy into the radius: a borderline reading counts as
        // inside so an unreliable fix can't end a mode on a jitter edge.
        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
        return distance - accuracy <= radiusM.toFloat()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? =
        runCatching {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                client
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { location: Location? -> cont.resume(location) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }.onFailure { Log.w(TAG, "currentLocation failed", it) }
            .getOrNull()

    @SuppressLint("MissingPermission")
    private suspend fun lastLocation(): Location? =
        runCatching {
            suspendCancellableCoroutine { cont ->
                client.lastLocation
                    .addOnSuccessListener { location: Location? -> cont.resume(location) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }.onFailure { Log.w(TAG, "lastLocation failed", it) }
            .getOrNull()

    companion object {
        private const val TAG = "FusedGeofenceGate"
    }
}

package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Starts a foreground service without crashing when the app holds no
 * FGS-start exemption. On Android 12+ the platform throws
 * ForegroundServiceStartNotAllowedException whenever mAllowStartForeground
 * is false — a state any receiver or monitor callback can run in once no
 * service is currently foreground (PersistentMonitorService between
 * onCreate and startForeground, or a cached process woken by a broadcast).
 *
 * On rejection the event is dropped; the next broadcast edge, alarm or app
 * launch re-dispatches. Returns true when the platform accepted the start,
 * so callers that persist side effects can gate them on real delivery.
 */
fun Context.startForegroundServiceSafely(intent: Intent): Boolean =
    runCatching { ContextCompat.startForegroundService(this, intent) }
        .onFailure { Log.w(TAG, "FGS start rejected for ${intent.action}", it) }
        .isSuccess

private const val TAG = "FgsStart"

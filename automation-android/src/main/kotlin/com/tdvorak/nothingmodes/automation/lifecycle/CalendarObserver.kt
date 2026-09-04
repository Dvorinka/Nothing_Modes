package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.engine.model.CalendarDirection
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Observes the calendar provider for event changes and dispatches
 * CalendarEventChanged trigger events.
 *
 * Uses a polling approach: when the calendar provider notifies of changes,
 * queries upcoming events and fires triggers for events that are starting
 * or ending within a small window.
 *
 * Requires READ_CALENDAR permission.
 */
class CalendarObserver(
    private val context: Context,
    private val onEvent: (TriggerEvent.CalendarEventChanged) -> Unit,
) {
    private val thread = HandlerThread("calendar-observer").also { it.start() }
    private val handler = Handler(thread.looper)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var lastFireTime = 0L

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.d(TAG, "Calendar content changed")
            scope.launch { checkEvents() }
        }
    }

    fun start() {
        runCatching {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer,
            )
        }.onFailure { e ->
            if (e is SecurityException) {
                Log.w(TAG, "READ_CALENDAR permission not granted, observer inactive")
            } else {
                Log.e(TAG, "Failed to register calendar observer", e)
            }
        }
        // Also do an initial check (will silently skip if no permission)
        scope.launch { checkEvents() }
    }

    fun stop() {
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
    }

    private suspend fun checkEvents() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            // Debounce: don't check more than once per 30 seconds
            if (now - lastFireTime < 30_000) return
            lastFireTime = now

            val windowStart = now
            val windowEnd = now + 60_000 // Check events starting in the next minute

            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.CALENDAR_ID,
            )

            val selection = "(? BETWEEN ${CalendarContract.Events.DTSTART} AND ${CalendarContract.Events.DTEND}) " +
                "OR (${CalendarContract.Events.DTSTART} BETWEEN ? AND ?)"
            val selectionArgs = arrayOf(now.toString(), windowStart.toString(), windowEnd.toString())

            try {
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC",
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(0) ?: continue
                        val dtStart = cursor.getLong(1)
                        val dtEnd = cursor.getLong(2)
                        val calendarId = cursor.getString(3)

                        // Event starting now (within 60s window)
                        if (dtStart in windowStart..windowEnd) {
                            Log.d(TAG, "Calendar event starting: $title")
                            onEvent(TriggerEvent.CalendarEventChanged(
                                eventId = "cal_start:${dtStart}:$title",
                                direction = CalendarDirection.START,
                                title = title,
                                calendarId = calendarId,
                            ))
                        }

                        // Event ending now (within 60s window)
                        if (dtEnd in windowStart..windowEnd) {
                            Log.d(TAG, "Calendar event ending: $title")
                            onEvent(TriggerEvent.CalendarEventChanged(
                                eventId = "cal_end:${dtEnd}:$title",
                                direction = CalendarDirection.END,
                                title = title,
                                calendarId = calendarId,
                            ))
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "READ_CALENDAR permission not granted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query calendar events", e)
            }
        }
    }

    companion object {
        private const val TAG = "CalendarObserver"
    }
}

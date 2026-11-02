package com.tdvorak.nothingmodes.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log

private const val TAG = "NothingModesDB"

/** Rows older than this in append-only log tables are pruned on each write. */
const val LOG_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

/**
 * Bounded-write guard for append-only tables (SQLiteFullException, code 13).
 *
 * These tables grow without bound; once the disk fills, Room throws inside
 * endTransaction on a worker thread and crashes the app. The guard prunes old
 * rows before each write, retries once after a SQLITE_FULL failure (the prune
 * may have freed pages), and degrades to null when storage stays full — a
 * dropped log row must never crash a background worker.
 */
suspend fun <T> guardedAppend(
    prune: suspend () -> Int,
    write: suspend () -> T,
): T? {
    try {
        runCatching { prune() }
        return write()
    } catch (e: SQLiteFullException) {
        Log.w(TAG, "SQLite full on write; pruning and retrying once", e)
        runCatching { prune() }
        return try {
            write()
        } catch (retry: SQLiteException) {
            Log.w(TAG, "SQLite write still failing after prune; dropping row", retry)
            null
        }
    }
}

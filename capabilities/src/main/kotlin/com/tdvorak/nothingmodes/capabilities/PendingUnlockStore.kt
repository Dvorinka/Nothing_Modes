package com.tdvorak.nothingmodes.capabilities

import android.content.Context
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.EngineJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent queue for actions that need to show UI but fired while the
 * keyguard was locked (settings panels, launch-app, open-URL). Without
 * Shizuku those fall back to `startActivity`, which the system drops when
 * the app is backgrounded on a locked screen.
 *
 * Entries survive process death so a queued panel is never lost — the queue
 * is drained when the device unlocks (directly by the automation service, or
 * by the notification tap receiver).
 */
class PendingUnlockStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class PendingAction(
        val automationId: AutomationId,
        val action: Action,
        val queuedAtMillis: Long,
    )

    /** Append an action to the queue. Entries older than [MAX_AGE_MS] are dropped. */
    fun enqueue(
        automationId: AutomationId,
        action: Action,
    ) {
        synchronized(LOCK) {
            val queue = readQueue()
            queue.put(
                JSONObject()
                    .put(KEY_AUTOMATION_ID, automationId.value)
                    .put(KEY_ACTION, EngineJson.json.encodeToString(Action.serializer(), action))
                    .put(KEY_QUEUED_AT, System.currentTimeMillis()),
            )
            // Cap the queue so a broken mode can't grow it forever.
            while (queue.length() > MAX_ENTRIES) queue.remove(0)
            prefs.edit().putString(KEY_QUEUE, queue.toString()).apply()
        }
    }

    /** Read all pending entries without removing them. */
    fun pending(): List<PendingAction> =
        synchronized(LOCK) {
            decode(readQueue())
        }

    /** Read all pending entries and clear the queue atomically. */
    fun drain(): List<PendingAction> =
        synchronized(LOCK) {
            val entries = decode(readQueue())
            if (entries.isNotEmpty()) prefs.edit().remove(KEY_QUEUE).apply()
            entries
        }

    fun hasPending(): Boolean = pending().isNotEmpty()

    fun clear() {
        synchronized(LOCK) {
            prefs.edit().remove(KEY_QUEUE).apply()
        }
    }

    private fun readQueue(): JSONArray {
        val raw = prefs.getString(KEY_QUEUE, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            // Corrupted payload — drop it rather than crashing every unlock.
            prefs.edit().remove(KEY_QUEUE).apply()
            JSONArray()
        }
    }

    private fun decode(queue: JSONArray): List<PendingAction> {
        val now = System.currentTimeMillis()
        val entries = mutableListOf<PendingAction>()
        for (i in 0 until queue.length()) {
            val obj = queue.optJSONObject(i) ?: continue
            val queuedAt = obj.optLong(KEY_QUEUED_AT, 0L)
            if (queuedAt <= 0 || now - queuedAt > MAX_AGE_MS) continue
            val automationId = obj.optString(KEY_AUTOMATION_ID).takeIf { it.isNotBlank() } ?: continue
            val actionJson = obj.optString(KEY_ACTION).takeIf { it.isNotBlank() } ?: continue
            val action =
                runCatching {
                    EngineJson.json.decodeFromString(Action.serializer(), actionJson)
                }.getOrNull() ?: continue
            entries += PendingAction(AutomationId(automationId), action, queuedAt)
        }
        return entries
    }

    companion object {
        private const val PREFS = "pending_unlock_actions"
        private const val KEY_QUEUE = "queue"
        private const val KEY_AUTOMATION_ID = "automation_id"
        private const val KEY_ACTION = "action"
        private const val KEY_QUEUED_AT = "queued_at"
        private const val MAX_ENTRIES = 50
        private const val MAX_AGE_MS = 24 * 60 * 60 * 1000L
        private val LOCK = Any()
    }
}

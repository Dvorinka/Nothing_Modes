package com.tdvorak.nothingmodes.automation.pending

import com.tdvorak.nothingmodes.capabilities.PendingUnlockStore
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.FireContext

/** Replays queued UI actions after unlock — shared by the service's direct
 *  drain and the notification-tap broadcast. */
object PendingUnlockDrain {
    suspend fun run(
        store: PendingUnlockStore,
        executor: ActionExecutor,
    ) {
        store.drain().forEach { entry ->
            runCatching {
                executor.execute(
                    entry.action,
                    FireContext(
                        eventId = "unlock:${System.currentTimeMillis()}",
                        executionId = "unlock:${entry.automationId.value}:${entry.queuedAtMillis}",
                        automationId = entry.automationId,
                        actionIndex = -1,
                        priority = 0,
                    ),
                )
            }
        }
    }
}

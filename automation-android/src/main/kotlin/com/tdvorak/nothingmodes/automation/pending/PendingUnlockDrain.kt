package com.tdvorak.nothingmodes.automation.pending

import com.tdvorak.nothingmodes.capabilities.PendingUnlockStore
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.AuditEvent
import com.tdvorak.nothingmodes.engine.runtime.AuditKind
import com.tdvorak.nothingmodes.engine.runtime.AuditSink
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import com.tdvorak.nothingmodes.engine.runtime.NoopAuditSink
import com.tdvorak.nothingmodes.engine.runtime.failureLabel
import kotlinx.coroutines.CancellationException

/** Replays queued UI actions after unlock — shared by the service's direct
 *  drain and the notification-tap broadcast. Also used for the locked-write
 *  queue: settings written under the keyguard are reverted by the platform
 *  at unlock, so they are replayed here to win the race. */
object PendingUnlockDrain {
    suspend fun run(
        store: PendingUnlockStore,
        executor: ActionExecutor,
        audit: AuditSink = NoopAuditSink,
    ) {
        store.drain().forEach { entry ->
            val ctx =
                FireContext(
                    eventId = "unlock:${System.currentTimeMillis()}",
                    executionId = "unlock:${entry.automationId.value}:${entry.queuedAtMillis}",
                    automationId = entry.automationId,
                    actionIndex = -1,
                    priority = 0,
                )
            val result =
                try {
                    executor.execute(entry.action, ctx)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ActionResult.Failure(e::class.simpleName ?: "drain_exception")
                }
            result.failureLabel(entry.action)?.let { label ->
                runCatching {
                    audit.record(
                        AuditEvent(
                            automationId = entry.automationId,
                            kind = AuditKind.ACTION_FAILED,
                            atMillis = System.currentTimeMillis(),
                            detail = "unlock replay: $label",
                            eventId = ctx.eventId,
                            executionId = ctx.executionId,
                        ),
                    )
                }
            }
        }
    }
}

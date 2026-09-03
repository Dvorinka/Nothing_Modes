package com.dvoranka.nothingmodes.engine.runtime

import com.dvoranka.nothingmodes.engine.model.Action
import com.dvoranka.nothingmodes.engine.model.Automation
import com.dvoranka.nothingmodes.engine.model.AutomationId
import com.dvoranka.nothingmodes.engine.model.AutomationStatus
import com.dvoranka.nothingmodes.engine.model.AutomationType

/** Store interface for automation persistence. */
interface AutomationStore {
    suspend fun get(id: AutomationId): Automation?
    suspend fun armed(): List<Automation>
    suspend fun save(automation: Automation)
    suspend fun delete(id: AutomationId)
    suspend fun all(): List<Automation>
}

/** In-memory store for testing. */
class InMemoryAutomationStore : AutomationStore {
    private val map = linkedMapOf<String, Automation>()

    override suspend fun get(id: AutomationId): Automation? = map[id.value]
    override suspend fun armed(): List<Automation> = map.values.filter {
        it.status == AutomationStatus.ARMED && it.enabled
    }
    override suspend fun save(automation: Automation) { map[automation.id.value] = automation }
    override suspend fun delete(id: AutomationId) { map.remove(id.value) }
    override suspend fun all(): List<Automation> = map.values.toList()
}

/** Audit event for the execution journal. */
data class AuditEvent(
    val automationId: AutomationId,
    val kind: AuditKind,
    val atMillis: Long,
    val detail: String = "",
    val eventId: String = "",
    val executionId: String? = null,
)

enum class AuditKind {
    FIRED, BLOCKED_POLICY, CONDITIONS_NOT_MET,
    SUPPRESSED_COOLDOWN, SUPPRESSED_DUPLICATE, ERROR,
    MODE_ACTIVATED, MODE_DEACTIVATED, RULE_NEEDS_REVIEW,
}

fun interface AuditSink {
    suspend fun record(event: AuditEvent)
}

object NoopAuditSink : AuditSink {
    override suspend fun record(event: AuditEvent) = Unit
}

/** Execution journal entry. */
data class ExecutionCompletion(
    val executionId: String,
    val automationId: AutomationId,
    val atMillis: Long,
    val status: ExecutionStatus,
    val actionCount: Int,
)

enum class ExecutionStatus { COMPLETED, FAILED, CANCELLED, SUPPRESSED_COOLDOWN, SUPPRESSED_NOT_ELIGIBLE }

fun interface ExecutionJournal {
    suspend fun finish(completion: ExecutionCompletion)
}

object NoopExecutionJournal : ExecutionJournal {
    override suspend fun finish(completion: ExecutionCompletion) = Unit
}

/** Fire policy: cooldown and duplicate suppression. */
class FirePolicy {

    sealed interface Decision {
        data object Allow : Decision
        data class Block(val code: String, val needsReview: Boolean = false) : Decision
    }

    private val lastFired = mutableMapOf<AutomationId, Long>()

    fun evaluate(automation: Automation, event: TriggerEvent, now: Long): Decision {
        if (automation.cooldownMs <= 0) return Decision.Allow
        val last = lastFired[automation.id]
        if (last != null && now - last < automation.cooldownMs) {
            return Decision.Block("cooldown_active")
        }
        lastFired[automation.id] = now
        return Decision.Allow
    }

    fun reset(id: AutomationId) { lastFired.remove(id) }
}

/** Execution ID factory. */
fun interface ExecutionIdFactory {
    fun create(automationId: AutomationId, eventId: String): String
}

object StableExecutionIdFactory : ExecutionIdFactory {
    override fun create(automationId: AutomationId, eventId: String): String =
        "${automationId.value}:${eventId}"
}

/** Fire outcome for a single automation. */
data class FireOutcome(
    val automation: Automation,
    val actions: List<Action>,
    val results: List<ActionResult>,
    val eventId: String,
    val executionId: String,
)

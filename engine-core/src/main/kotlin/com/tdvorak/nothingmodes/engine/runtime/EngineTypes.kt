package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import kotlinx.serialization.Serializable

/** Typed wrapper for an execution identifier. */
@JvmInline
@Serializable
value class ExecutionId(val value: String)

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

/** Provides device state for condition evaluation. */
fun interface StateProvider {
    suspend fun read(): DeviceState
}

object NoopStateProvider : StateProvider {
    override suspend fun read(): DeviceState = DeviceState()
}

/** Snapshot of a single setting value for later restoration. */
data class StateSnapshot(
    val automationId: AutomationId,
    val settingKey: String,
    val previousValue: String,
    val capturedAtMillis: Long,
    val namespace: String = "system",
)

/** Stores and retrieves state snapshots for mode restoration. */
interface StateSnapshotStore {
    suspend fun save(snapshot: StateSnapshot)
    suspend fun forAutomation(id: AutomationId): List<StateSnapshot>
    suspend fun deleteForAutomation(id: AutomationId)
}

object NoopStateSnapshotStore : StateSnapshotStore {
    override suspend fun save(snapshot: StateSnapshot) = Unit
    override suspend fun forAutomation(id: AutomationId): List<StateSnapshot> = emptyList()
    override suspend fun deleteForAutomation(id: AutomationId) = Unit
}

/** Reads a single setting value for snapshotting (before mode activation). */
fun interface SettingReader {
    suspend fun read(key: String): String?
}

object NoopSettingReader : SettingReader {
    override suspend fun read(key: String): String? = null
}

/** Fire policy: cooldown and duplicate suppression. Thread-safe. */
class FirePolicy {

    sealed interface Decision {
        data object Allow : Decision
        data class Block(val code: String, val needsReview: Boolean = false) : Decision
    }

    private val lastFired = java.util.concurrent.ConcurrentHashMap<AutomationId, Long>()
    private val lock = Any()

    fun evaluate(automation: Automation, event: TriggerEvent, now: Long): Decision {
        if (automation.cooldownMs <= 0) return Decision.Allow
        synchronized(lock) {
            val last = lastFired[automation.id]
            if (last != null && now - last < automation.cooldownMs) {
                return Decision.Block("cooldown_active")
            }
            lastFired[automation.id] = now
        }
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

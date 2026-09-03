package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.affectedSettings
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import kotlinx.coroutines.CancellationException

/** Core engine: matches triggers, evaluates conditions, executes actions. */
class Engine(
    private val store: AutomationStore,
    private val executor: ActionExecutor,
    private val evaluator: ConditionEvaluator = ConditionEvaluator(),
    private val matcher: TriggerMatcher = TriggerMatcher(),
    private val firePolicy: FirePolicy = FirePolicy(),
    private val audit: AuditSink = NoopAuditSink,
    private val journal: ExecutionJournal = NoopExecutionJournal,
    private val stateProvider: StateProvider = NoopStateProvider,
    private val snapshotStore: StateSnapshotStore = NoopStateSnapshotStore,
    private val settingReader: SettingReader = NoopSettingReader,
    private val executionIds: ExecutionIdFactory = StableExecutionIdFactory,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun onTrigger(envelope: TriggerEnvelope): List<FireOutcome> {
        val event = envelope.event
        val candidates = when (event) {
            is TriggerEvent.Registered -> listOfNotNull(store.get(event.automationId))
            is TriggerEvent.TimeFired -> listOfNotNull(store.get(event.automationId))
            is TriggerEvent.ModeWindowStart -> listOfNotNull(store.get(event.automationId))
            is TriggerEvent.ModeWindowEnd -> listOfNotNull(store.get(event.automationId))
            else -> store.armed()
        }.filter { it.status == AutomationStatus.ARMED && it.enabled }
            .sortedWith(compareBy({ it.priority }, { it.id.value }))

        val batchNow = now()
        val outcomes = mutableListOf<FireOutcome>()

        for (automation in candidates) {
            val executionId = executionIds.create(automation.id, envelope.id)
            val actionResults = mutableListOf<ActionResult>()

            try {
                if (!matcher.matches(automation.trigger, event)) continue

                when (val decision = firePolicy.evaluate(automation, event, batchNow)) {
                    FirePolicy.Decision.Allow -> Unit
                    is FirePolicy.Decision.Block -> {
                        audit.record(AuditEvent(
                            automationId = automation.id,
                            kind = AuditKind.SUPPRESSED_COOLDOWN,
                            atMillis = batchNow,
                            detail = decision.code,
                            eventId = envelope.id,
                        ))
                        continue
                    }
                }

                if (automation.conditions != null) {
                    val state = runCatching { stateProvider.read() }.getOrDefault(DeviceState(now = batchNow))
                    when (evaluator.result(automation.conditions, state)) {
                        ConditionEvaluator.Result.MET -> Unit
                        ConditionEvaluator.Result.NOT_MET,
                        ConditionEvaluator.Result.STATE_UNAVAILABLE -> {
                            audit.record(AuditEvent(
                                automationId = automation.id,
                                kind = AuditKind.CONDITIONS_NOT_MET,
                                atMillis = batchNow,
                                eventId = envelope.id,
                            ))
                            continue
                        }
                    }
                }

                // Mode window-end: restore snapshots before executing actions
                if (event is TriggerEvent.ModeWindowEnd && automation.type == AutomationType.MODE) {
                    restoreSnapshots(automation.id, batchNow)
                }

                // Mode window-start: snapshot affected settings before executing
                if (event is TriggerEvent.ModeWindowStart && automation.type == AutomationType.MODE) {
                    snapshotSettings(automation, batchNow)
                }

                automation.actions.forEachIndexed { index, action ->
                    val context = FireContext(
                        eventId = envelope.id,
                        executionId = executionId,
                        automationId = automation.id,
                        actionIndex = index,
                        priority = automation.priority,
                    )
                    val result = try {
                        executor.execute(action, context)
                    } catch (_: CancellationException) {
                        throw CancellationException()
                    } catch (_: Exception) {
                        ActionResult.Failure("executor_exception")
                    }
                    actionResults += result
                }

                journal.finish(ExecutionCompletion(
                    executionId = executionId,
                    automationId = automation.id,
                    atMillis = batchNow,
                    status = if (actionResults.all { it is ActionResult.Success }) ExecutionStatus.COMPLETED else ExecutionStatus.FAILED,
                    actionCount = actionResults.size,
                ))

                audit.record(AuditEvent(
                    automationId = automation.id,
                    kind = if (event is TriggerEvent.ModeWindowEnd && automation.type == AutomationType.MODE)
                        AuditKind.MODE_DEACTIVATED
                    else if (automation.type == AutomationType.MODE) AuditKind.MODE_ACTIVATED
                    else AuditKind.FIRED,
                    atMillis = batchNow,
                    eventId = envelope.id,
                    executionId = executionId,
                ))

                outcomes += FireOutcome(automation, automation.actions, actionResults.toList(), envelope.id, executionId)
            } catch (e: CancellationException) {
                journal.finish(ExecutionCompletion(
                    executionId = executionId,
                    automationId = automation.id,
                    atMillis = batchNow,
                    status = ExecutionStatus.CANCELLED,
                    actionCount = actionResults.size,
                ))
                throw e
            } catch (e: Exception) {
                journal.finish(ExecutionCompletion(
                    executionId = executionId,
                    automationId = automation.id,
                    atMillis = batchNow,
                    status = ExecutionStatus.FAILED,
                    actionCount = actionResults.size,
                ))
                audit.record(AuditEvent(
                    automationId = automation.id,
                    kind = AuditKind.ERROR,
                    atMillis = batchNow,
                    detail = e::class.simpleName ?: "error",
                    eventId = envelope.id,
                    executionId = executionId,
                ))
            }
        }
        return outcomes
    }

    private suspend fun snapshotSettings(automation: Automation, batchNow: Long) {
        val keys = automation.actions.filter { it.supportsRestore }.flatMap { it.affectedSettings }.toSet()
        for (key in keys) {
            val value = runCatching { settingReader.read(key) }.getOrNull() ?: continue
            snapshotStore.save(StateSnapshot(
                automationId = automation.id,
                settingKey = key,
                previousValue = value,
                capturedAtMillis = batchNow,
            ))
        }
    }

    private suspend fun restoreSnapshots(id: AutomationId, batchNow: Long) {
        val snapshots = snapshotStore.forAutomation(id)
        for (snapshot in snapshots) {
            val restoreAction = Action.WriteSetting(
                namespace = com.tdvorak.nothingmodes.engine.model.SettingNamespace.SYSTEM,
                key = snapshot.settingKey,
                value = snapshot.previousValue,
            )
            val context = FireContext(
                eventId = "restore:${id.value}",
                executionId = "restore:${id.value}:$batchNow",
                automationId = id,
                actionIndex = -1,
                priority = 100,
            )
            runCatching { executor.execute(restoreAction, context) }
        }
        snapshotStore.deleteForAutomation(id)
    }
}

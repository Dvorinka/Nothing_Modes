package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.affectedSettings
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
    private val modeActivationSink: ModeActivationSink = NoopModeActivationSink,
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
            .sortedWith(compareByDescending<Automation> { it.priority }.thenBy { it.id.value })

        val batchNow = now()
        val outcomes = mutableListOf<FireOutcome>()

        for (automation in candidates) {
            val executionId = executionIds.create(automation.id, envelope.id)
            val actionResults = mutableListOf<ActionResult>()

            try {
                if (event is TriggerEvent.ManualFired && event.automationId != automation.id) continue
                if (!matcher.matches(automation.trigger, event)) continue

                // Check day-of-week filter for time-based triggers
                if (!shouldFireOnDay(automation.trigger, batchNow)) continue

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

                val isModeActivation = automation.type == AutomationType.MODE &&
                    event !is TriggerEvent.ModeWindowEnd
                val isModeDeactivation = event is TriggerEvent.ModeWindowEnd &&
                    automation.type == AutomationType.MODE

                if (isModeActivation) {
                    modeActivationSink.activate(automation.id, batchNow)
                } else if (isModeDeactivation) {
                    modeActivationSink.deactivate(automation.id, batchNow)
                }

                audit.record(AuditEvent(
                    automationId = automation.id,
                    kind = if (isModeDeactivation)
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
            } catch (e: StackOverflowError) {
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
                    detail = "stack_overflow: condition nesting too deep",
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
                namespace = namespaceForKey(key),
            ))
        }
    }

    private suspend fun restoreSnapshots(id: AutomationId, batchNow: Long) {
        val snapshots = snapshotStore.forAutomation(id)
        // Deduplicate by setting key, keeping the newest snapshot per key
        val latestByKey = snapshots.associateBy { it.settingKey }
        for (snapshot in latestByKey.values) {
            val ns = when (snapshot.namespace) {
                "secure" -> com.tdvorak.nothingmodes.engine.model.SettingNamespace.SECURE
                "global" -> com.tdvorak.nothingmodes.engine.model.SettingNamespace.GLOBAL
                else -> com.tdvorak.nothingmodes.engine.model.SettingNamespace.SYSTEM
            }
            val restoreAction = Action.WriteSetting(
                namespace = ns,
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

    /** Maps a setting key to its Android Settings namespace. */
    private fun namespaceForKey(key: String): String = when (key) {
        "reduce_bright_colors_activated" -> "secure"
        "airplane_mode_on", "low_power", "data_saver" -> "global"
        else -> "system"
    }

    /** Checks if a time-based trigger should fire on the current day. */
    private fun shouldFireOnDay(trigger: Trigger, nowMillis: Long): Boolean {
        val days = when (trigger) {
            is Trigger.Time -> trigger.days
            is Trigger.TimeWindow -> trigger.days
            else -> return true
        } ?: return true
        if (days.isEmpty()) return true
        val tz = when (trigger) {
            is Trigger.Time -> trigger.tz
            is Trigger.TimeWindow -> trigger.tz
            else -> return true
        }
        val zone = runCatching { ZoneId.of(tz) }.getOrNull() ?: return true
        val javaDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone).dayOfWeek
        val engineDay = when (javaDay) {
            java.time.DayOfWeek.MONDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> com.tdvorak.nothingmodes.engine.model.DayOfWeek.SUNDAY
        }
        return engineDay in days
    }
}

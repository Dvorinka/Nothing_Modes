package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AodMode
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.affectedSettings
import com.tdvorak.nothingmodes.engine.model.isGlyphAction
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
        val candidates =
            when (event) {
                is TriggerEvent.Registered -> listOfNotNull(store.get(event.automationId))
                is TriggerEvent.TimeFired -> listOfNotNull(store.get(event.automationId))
                is TriggerEvent.ModeWindowStart -> listOfNotNull(store.get(event.automationId))
                is TriggerEvent.ModeWindowEnd -> listOfNotNull(store.get(event.automationId))
                // Geofence events carry the registering automation's ID, so only that
                // automation is a candidate instead of every armed geofence automation.
                is TriggerEvent.GeofenceTriggered ->
                    event.geofenceId
                        ?.let { listOfNotNull(store.get(AutomationId(it))) }
                        ?: store.armed()
                else -> store.armed()
            }.filter { it.status == AutomationStatus.ARMED && it.enabled }
                .sortedWith(compareByDescending<Automation> { it.priority }.thenBy { it.id.value })

        val batchNow = now()
        val outcomes = mutableListOf<FireOutcome>()
        val claimedSettings = mutableSetOf<String>()

        for (automation in candidates) {
            val executionId = executionIds.create(automation.id, envelope.id)
            val actionResults = mutableListOf<ActionResult>()
            val startedAtNano = System.nanoTime()

            try {
                if (event is TriggerEvent.ManualFired && event.automationId != automation.id) continue
                if (!matcher.matches(automation.trigger, event)) continue

                // Check day-of-week filter for time-based triggers
                if (!shouldFireOnDay(automation.trigger, batchNow)) continue

                when (val decision = firePolicy.evaluate(automation, event, batchNow)) {
                    FirePolicy.Decision.Allow -> Unit
                    is FirePolicy.Decision.Block -> {
                        audit.record(
                            AuditEvent(
                                automationId = automation.id,
                                kind = AuditKind.SUPPRESSED_COOLDOWN,
                                atMillis = batchNow,
                                detail = decision.code,
                                eventId = envelope.id,
                            ),
                        )
                        continue
                    }
                }

                if (automation.conditions != null) {
                    val state = runCatching { stateProvider.read() }.getOrDefault(DeviceState(now = batchNow))
                    when (evaluator.result(automation.conditions, state)) {
                        ConditionEvaluator.Result.MET -> Unit
                        ConditionEvaluator.Result.NOT_MET,
                        ConditionEvaluator.Result.STATE_UNAVAILABLE,
                        -> {
                            audit.record(
                                AuditEvent(
                                    automationId = automation.id,
                                    kind = AuditKind.CONDITIONS_NOT_MET,
                                    atMillis = batchNow,
                                    eventId = envelope.id,
                                ),
                            )
                            continue
                        }
                    }
                }

                // Priority conflict resolution: a higher-priority candidate that
                // already fired and claimed a setting blocks lower-priority
                // candidates that would overwrite the same setting.
                val affected =
                    if (event is TriggerEvent.ModeWindowEnd) {
                        emptySet()
                    } else {
                        automation.actions.flatMap { it.affectedSettings }.toSet()
                    }
                val overlap = affected intersect claimedSettings
                if (overlap.isNotEmpty()) {
                    audit.record(
                        AuditEvent(
                            automationId = automation.id,
                            kind = AuditKind.SUPPRESSED_CONFLICT,
                            atMillis = batchNow,
                            detail = overlap.joinToString(","),
                            eventId = envelope.id,
                        ),
                    )
                    continue
                }

                // Window-end: restore snapshots and stop — the action list must
                // NOT run again or it would instantly undo the restore. Windowed
                // automations snapshot at start regardless of MODE/ROUTINE type.
                val isWindowed = automation.trigger is Trigger.TimeWindow
                if (event is TriggerEvent.ModeWindowEnd && isWindowed) {
                    restoreSnapshots(automation.id, batchNow)
                    // Glyph output isn't a settings key — clear it explicitly so
                    // text/matrix/stripes never linger after the mode ends.
                    if (automation.actions.any { it.isGlyphAction }) {
                        runCatching {
                            executor.execute(
                                Action.GlyphTurnOff,
                                FireContext(
                                    eventId = "restore:${automation.id.value}",
                                    executionId = "restore:${automation.id.value}:$batchNow",
                                    automationId = automation.id,
                                    actionIndex = -2,
                                    priority = 100,
                                ),
                            )
                        }
                    }
                }

                // Window-start: snapshot affected settings before executing
                if (event is TriggerEvent.ModeWindowStart && isWindowed) {
                    snapshotSettings(automation, batchNow)
                }

                if (event !is TriggerEvent.ModeWindowEnd) {
                    automation.actions.forEachIndexed { index, action ->
                        val context =
                            FireContext(
                                eventId = envelope.id,
                                executionId = executionId,
                                automationId = automation.id,
                                actionIndex = index,
                                priority = automation.priority,
                            )
                        val result =
                            try {
                                executor.execute(action, context)
                            } catch (_: CancellationException) {
                                throw CancellationException()
                            } catch (_: Exception) {
                                ActionResult.Failure("executor_exception")
                            }
                        actionResults += result
                    }
                }

                claimedSettings += affected

                val completedAt = now()
                val latencyMillis = (System.nanoTime() - startedAtNano) / 1_000_000

                journal.finish(
                    ExecutionCompletion(
                        executionId = executionId,
                        automationId = automation.id,
                        atMillis = completedAt,
                        status =
                            if (actionResults.all {
                                    it is ActionResult.Success || it is ActionResult.NeedsUserAction
                                }
                            ) {
                                ExecutionStatus.COMPLETED
                            } else {
                                ExecutionStatus.FAILED
                            },
                        actionCount = actionResults.size,
                    ),
                )

                // Modes and routines merged: window semantics come from the
                // trigger, not the legacy type flag.
                val modeLike = automation.type == AutomationType.MODE || isWindowed
                val isModeActivation = modeLike && event !is TriggerEvent.ModeWindowEnd
                val isModeDeactivation = event is TriggerEvent.ModeWindowEnd && modeLike

                if (isModeActivation) {
                    modeActivationSink.activate(automation.id, batchNow)
                } else if (isModeDeactivation) {
                    modeActivationSink.deactivate(automation.id, batchNow)
                }

                audit.record(
                    AuditEvent(
                        automationId = automation.id,
                        kind =
                            if (isModeDeactivation) {
                                AuditKind.MODE_DEACTIVATED
                            } else if (modeLike) {
                                AuditKind.MODE_ACTIVATED
                            } else {
                                AuditKind.FIRED
                            },
                        atMillis = completedAt,
                        eventId = envelope.id,
                        executionId = executionId,
                        latencyMillis = latencyMillis,
                    ),
                )

                outcomes += FireOutcome(automation, automation.actions, actionResults.toList(), envelope.id, executionId)
            } catch (e: CancellationException) {
                journal.finish(
                    ExecutionCompletion(
                        executionId = executionId,
                        automationId = automation.id,
                        atMillis = batchNow,
                        status = ExecutionStatus.CANCELLED,
                        actionCount = actionResults.size,
                    ),
                )
                throw e
            } catch (e: Exception) {
                journal.finish(
                    ExecutionCompletion(
                        executionId = executionId,
                        automationId = automation.id,
                        atMillis = batchNow,
                        status = ExecutionStatus.FAILED,
                        actionCount = actionResults.size,
                    ),
                )
                audit.record(
                    AuditEvent(
                        automationId = automation.id,
                        kind = AuditKind.ERROR,
                        atMillis = batchNow,
                        detail = e::class.simpleName ?: "error",
                        eventId = envelope.id,
                        executionId = executionId,
                    ),
                )
            } catch (e: StackOverflowError) {
                journal.finish(
                    ExecutionCompletion(
                        executionId = executionId,
                        automationId = automation.id,
                        atMillis = batchNow,
                        status = ExecutionStatus.FAILED,
                        actionCount = actionResults.size,
                    ),
                )
                audit.record(
                    AuditEvent(
                        automationId = automation.id,
                        kind = AuditKind.ERROR,
                        atMillis = batchNow,
                        detail = "stack_overflow: condition nesting too deep",
                        eventId = envelope.id,
                        executionId = executionId,
                    ),
                )
            }
        }
        return outcomes
    }

    private suspend fun snapshotSettings(
        automation: Automation,
        batchNow: Long,
    ) {
        // Don't overwrite existing snapshots — if the mode is already active,
        // re-capturing would store the mode's own values and break restore.
        if (snapshotStore.forAutomation(automation.id).isNotEmpty()) return
        val keys =
            automation.actions
                .filter { it.supportsRestore }
                .flatMap { it.affectedSettings }
                .toSet()
        for (key in keys) {
            val value = runCatching { settingReader.read(key) }.getOrNull() ?: continue
            snapshotStore.save(
                StateSnapshot(
                    automationId = automation.id,
                    settingKey = key,
                    previousValue = value,
                    capturedAtMillis = batchNow,
                    namespace = namespaceForKey(key),
                ),
            )
        }
    }

    private suspend fun restoreSnapshots(
        id: AutomationId,
        batchNow: Long,
    ) {
        val snapshots = snapshotStore.forAutomation(id)
        // Deduplicate by setting key, keeping the newest snapshot per key
        val latestByKey = snapshots.associateBy { it.settingKey }
        for (snapshot in latestByKey.values) {
            val restoreAction = restoreActionFor(snapshot) ?: continue
            val context =
                FireContext(
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

    /**
     * Maps a snapshotted key back to a real action. Semantic keys (dnd_mode,
     * night_mode, volume_*) restore through their proper controllers; plain
     * settings keys go through WriteSetting. Glyph keys are cleared separately.
     */
    private fun restoreActionFor(snapshot: StateSnapshot): Action? {
        val value = snapshot.previousValue
        return when {
            snapshot.settingKey == "dnd_mode" ->
                Action.SetDnd(
                    mode =
                        runCatching {
                            com.tdvorak.nothingmodes.engine.model.DndMode
                                .valueOf(value)
                        }.getOrDefault(com.tdvorak.nothingmodes.engine.model.DndMode.OFF),
                    restore = false,
                )
            snapshot.settingKey == "night_mode" ->
                Action.SetDarkMode(
                    mode =
                        runCatching {
                            com.tdvorak.nothingmodes.engine.model.NightMode
                                .valueOf(value)
                        }.getOrDefault(com.tdvorak.nothingmodes.engine.model.NightMode.OFF),
                    restore = false,
                )
            snapshot.settingKey == "wifi_enabled" ->
                value.toBooleanStrictOrNull()?.let { Action.SetWifi(on = it, restore = false) }
            snapshot.settingKey == "bluetooth_enabled" ->
                value.toBooleanStrictOrNull()?.let { Action.SetBluetooth(on = it, restore = false) }
            snapshot.settingKey == "mobile_data_enabled" ->
                value.toBooleanStrictOrNull()?.let { Action.SetMobileData(on = it, restore = false) }
            snapshot.settingKey == "flashlight_on" ->
                value.toBooleanStrictOrNull()?.let { Action.SetFlashlight(on = it, restore = false) }
            snapshot.settingKey == "aod_enabled" ->
                value.toBooleanStrictOrNull()?.let {
                    Action.SetAlwaysOnDisplay(
                        mode = if (it) AodMode.ALWAYS_ON else AodMode.OFF,
                        restore = false,
                    )
                }
            snapshot.settingKey == "nfc_enabled" ->
                value.toBooleanStrictOrNull()?.let { Action.SetNfc(on = it, restore = false) }
            snapshot.settingKey == "hotspot_enabled" ->
                value.toBooleanStrictOrNull()?.let { Action.SetHotspot(on = it, restore = false) }
            snapshot.settingKey == "location_mode" ->
                runCatching {
                    Action.SetLocationMode(
                        mode =
                            com.tdvorak.nothingmodes.engine.model.LocationMode
                                .valueOf(value),
                        restore = false,
                    )
                }.getOrNull()
            snapshot.settingKey == "auto_sync" ->
                value.toBooleanStrictOrNull()?.let { Action.SetAutoSync(on = it, restore = false) }
            snapshot.settingKey == "ringer_mode" ->
                Action.SetRinger(mode = value, restore = false)
            snapshot.settingKey.matches(volumeKeyRegex) -> {
                val stream =
                    runCatching {
                        com.tdvorak.nothingmodes.engine.model.VolumeStream
                            .valueOf(snapshot.settingKey.removePrefix("volume_").uppercase())
                    }.getOrNull() ?: return null
                val level = value.toIntOrNull() ?: return null
                Action.SetVolume(volumes = mapOf(stream to level), restore = false)
            }
            snapshot.settingKey in setOf("glyph_state", "glyph_matrix_state") -> null
            else ->
                Action.WriteSetting(
                    namespace =
                        when (snapshot.namespace) {
                            "secure" ->
                                com.tdvorak.nothingmodes.engine.model.SettingNamespace.SECURE
                            "global" ->
                                com.tdvorak.nothingmodes.engine.model.SettingNamespace.GLOBAL
                            else ->
                                com.tdvorak.nothingmodes.engine.model.SettingNamespace.SYSTEM
                        },
                    key = snapshot.settingKey,
                    value = value,
                )
        }
    }

    private val volumeKeyRegex = Regex("^volume_.*$")

    /** Maps a setting key to its Android Settings namespace. */
    private fun namespaceForKey(key: String): String =
        when (key) {
            "reduce_bright_colors_activated", "aod_enabled", "location_mode" -> "secure"
            "airplane_mode_on", "low_power", "data_saver", "mobile_data_enabled" -> "global"
            else -> "system"
        }

    /** Checks if a time-based trigger should fire on the current day. */
    private fun shouldFireOnDay(
        trigger: Trigger,
        nowMillis: Long,
    ): Boolean {
        val days =
            when (trigger) {
                is Trigger.Time -> trigger.days
                is Trigger.TimeWindow -> trigger.days
                else -> return true
            } ?: return true
        if (days.isEmpty()) return true
        val tz =
            when (trigger) {
                is Trigger.Time -> trigger.tz
                is Trigger.TimeWindow -> trigger.tz
                else -> return true
            }
        val zone = runCatching { ZoneId.of(tz) }.getOrNull() ?: return true
        val javaDay = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone).dayOfWeek
        val engineDay =
            when (javaDay) {
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

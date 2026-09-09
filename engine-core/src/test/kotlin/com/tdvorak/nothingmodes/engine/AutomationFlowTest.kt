package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.ExecutionCompletion
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.ExecutionStatus
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink
import com.tdvorak.nothingmodes.engine.runtime.NoopActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.SettingReader
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshot
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshotStore
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutomationFlowTest {
    @Test
    fun `overnight time window fires start and end with snapshot restore`() =
        runTest {
            val store = InMemoryAutomationStore()
            val mode =
                Automation(
                    id = AutomationId("mode-sleep"),
                    name = "Sleep",
                    type = AutomationType.MODE,
                    createdBy = CreatedBy.USER,
                    status = AutomationStatus.ARMED,
                    trigger =
                        Trigger.TimeWindow(
                            startLocal = "22:00",
                            endLocal = "07:00",
                            tz = "Europe/Prague",
                        ),
                    actions =
                        listOf(
                            Action.SetDnd(DndMode.PRIORITY),
                            Action.SetBrightness(26, restore = true),
                        ),
                )
            store.save(mode)

            val snapshots = mutableListOf<StateSnapshot>()
            val snapshotStore =
                object : StateSnapshotStore {
                    override suspend fun save(snapshot: StateSnapshot) {
                        snapshots.add(snapshot)
                    }

                    override suspend fun forAutomation(id: AutomationId): List<StateSnapshot> = snapshots.filter { it.automationId == id }

                    override suspend fun deleteForAutomation(id: AutomationId) {
                        snapshots.removeAll { it.automationId == id }
                    }
                }

            val settingReader =
                SettingReader { key ->
                    when (key) {
                        "screen_brightness" -> "128"
                        "dnd_mode" -> "OFF"
                        else -> null
                    }
                }

            var clock = 0L
            val activations = RecordingModeActivationSink()
            val journal = RecordingExecutionJournal()
            val engine =
                Engine(
                    store = store,
                    executor = NoopActionExecutor,
                    snapshotStore = snapshotStore,
                    settingReader = settingReader,
                    modeActivationSink = activations,
                    journal = journal,
                    executionIds = StableExecutionIdFactory,
                    now = { clock },
                )

            // 22:00 — window start
            clock = 0
            val start1 =
                engine.onTrigger(
                    TriggerEnvelope(
                        id = "evt-start-1",
                        event =
                            TriggerEvent.ModeWindowStart(
                                eventId = "evt-start-1",
                                automationId = mode.id,
                                atMillis = clock,
                            ),
                        receivedAtMillis = clock,
                    ),
                )
            assertEquals(1, start1.size, "Window start should produce exactly one outcome")
            assertTrue(start1[0].results.all { it is ActionResult.Success })
            assertEquals(1, activations.active.size, "Mode should activate on start")
            assertEquals(2, snapshots.size, "Settings should be snapshotted before run")

            // 07:00 — window end, 9 hours later
            clock = 1000L * 60 * 60 * 9
            val end1 =
                engine.onTrigger(
                    TriggerEnvelope(
                        id = "evt-end-1",
                        event =
                            TriggerEvent.ModeWindowEnd(
                                eventId = "evt-end-1",
                                automationId = mode.id,
                                atMillis = clock,
                            ),
                        receivedAtMillis = clock,
                    ),
                )
            assertEquals(1, end1.size, "Window end should produce one completion")
            assertTrue(end1[0].results.isEmpty(), "Window end should only restore, not run actions")
            assertTrue(activations.inactive.contains(mode.id.value), "Mode should deactivate on end")
            assertTrue(snapshots.isEmpty(), "Restore should delete snapshots")

            // Next-day 22:00 — start again, 24 hours after first start
            clock = 1000L * 60 * 60 * 24
            val start2 =
                engine.onTrigger(
                    TriggerEnvelope(
                        id = "evt-start-2",
                        event =
                            TriggerEvent.ModeWindowStart(
                                eventId = "evt-start-2",
                                automationId = mode.id,
                                atMillis = clock,
                            ),
                        receivedAtMillis = clock,
                    ),
                )
            assertEquals(1, start2.size, "Next-day window start should fire again")
            assertEquals(2, activations.active.size, "Mode should re-activate")

            assertEquals(3, journal.completions.size)
            assertEquals(ExecutionStatus.COMPLETED, journal.completions[0].status)
            assertEquals(ExecutionStatus.COMPLETED, journal.completions[1].status)
            assertEquals(ExecutionStatus.COMPLETED, journal.completions[2].status)
        }

    @Test
    fun `cooldown suppresses repeated window start events`() =
        runTest {
            val store = InMemoryAutomationStore()
            val mode =
                Automation(
                    id = AutomationId("mode-cooldown"),
                    name = "Cooldown",
                    type = AutomationType.MODE,
                    createdBy = CreatedBy.USER,
                    status = AutomationStatus.ARMED,
                    trigger =
                        Trigger.TimeWindow(
                            startLocal = "08:00",
                            endLocal = "09:00",
                            tz = "Europe/Prague",
                        ),
                    actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
                    cooldownMs = 60_000,
                )
            store.save(mode)

            var clock = 0L
            val engine =
                Engine(
                    store = store,
                    executor = NoopActionExecutor,
                    executionIds = StableExecutionIdFactory,
                    now = { clock },
                )

            val start1 =
                engine.onTrigger(
                    TriggerEnvelope(
                        id = "evt-1",
                        event =
                            TriggerEvent.ModeWindowStart(
                                eventId = "evt-1",
                                automationId = mode.id,
                                atMillis = clock,
                            ),
                        receivedAtMillis = clock,
                    ),
                )
            assertEquals(1, start1.size)

            // 30 seconds later: still inside the 60s cooldown
            clock = 30_000
            val start2 =
                engine.onTrigger(
                    TriggerEnvelope(
                        id = "evt-2",
                        event =
                            TriggerEvent.ModeWindowStart(
                                eventId = "evt-2",
                                automationId = mode.id,
                                atMillis = clock,
                            ),
                        receivedAtMillis = clock,
                    ),
                )
            assertEquals(0, start2.size, "Start inside cooldown should be blocked")

            // 90 seconds after the first start: cooldown expired
            clock = 90_000
            val start3 =
                engine.onTrigger(
                    TriggerEnvelope(
                        id = "evt-3",
                        event =
                            TriggerEvent.ModeWindowStart(
                                eventId = "evt-3",
                                automationId = mode.id,
                                atMillis = clock,
                            ),
                        receivedAtMillis = clock,
                    ),
                )
            assertEquals(1, start3.size, "Start after cooldown should be allowed")
        }

    private class RecordingModeActivationSink : ModeActivationSink {
        val active = mutableListOf<String>()
        val inactive = mutableListOf<String>()

        override suspend fun activate(
            automationId: AutomationId,
            atMillis: Long,
        ) {
            active.add(automationId.value)
        }

        override suspend fun deactivate(
            automationId: AutomationId,
            atMillis: Long,
        ) {
            inactive.add(automationId.value)
        }
    }

    private class RecordingExecutionJournal : ExecutionJournal {
        val completions = mutableListOf<ExecutionCompletion>()

        override suspend fun finish(completion: ExecutionCompletion) {
            completions.add(completion)
        }
    }
}

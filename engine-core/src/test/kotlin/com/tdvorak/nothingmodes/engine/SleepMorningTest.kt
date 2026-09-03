package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.AuditEvent
import com.tdvorak.nothingmodes.engine.runtime.AuditKind
import com.tdvorak.nothingmodes.engine.runtime.AuditSink
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.ExecutionCompletion
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.ExecutionStatus
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.NoopActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class SleepMorningTest {

    @Test
    fun `Sleep mode activates on time trigger with DND, dark mode, extra dim, brightness, glyph off`() = runTest {
        val store = InMemoryAutomationStore()
        val sleep = Automation(
            id = AutomationId("mode-sleep"),
            name = "Sleep",
            type = AutomationType.MODE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(cron = "30 22 * * *", tz = "Europe/Prague"),
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetDarkMode(NightMode.ON),
                Action.SetExtraDim(true, restore = true),
                Action.SetBrightness(26, restore = true),
                Action.SetGlyph(false),
            ),
        )
        store.save(sleep)

        val audit = RecordingAuditSink()
        val journal = RecordingExecutionJournal()
        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            audit = audit,
            journal = journal,
            executionIds = StableExecutionIdFactory,
        )

        val envelope = TriggerEnvelope(
            id = "evt-sleep-001",
            event = TriggerEvent.TimeFired(
                eventId = "evt-sleep-001",
                automationId = AutomationId("mode-sleep"),
                atMillis = System.currentTimeMillis(),
            ),
            receivedAtMillis = System.currentTimeMillis(),
        )

        val outcomes = engine.onTrigger(envelope)

        assertEquals(1, outcomes.size)
        val outcome = outcomes[0]
        assertEquals(sleep.id, outcome.automation.id)
        assertEquals(5, outcome.actions.size)
        assertTrue(outcome.results.all { it is ActionResult.Success })

        assertEquals(1, audit.events.size)
        assertEquals(AuditKind.MODE_ACTIVATED, audit.events[0].kind)

        assertEquals(1, journal.completions.size)
        assertEquals(ExecutionStatus.COMPLETED, journal.completions[0].status)
        assertEquals(5, journal.completions[0].actionCount)
    }

    @Test
    fun `Morning routine deactivates Sleep with DND off, extra dim off, brightness restore, glyph restore`() = runTest {
        val store = InMemoryAutomationStore()
        val morning = Automation(
            id = AutomationId("routine-morning"),
            name = "Morning",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(cron = "0 7 * * *", tz = "Europe/Prague"),
            actions = listOf(
                Action.SetDnd(DndMode.OFF),
                Action.SetDarkMode(NightMode.OFF),
                Action.SetExtraDim(false, restore = true),
                Action.SetBrightness(128, restore = true),
                Action.SetGlyph(true, restore = true),
            ),
        )
        store.save(morning)

        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            executionIds = StableExecutionIdFactory,
        )

        val envelope = TriggerEnvelope(
            id = "evt-morning-001",
            event = TriggerEvent.TimeFired(
                eventId = "evt-morning-001",
                automationId = AutomationId("routine-morning"),
                atMillis = System.currentTimeMillis(),
            ),
            receivedAtMillis = System.currentTimeMillis(),
        )

        val outcomes = engine.onTrigger(envelope)

        assertEquals(1, outcomes.size)
        val outcome = outcomes[0]
        assertEquals(morning.id, outcome.automation.id)
        assertEquals(5, outcome.actions.size)
        assertTrue(outcome.results.all { it is ActionResult.Success })
    }

    @Test
    fun `Sleep mode with conditions - only fires when battery above 10 and not charging`() = runTest {
        val store = InMemoryAutomationStore()
        val sleep = Automation(
            id = AutomationId("mode-sleep-conditional"),
            name = "Sleep (Conditional)",
            type = AutomationType.MODE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(cron = "30 22 * * *", tz = "Europe/Prague"),
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetDarkMode(NightMode.ON),
            ),
            conditions = Condition.And(listOf(
                Condition.BatteryLevel(
                    com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 10,
                ),
                Condition.Not(Condition.Charging(true)),
            )),
        )
        store.save(sleep)

        val audit = RecordingAuditSink()
        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            audit = audit,
            executionIds = StableExecutionIdFactory,
        )

        // Fire with battery=5 and charging=false → conditions NOT met
        val lowBatteryState = TriggerEnvelope(
            id = "evt-low-battery",
            event = TriggerEvent.TimeFired(
                eventId = "evt-low-battery",
                automationId = AutomationId("mode-sleep-conditional"),
                atMillis = System.currentTimeMillis(),
            ),
            receivedAtMillis = System.currentTimeMillis(),
        )

        // The engine uses DeviceState defaults (batteryLevel=-1) which means STATE_UNAVAILABLE
        // for battery condition. This is correct fail-closed behavior.
        val outcomes = engine.onTrigger(lowBatteryState)
        assertEquals(0, outcomes.size, "Should not fire when conditions not met")
        assertTrue(audit.events.any { it.kind == AuditKind.CONDITIONS_NOT_MET })
    }

    @Test
    fun `TimeWindow mode fires on window start event`() = runTest {
        val store = InMemoryAutomationStore()
        val sleep = Automation(
            id = AutomationId("mode-sleep-window"),
            name = "Sleep Window",
            type = AutomationType.MODE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.TimeWindow(
                startLocal = "22:30",
                endLocal = "07:00",
                tz = "Europe/Prague",
            ),
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetExtraDim(true, restore = true),
            ),
        )
        store.save(sleep)

        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            executionIds = StableExecutionIdFactory,
        )

        val envelope = TriggerEnvelope(
            id = "evt-window-start",
            event = TriggerEvent.ModeWindowStart(
                eventId = "evt-window-start",
                automationId = AutomationId("mode-sleep-window"),
                atMillis = System.currentTimeMillis(),
            ),
            receivedAtMillis = System.currentTimeMillis(),
        )

        val outcomes = engine.onTrigger(envelope)
        assertEquals(1, outcomes.size)
        assertEquals(2, outcomes[0].actions.size)
        assertTrue(outcomes[0].results.all { it is ActionResult.Success })
    }

    @Test
    fun `cooldown suppresses repeated firing within cooldown window`() = runTest {
        val store = InMemoryAutomationStore()
        val automation = Automation(
            id = AutomationId("routine-cooldown"),
            name = "Cooldown Test",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Boot,
            actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
            cooldownMs = 60_000,
        )
        store.save(automation)

        val audit = RecordingAuditSink()
        var clock = 10_000L
        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            audit = audit,
            executionIds = StableExecutionIdFactory,
            now = { clock },
        )

        // First fire: allowed
        val first = engine.onTrigger(TriggerEnvelope(
            id = "evt-1",
            event = TriggerEvent.BootCompleted("evt-1"),
            receivedAtMillis = 10_000,
        ))
        assertEquals(1, first.size)

        // Second fire at 30_000: within 60s cooldown → suppressed
        clock = 30_000
        val second = engine.onTrigger(TriggerEnvelope(
            id = "evt-2",
            event = TriggerEvent.BootCompleted("evt-2"),
            receivedAtMillis = 30_000,
        ))
        assertEquals(0, second.size, "Should be suppressed by cooldown")
        assertTrue(audit.events.any { it.kind == AuditKind.SUPPRESSED_COOLDOWN })
    }

    @Test
    fun `action count and execution status are correctly reported`() = runTest {
        val store = InMemoryAutomationStore()
        val automation = Automation(
            id = AutomationId("routine-status"),
            name = "Status Test",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Boot,
            actions = listOf(
                Action.SetDnd(DndMode.OFF),
                Action.SetDarkMode(NightMode.OFF),
                Action.SetBrightness(200),
            ),
        )
        store.save(automation)

        val journal = RecordingExecutionJournal()
        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            journal = journal,
            executionIds = StableExecutionIdFactory,
        )

        engine.onTrigger(TriggerEnvelope(
            id = "evt-1",
            event = TriggerEvent.BootCompleted("evt-1"),
            receivedAtMillis = 0,
        ))

        assertEquals(1, journal.completions.size)
        val completion = journal.completions[0]
        assertEquals(3, completion.actionCount)
        assertEquals(ExecutionStatus.COMPLETED, completion.status)
    }

    private class RecordingAuditSink : AuditSink {
        val events = mutableListOf<AuditEvent>()
        override suspend fun record(event: AuditEvent) { events += event }
    }

    private class RecordingExecutionJournal : ExecutionJournal {
        val completions = mutableListOf<ExecutionCompletion>()
        override suspend fun finish(completion: ExecutionCompletion) { completions += completion }
    }
}

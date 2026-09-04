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
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.ExecutionCompletion
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.ExecutionStatus
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineEdgeCasesTest {

    private fun makeAutomation(
        id: String,
        actions: List<Action>,
        trigger: Trigger = Trigger.Boot,
    ) = Automation(
        id = AutomationId(id),
        name = id,
        type = AutomationType.ROUTINE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = trigger,
        actions = actions,
    )

    private fun bootEnvelope(id: String = "evt-1") = TriggerEnvelope(
        id = id,
        event = TriggerEvent.BootCompleted(id),
        receivedAtMillis = 1000L,
    )

    @Test
    fun `empty action list produces success outcome with zero results`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("empty", actions = emptyList()))
        val engine = Engine(store = store, executor = ActionExecutor { _, _ -> ActionResult.Success })

        val outcomes = engine.onTrigger(bootEnvelope())

        assertEquals(1, outcomes.size)
        assertEquals(0, outcomes[0].results.size)
        assertTrue(outcomes[0].results.all { it is ActionResult.Success })
    }

    @Test
    fun `all actions fail produces failure outcome`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("all-fail", actions = listOf(
            Action.SetDnd(DndMode.TOTAL),
            Action.SetBrightness(100),
            Action.Vibrate(500),
        )))
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Failure("boom") },
        )

        val outcomes = engine.onTrigger(bootEnvelope())

        assertEquals(1, outcomes.size)
        assertEquals(3, outcomes[0].results.size)
        assertTrue(outcomes[0].results.all { it is ActionResult.Failure })
    }

    @Test
    fun `mixed success and failure produces partial failure outcome`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mixed", actions = listOf(
            Action.SetDnd(DndMode.OFF),
            Action.SetBrightness(100),
            Action.Vibrate(500),
        )))
        val callCount = java.util.concurrent.atomic.AtomicInteger(0)
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ ->
                if (callCount.incrementAndGet() == 2) ActionResult.Failure("middle_failed")
                else ActionResult.Success
            },
        )

        val outcomes = engine.onTrigger(bootEnvelope())

        assertEquals(1, outcomes.size)
        assertEquals(3, outcomes[0].results.size)
        assertTrue(outcomes[0].results[0] is ActionResult.Success)
        assertTrue(outcomes[0].results[1] is ActionResult.Failure)
        assertTrue(outcomes[0].results[2] is ActionResult.Success)
    }

    @Test
    fun `mixed success failure reports failed execution status`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mixed-status", actions = listOf(
            Action.SetDnd(DndMode.OFF),
            Action.SetBrightness(100),
        )))
        val journal = RecordingJournal()
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, ctx ->
                if (ctx.actionIndex == 1) ActionResult.Failure("fail")
                else ActionResult.Success
            },
            journal = journal,
        )

        engine.onTrigger(bootEnvelope())

        assertEquals(1, journal.completions.size)
        assertEquals(ExecutionStatus.FAILED, journal.completions[0].status)
    }

    @Test
    fun `empty action list reports completed execution status`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("empty-ok", actions = emptyList()))
        val journal = RecordingJournal()
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
            journal = journal,
        )

        engine.onTrigger(bootEnvelope())

        assertEquals(1, journal.completions.size)
        assertEquals(ExecutionStatus.COMPLETED, journal.completions[0].status)
        assertEquals(0, journal.completions[0].actionCount)
    }

    @Test
    fun `all actions fail reports failed execution status`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("all-fail-status", actions = listOf(
            Action.SetDnd(DndMode.OFF),
        )))
        val journal = RecordingJournal()
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Failure("fail") },
            journal = journal,
        )

        engine.onTrigger(bootEnvelope())

        assertEquals(1, journal.completions.size)
        assertEquals(ExecutionStatus.FAILED, journal.completions[0].status)
    }

    @Test
    fun `no armed automations produces empty outcome list`() = runTest {
        val store = InMemoryAutomationStore()
        // Store is empty
        val engine = Engine(store = store, executor = ActionExecutor { _, _ -> ActionResult.Success })

        val outcomes = engine.onTrigger(bootEnvelope())

        assertTrue(outcomes.isEmpty())
    }

    @Test
    fun `pending approval automation does not fire`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("pending", actions = listOf(Action.SetDnd(DndMode.OFF)))
            .copy(status = AutomationStatus.PENDING_APPROVAL))
        var fired = false
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> fired = true; ActionResult.Success },
        )

        engine.onTrigger(bootEnvelope())

        assertTrue(!fired, "Pending approval automation should not fire")
    }

    @Test
    fun `executor exception is caught and recorded as failure`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("throws", actions = listOf(Action.SetDnd(DndMode.OFF))))
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> error("unexpected crash") },
        )

        val outcomes = engine.onTrigger(bootEnvelope())

        assertEquals(1, outcomes.size)
        assertTrue(outcomes[0].results[0] is ActionResult.Failure)
    }

    @Test
    fun `multiple armed automations fire on broadcast event`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("a1", actions = listOf(Action.SetDnd(DndMode.OFF))))
        store.save(makeAutomation("a2", actions = listOf(Action.SetDnd(DndMode.PRIORITY))))
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
        )

        val outcomes = engine.onTrigger(bootEnvelope())

        assertEquals(2, outcomes.size)
    }

    private class RecordingJournal : ExecutionJournal {
        val completions = mutableListOf<ExecutionCompletion>()
        override suspend fun finish(completion: ExecutionCompletion) { completions += completion }
    }
}

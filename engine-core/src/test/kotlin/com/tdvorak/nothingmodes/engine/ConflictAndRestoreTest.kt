package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.DeviceState
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink
import com.tdvorak.nothingmodes.engine.runtime.SettingReader
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshot
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshotStore
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ConflictAndRestoreTest {

    private fun makeMode(
        id: String,
        name: String,
        priority: Int,
        actions: List<Action>,
        trigger: Trigger = Trigger.TimeWindow("22:00", "07:00", "Europe/Prague"),
    ) = Automation(
        id = AutomationId(id),
        name = name,
        type = AutomationType.MODE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = trigger,
        actions = actions,
        priority = priority,
    )

    private fun envelope(id: String, event: TriggerEvent) = TriggerEnvelope(id = id, event = event, receivedAtMillis = 1000L)

    @Test
    fun `higher priority mode executes first`() = runTest {
        val store = InMemoryAutomationStore()
        val lowPriority = makeMode("mode-low", "Low", priority = 1, actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
            trigger = Trigger.Boot)
        val highPriority = makeMode("mode-high", "High", priority = 10, actions = listOf(Action.SetDnd(DndMode.TOTAL)),
            trigger = Trigger.Boot)
        store.save(lowPriority)
        store.save(highPriority)

        val executionOrder = mutableListOf<String>()
        val executor = ActionExecutor { _, context ->
            executionOrder.add(context.automationId.value)
            ActionResult.Success
        }

        val engine = Engine(store = store, executor = executor)
        engine.onTrigger(envelope("evt1", TriggerEvent.BootCompleted("evt1")))

        assertEquals(listOf("mode-high", "mode-low"), executionOrder)
    }

    @Test
    fun `mode activation records to mode activation sink`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode("mode-sleep", "Sleep", priority = 10, actions = listOf(Action.SetDnd(DndMode.PRIORITY)))
        store.save(mode)

        val activations = mutableListOf<Pair<String, Long>>()
        val sink = object : ModeActivationSink {
            override suspend fun activate(automationId: AutomationId, atMillis: Long) {
                activations.add(automationId.value to atMillis)
            }
            override suspend fun deactivate(automationId: AutomationId, atMillis: Long) {}
        }

        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
            modeActivationSink = sink,
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowStart("evt1", AutomationId("mode-sleep"), 1000L)))

        assertEquals(1, activations.size)
        assertEquals("mode-sleep", activations[0].first)
    }

    @Test
    fun `mode deactivation records to mode activation sink`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode("mode-sleep", "Sleep", priority = 10, actions = listOf(Action.SetDnd(DndMode.PRIORITY)))
        store.save(mode)

        val deactivations = mutableListOf<Pair<String, Long>>()
        val sink = object : ModeActivationSink {
            override suspend fun activate(automationId: AutomationId, atMillis: Long) {}
            override suspend fun deactivate(automationId: AutomationId, atMillis: Long) {
                deactivations.add(automationId.value to atMillis)
            }
        }

        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
            modeActivationSink = sink,
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowEnd("evt1", AutomationId("mode-sleep"), 1000L)))

        assertEquals(1, deactivations.size)
        assertEquals("mode-sleep", deactivations[0].first)
    }

    @Test
    fun `mode window start snapshots affected settings`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode(
            "mode-sleep", "Sleep", priority = 10,
            actions = listOf(
                Action.SetBrightness(26, restore = true),
                Action.SetExtraDim(true, restore = true),
            ),
        )
        store.save(mode)

        val snapshots = mutableListOf<StateSnapshot>()
        val snapshotStore = object : StateSnapshotStore {
            override suspend fun save(snapshot: StateSnapshot) { snapshots.add(snapshot) }
            override suspend fun forAutomation(id: AutomationId): List<StateSnapshot> = snapshots.filter { it.automationId == id }
            override suspend fun deleteForAutomation(id: AutomationId) { snapshots.removeAll { it.automationId == id } }
        }
        val settingReader = SettingReader { key ->
            when (key) {
                "screen_brightness" -> "128"
                "reduce_bright_colors_activated" -> "0"
                else -> null
            }
        }

        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
            snapshotStore = snapshotStore,
            settingReader = settingReader,
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowStart("evt1", AutomationId("mode-sleep"), 1000L)))

        assertEquals(2, snapshots.size)
        assertTrue(snapshots.any { it.settingKey == "screen_brightness" && it.previousValue == "128" })
        assertTrue(snapshots.any { it.settingKey == "reduce_bright_colors_activated" && it.previousValue == "0" })
    }

    @Test
    fun `mode window end restores previous settings via WriteSetting`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode(
            "mode-sleep", "Sleep", priority = 10,
            actions = listOf(
                Action.SetBrightness(26, restore = true),
                Action.SetExtraDim(true, restore = true),
            ),
        )
        store.save(mode)

        val snapshots = mutableListOf(
            StateSnapshot(AutomationId("mode-sleep"), "screen_brightness", "128", 1000),
            StateSnapshot(AutomationId("mode-sleep"), "reduce_bright_colors_activated", "0", 1000),
        )
        val snapshotStore = object : StateSnapshotStore {
            override suspend fun save(snapshot: StateSnapshot) { snapshots.add(snapshot) }
            override suspend fun forAutomation(id: AutomationId): List<StateSnapshot> = snapshots.filter { it.automationId == id }
            override suspend fun deleteForAutomation(id: AutomationId) { snapshots.removeAll { it.automationId == id } }
        }

        val executedActions = mutableListOf<Action>()
        val executor = ActionExecutor { action, _ ->
            executedActions.add(action)
            ActionResult.Success
        }

        val engine = Engine(
            store = store,
            executor = executor,
            snapshotStore = snapshotStore,
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowEnd("evt1", AutomationId("mode-sleep"), 1000L)))

        val restoreActions = executedActions.filterIsInstance<Action.WriteSetting>()
        assertEquals(2, restoreActions.size)
        assertTrue(restoreActions.any { it.key == "screen_brightness" && it.value == "128" })
        assertTrue(restoreActions.any { it.key == "reduce_bright_colors_activated" && it.value == "0" })
        assertEquals(0, snapshots.size)
    }

    @Test
    fun `disabled automation does not fire`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode("mode-disabled", "Disabled", priority = 10, actions = listOf(Action.SetDnd(DndMode.PRIORITY)))
        store.save(mode.copy(enabled = false))

        var fired = false
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> fired = true; ActionResult.Success },
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowStart("evt1", AutomationId("mode-disabled"), 1000L)))

        assertTrue(!fired, "Disabled automation should not fire")
    }

    @Test
    fun `condition not met prevents execution`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode(
            "mode-conditional", "Conditional", priority = 10,
            actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
        ).copy(
            conditions = Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.LT, 20),
        )
        store.save(mode)

        var fired = false
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> fired = true; ActionResult.Success },
            stateProvider = { DeviceState(batteryLevel = 80) },
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowStart("evt1", AutomationId("mode-conditional"), 1000L)))

        assertTrue(!fired, "Condition not met should prevent execution")
    }

    @Test
    fun `cooldown suppresses duplicate firing`() = runTest {
        val store = InMemoryAutomationStore()
        val automation = Automation(
            id = AutomationId("routine-test"),
            name = "Test",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Boot,
            actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
            cooldownMs = 60_000,
        )
        store.save(automation)

        val fireCount = AtomicInteger(0)
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> fireCount.incrementAndGet(); ActionResult.Success },
            now = { 1000L },
        )

        engine.onTrigger(envelope("evt1", TriggerEvent.BootCompleted("evt1")))
        engine.onTrigger(TriggerEnvelope(id = "evt2", event = TriggerEvent.BootCompleted("evt2"), receivedAtMillis = 2000L))

        assertEquals(1, fireCount.get(), "Cooldown should suppress second fire")
    }

    @Test
    fun `failed action records failed execution status`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode("mode-fail", "Fail", priority = 10, actions = listOf(Action.SetDnd(DndMode.PRIORITY)))
        store.save(mode)

        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Failure("test_failure") },
        )
        val outcomes = engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowStart("evt1", AutomationId("mode-fail"), 1000L)))

        assertEquals(1, outcomes.size)
        assertTrue(outcomes[0].results.any { it is ActionResult.Failure })
    }

    @Test
    fun `routine does not snapshot or restore`() = runTest {
        val store = InMemoryAutomationStore()
        val routine = Automation(
            id = AutomationId("routine-test"),
            name = "Test Routine",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(cron = "0 12 * * *", tz = "UTC"),
            actions = listOf(Action.SetBrightness(26, restore = true)),
        )
        store.save(routine)

        val snapshots = mutableListOf<StateSnapshot>()
        val snapshotStore = object : StateSnapshotStore {
            override suspend fun save(snapshot: StateSnapshot) { snapshots.add(snapshot) }
            override suspend fun forAutomation(id: AutomationId): List<StateSnapshot> = snapshots.filter { it.automationId == id }
            override suspend fun deleteForAutomation(id: AutomationId) { snapshots.removeAll { it.automationId == id } }
        }

        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
            snapshotStore = snapshotStore,
            settingReader = SettingReader { "128" },
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.TimeFired("evt1", AutomationId("routine-test"), 1000L)))

        assertEquals(0, snapshots.size, "Routines should not snapshot settings")
    }

    @Test
    fun `mode window end without snapshots does not crash`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode("mode-empty", "Empty", priority = 10, actions = listOf(Action.SetDnd(DndMode.PRIORITY)))
        store.save(mode)

        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> ActionResult.Success },
        )
        val outcomes = engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowEnd("evt1", AutomationId("mode-empty"), 1000L)))

        assertEquals(1, outcomes.size)
    }

    @Test
    fun `current mode active condition evaluates with state`() = runTest {
        val store = InMemoryAutomationStore()
        val mode = makeMode(
            "mode-dependent", "Dependent", priority = 10,
            actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
        ).copy(
            conditions = Condition.CurrentModeActive("mode-sleep"),
        )
        store.save(mode)

        var fired = false
        val engine = Engine(
            store = store,
            executor = ActionExecutor { _, _ -> fired = true; ActionResult.Success },
            stateProvider = { DeviceState(activeModeIds = setOf("mode-sleep")) },
        )
        engine.onTrigger(envelope("evt1", TriggerEvent.ModeWindowStart("evt1", AutomationId("mode-dependent"), 1000L)))

        assertTrue(fired, "CurrentModeActive condition should be met when mode is in activeModeIds")
    }
}

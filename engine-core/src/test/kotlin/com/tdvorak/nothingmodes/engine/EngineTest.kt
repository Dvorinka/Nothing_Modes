package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationDraft
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ConditionEvaluator
import com.tdvorak.nothingmodes.engine.runtime.DeviceState
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.FirePolicy
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.NoopActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.NoopAuditSink
import com.tdvorak.nothingmodes.engine.runtime.NoopExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import com.tdvorak.nothingmodes.engine.runtime.TriggerMatcher
import com.tdvorak.nothingmodes.engine.runtime.CronSchedule
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineTest {

    @Test
    fun `action serialization round trip`() {
        val actions: List<Action> = listOf(
            Action.SetDnd(DndMode.PRIORITY),
            Action.SetDarkMode(NightMode.ON),
            Action.SetBrightness(128, restore = true),
            Action.SetExtraDim(true, restore = true),
            Action.SetScreenTimeout(30000, restore = true),
            Action.SetVolume(com.tdvorak.nothingmodes.engine.model.VolumeStream.MEDIA, 50),
            Action.SetWifi(true),
            Action.SetGlyph(true, channels = listOf(0, 1, 2)),
        )

        for (action in actions) {
            val json = EngineJson.json.encodeToString(Action.serializer(), action)
            val decoded = EngineJson.json.decodeFromString(Action.serializer(), json)
            assertEquals(action, decoded, "Round trip failed for $action")
        }
    }

    @Test
    fun `trigger serialization round trip`() {
        val triggers: List<Trigger> = listOf(
            Trigger.Time(cron = "0 8 * * *", tz = "Europe/Prague"),
            Trigger.TimeWindow(startLocal = "22:00", endLocal = "07:00", tz = "Europe/Prague"),
            Trigger.Immediate,
            Trigger.Notification(pkg = "com.whatsapp", sender = "Mom"),
            Trigger.PhoneState(
                event = com.tdvorak.nothingmodes.engine.model.PhoneEvent.SMS_RECEIVED,
                textMatch = "OTP",
            ),
            Trigger.Connectivity(
                medium = com.tdvorak.nothingmodes.engine.model.ConnMedium.WIFI,
                state = com.tdvorak.nothingmodes.engine.model.ConnState.CONNECTED,
            ),
            Trigger.Boot,
            Trigger.BatteryLevel(level = 20),
            Trigger.ScreenStateTrigger(com.tdvorak.nothingmodes.engine.model.ScreenState.OFF),
            Trigger.AppOpened("com.android.chrome"),
        )

        for (trigger in triggers) {
            val json = EngineJson.json.encodeToString(Trigger.serializer(), trigger)
            val decoded = EngineJson.json.decodeFromString(Trigger.serializer(), json)
            assertEquals(trigger, decoded, "Round trip failed for $trigger")
        }
    }

    @Test
    fun `condition serialization round trip`() {
        val conditions: List<Condition> = listOf(
            Condition.TimeWindow("08:00", "22:00", "Europe/Prague"),
            Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.LT, 20),
            Condition.Charging(true),
            Condition.WifiConnected("MyHomeWifi"),
            Condition.BluetoothConnected(),
            Condition.ScreenStateCondition(com.tdvorak.nothingmodes.engine.model.ScreenState.ON),
            Condition.CurrentModeActive("mode-work"),
            Condition.AppInForeground("com.android.chrome"),
            Condition.And(listOf(
                Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 50),
                Condition.Charging(false),
            )),
            Condition.Or(listOf(
                Condition.WifiConnected(),
                Condition.BluetoothConnected(),
            )),
            Condition.Not(Condition.Charging(true)),
        )

        for (condition in conditions) {
            val json = EngineJson.json.encodeToString(Condition.serializer(), condition)
            val decoded = EngineJson.json.decodeFromString(Condition.serializer(), json)
            assertEquals(condition, decoded, "Round trip failed for $condition")
        }
    }

    @Test
    fun `automation serialization round trip`() {
        val automation = Automation(
            id = AutomationId("test-1"),
            name = "Night Mode",
            type = AutomationType.MODE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.TimeWindow("22:00", "07:00", "Europe/Prague"),
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetDarkMode(NightMode.ON),
                Action.SetExtraDim(true, restore = true),
            ),
            conditions = Condition.And(listOf(
                Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 10),
                Condition.Not(Condition.Charging(true)),
            )),
        )

        val json = EngineJson.json.encodeToString(Automation.serializer(), automation)
        val decoded = EngineJson.json.decodeFromString(Automation.serializer(), json)
        assertEquals(automation, decoded)
    }

    @Test
    fun `engine fires on matching trigger`() = runTest {
        val store = InMemoryAutomationStore()
        val automation = Automation(
            id = AutomationId("test-1"),
            name = "Work Mode",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Boot,
            actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
        )
        store.save(automation)

        val engine = Engine(
            store = store,
            executor = NoopActionExecutor,
            audit = NoopAuditSink,
            journal = NoopExecutionJournal,
            executionIds = StableExecutionIdFactory,
        )

        val envelope = TriggerEnvelope(
            id = "evt-1",
            event = TriggerEvent.BootCompleted("evt-1"),
            receivedAtMillis = System.currentTimeMillis(),
        )

        val outcomes = engine.onTrigger(envelope)
        assertEquals(1, outcomes.size)
        assertEquals(automation.id, outcomes[0].automation.id)
        assertEquals(1, outcomes[0].results.size)
        assertTrue(outcomes[0].results[0] is ActionResult.Success)
    }

    @Test
    fun `engine skips non-matching trigger`() = runTest {
        val store = InMemoryAutomationStore()
        val automation = Automation(
            id = AutomationId("test-1"),
            name = "WiFi Connected",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Connectivity(
                medium = com.tdvorak.nothingmodes.engine.model.ConnMedium.WIFI,
                state = com.tdvorak.nothingmodes.engine.model.ConnState.CONNECTED,
            ),
            actions = listOf(Action.SetDnd(DndMode.OFF)),
        )
        store.save(automation)

        val engine = Engine(store = store, executor = NoopActionExecutor)

        // Send a boot event — should NOT match the WiFi trigger
        val envelope = TriggerEnvelope(
            id = "evt-1",
            event = TriggerEvent.BootCompleted("evt-1"),
            receivedAtMillis = System.currentTimeMillis(),
        )

        val outcomes = engine.onTrigger(envelope)
        assertEquals(0, outcomes.size)
    }

    @Test
    fun `condition evaluator - battery level`() {
        val evaluator = ConditionEvaluator()
        val state = DeviceState(batteryLevel = 15, now = System.currentTimeMillis())

        val low = Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.LT, 20)
        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(low, state))

        val high = Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 50)
        assertEquals(ConditionEvaluator.Result.NOT_MET, evaluator.result(high, state))
    }

    @Test
    fun `condition evaluator - charging`() {
        val evaluator = ConditionEvaluator()
        val charging = DeviceState(isCharging = true, now = System.currentTimeMillis())
        val notCharging = DeviceState(isCharging = false, now = System.currentTimeMillis())

        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(Condition.Charging(true), charging))
        assertEquals(ConditionEvaluator.Result.NOT_MET, evaluator.result(Condition.Charging(true), notCharging))
    }

    @Test
    fun `condition evaluator - AND composition`() {
        val evaluator = ConditionEvaluator()
        val state = DeviceState(
            batteryLevel = 80,
            isCharging = false,
            now = System.currentTimeMillis(),
        )

        val cond = Condition.And(listOf(
            Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 50),
            Condition.Charging(false),
        ))
        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(cond, state))
    }

    @Test
    fun `cron schedule parses and matches`() {
        val cron = CronSchedule("0 8 * * *", java.time.ZoneId.of("Europe/Prague"))
        val morning = java.time.ZonedDateTime.of(2026, 1, 15, 8, 0, 0, 0, java.time.ZoneId.of("Europe/Prague"))
        val afternoon = java.time.ZonedDateTime.of(2026, 1, 15, 14, 0, 0, 0, java.time.ZoneId.of("Europe/Prague"))

        assertTrue(cron.matches(morning))
        assertTrue(!cron.matches(afternoon))
    }

    @Test
    fun `cron schedule computes next fire`() {
        val cron = CronSchedule("*/15 * * * *", java.time.ZoneId.of("UTC"))
        val after = java.time.ZonedDateTime.of(2026, 1, 15, 12, 7, 0, 0, java.time.ZoneId.of("UTC"))
        val next = cron.nextFire(after)
        assertNotNull(next)
        assertEquals(15, next!!.minute)
    }
}

package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.FirePolicy
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirePolicyTest {

    private fun makeAutomation(cooldownMs: Long = 0) = Automation(
        id = AutomationId("test-1"),
        name = "Test",
        type = AutomationType.ROUTINE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = Trigger.Boot,
        actions = listOf(Action.SetDnd(DndMode.OFF)),
        cooldownMs = cooldownMs,
    )

    private val bootEvent = TriggerEvent.BootCompleted("e1")

    @Test
    fun `no cooldown always allows`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 0)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 1000L))
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 1001L))
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 2000L))
    }

    @Test
    fun `cooldown blocks within window`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 60_000)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 10_000L))
        val decision = policy.evaluate(automation, bootEvent, 30_000L)
        assertTrue(decision is FirePolicy.Decision.Block)
        assertEquals("cooldown_active", (decision as FirePolicy.Decision.Block).code)
    }

    @Test
    fun `cooldown allows after window expires`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 60_000)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 10_000L))
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 70_001L))
    }

    @Test
    fun `cooldown allows at exact boundary`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 60_000)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 10_000L))
        // At exactly 70_000 (10_000 + 60_000), now - last = 60_000 which is NOT < 60_000, so allowed
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 70_000L))
    }

    @Test
    fun `cooldown blocks at one millisecond before boundary`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 60_000)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 10_000L))
        val decision = policy.evaluate(automation, bootEvent, 69_999L)
        assertTrue(decision is FirePolicy.Decision.Block)
    }

    @Test
    fun `reset clears cooldown state`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 60_000)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 10_000L))
        assertTrue(policy.evaluate(automation, bootEvent, 20_000L) is FirePolicy.Decision.Block)
        policy.reset(automation.id)
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 20_000L))
    }

    @Test
    fun `different automations have independent cooldowns`() {
        val policy = FirePolicy()
        val a1 = makeAutomation(cooldownMs = 60_000).copy(id = AutomationId("a1"))
        val a2 = makeAutomation(cooldownMs = 60_000).copy(id = AutomationId("a2"))
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(a1, bootEvent, 10_000L))
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(a2, bootEvent, 10_000L))
        // a1 is in cooldown, a2 is also in cooldown now but they are independent
        assertTrue(policy.evaluate(a1, bootEvent, 20_000L) is FirePolicy.Decision.Block)
        assertTrue(policy.evaluate(a2, bootEvent, 20_000L) is FirePolicy.Decision.Block)
    }

    @Test
    fun `sequential fires with cooldown eventually allow again`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 1000)
        // Fire at t=0
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 0L))
        // Blocked at t=500
        assertTrue(policy.evaluate(automation, bootEvent, 500L) is FirePolicy.Decision.Block)
        // Allowed at t=1000
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 1000L))
        // Blocked at t=1500
        assertTrue(policy.evaluate(automation, bootEvent, 1500L) is FirePolicy.Decision.Block)
        // Allowed at t=2000
        assertEquals(FirePolicy.Decision.Allow, policy.evaluate(automation, bootEvent, 2000L))
    }

    @Test
    fun `block decision has needsReview default false`() {
        val policy = FirePolicy()
        val automation = makeAutomation(cooldownMs = 60_000)
        policy.evaluate(automation, bootEvent, 10_000L)
        val decision = policy.evaluate(automation, bootEvent, 20_000L)
        assertTrue(decision is FirePolicy.Decision.Block)
        assertFalse((decision as FirePolicy.Decision.Block).needsReview)
    }

    private fun assertFalse(value: Boolean) {
        org.junit.jupiter.api.Assertions.assertFalse(value)
    }
}

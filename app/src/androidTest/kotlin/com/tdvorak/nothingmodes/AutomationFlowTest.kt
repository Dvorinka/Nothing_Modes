package com.tdvorak.nothingmodes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * E2E test: create automation → store → retrieve → verify.
 * Requires a device or emulator with Hilt test infrastructure.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AutomationFlowTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var store: AutomationStore

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun createAndRetrieveAutomation() = runBlocking {
        val automation = Automation(
            id = AutomationId("test-e2e-1"),
            name = "Test Automation",
            trigger = Trigger.Immediate,
            conditions = null,
            actions = listOf(Action.SetDarkMode(NightMode.ON)),
            priority = 50,
            type = AutomationType.ROUTINE,
            status = AutomationStatus.ARMED,
            enabled = true,
            cooldownMs = 0,
        )

        store.save(automation)

        val retrieved = store.get(AutomationId("test-e2e-1"))
        assertNotNull(retrieved)
        assertEquals("Test Automation", retrieved!!.name)
        assertTrue(retrieved.enabled)
        assertEquals(AutomationType.ROUTINE, retrieved.type)

        store.delete(AutomationId("test-e2e-1"))
    }

    @Test
    fun listArmedAutomations() = runBlocking {
        val a1 = Automation(
            id = AutomationId("test-e2e-armed"),
            name = "Armed",
            trigger = Trigger.Immediate,
            conditions = null,
            actions = listOf(Action.SetDarkMode(NightMode.ON)),
            priority = 50,
            type = AutomationType.ROUTINE,
            status = AutomationStatus.ARMED,
            enabled = true,
            cooldownMs = 0,
        )
        val a2 = Automation(
            id = AutomationId("test-e2e-draft"),
            name = "Draft",
            trigger = Trigger.Immediate,
            conditions = null,
            actions = listOf(Action.SetDarkMode(NightMode.OFF)),
            priority = 50,
            type = AutomationType.ROUTINE,
            status = AutomationStatus.DRAFT,
            enabled = false,
            cooldownMs = 0,
        )

        store.save(a1)
        store.save(a2)

        val armed = store.armed()
        assertTrue(armed.any { it.id.value == "test-e2e-armed" })
        assertTrue(armed.none { it.id.value == "test-e2e-draft" })

        store.delete(AutomationId("test-e2e-armed"))
        store.delete(AutomationId("test-e2e-draft"))
    }

    @Test
    fun appContextIsCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.tdvorak.nothingmodes", context.packageName)
    }
}

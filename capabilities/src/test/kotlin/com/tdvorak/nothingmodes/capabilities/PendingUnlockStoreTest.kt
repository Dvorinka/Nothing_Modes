package com.tdvorak.nothingmodes.capabilities

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingUnlockStoreTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: PendingUnlockStore

    private val automationId = AutomationId("test-auto")
    private val action = Action.OpenUrl(url = "https://example.com")

    @Before
    fun setUp() {
        store = PendingUnlockStore(ctx)
        store.clear()
    }

    @Test
    fun `enqueue then pending round-trips the action`() {
        store.enqueue(automationId, action)
        val entries = store.pending()
        assertEquals(1, entries.size)
        assertEquals(automationId, entries[0].automationId)
        assertEquals(action, entries[0].action)
    }

    @Test
    fun `drain returns entries and clears the queue`() {
        store.enqueue(automationId, action)
        store.enqueue(automationId, Action.LaunchApp(packages = listOf("com.example")))
        assertEquals(2, store.drain().size)
        assertTrue(store.pending().isEmpty())
        assertTrue(store.drain().isEmpty())
    }

    @Test
    fun `queue survives a new store instance`() {
        store.enqueue(automationId, action)
        val reloaded = PendingUnlockStore(ctx)
        assertEquals(1, reloaded.pending().size)
    }

    @Test
    fun `corrupted payload is dropped instead of crashing`() {
        ctx
            .getSharedPreferences("pending_unlock_actions", Context.MODE_PRIVATE)
            .edit()
            .putString("queue", "{not a json array")
            .apply()
        assertTrue(store.pending().isEmpty())
    }
}

package com.tdvorak.nothingmodes.capabilities.controllers

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The retry loop without a radio. What matters: the action stops when the
 * device never answers, refuses a blank address, and caps its budget so a
 * dead watch cannot loop forever.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ConnectBluetoothDeviceTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val fire =
        FireContext(
            eventId = "test",
            executionId = "exec-1",
            automationId = AutomationId("test"),
            actionIndex = 0,
            priority = 0,
        )

    private val watch = "AA:BB:CC:DD:EE:FF"

    @Before
    fun grantBluetoothPermission() {
        shadowOf(ctx as Application).grantPermissions(android.Manifest.permission.BLUETOOTH_CONNECT)
    }

    @Test
    fun `blank address fails before touching the radio`() =
        runTest {
            val result = executor().execute(Action.ConnectBluetoothDevice(""), fire)
            assertEquals(ActionResult.Failure("no device selected"), result)
        }

    @Test
    fun `malformed address fails before touching the radio`() =
        runTest {
            val result = executor().execute(Action.ConnectBluetoothDevice("not-a-mac"), fire)
            assertEquals(ActionResult.Failure("no device selected"), result)
        }

    @Test
    fun `unreachable device fails within its budget`() =
        runTest {
            enableRadio()
            val start = System.currentTimeMillis()
            val result =
                executor().execute(
                    Action.ConnectBluetoothDevice(watch, deviceName = "Watch", retryBudgetMs = 0),
                    fire,
                )
            assertTrue(result is ActionResult.Failure)
            assertTrue(
                "a zero budget must not loop: took ${System.currentTimeMillis() - start}ms",
                System.currentTimeMillis() - start < 5_000,
            )
        }

    @Test
    fun `radio that stays off is reported, not retried`() =
        runTest {
            val adapter = shadowOf(adapter())
            adapter.setState(BluetoothAdapter.STATE_OFF)
            val result =
                executor().execute(
                    Action.ConnectBluetoothDevice(watch, enableRadio = false),
                    fire,
                )
            assertEquals(ActionResult.Failure("bluetooth is off"), result)
        }

    private fun enableRadio() {
        val adapter = adapter()
        shadowOf(adapter).setState(BluetoothAdapter.STATE_ON)
        adapter.enable()
    }

    private fun adapter(): BluetoothAdapter =
        ctx.getSystemService(BluetoothManager::class.java).adapter

    private fun executor(): RealActionExecutor = RealActionExecutor.create(ctx)
}

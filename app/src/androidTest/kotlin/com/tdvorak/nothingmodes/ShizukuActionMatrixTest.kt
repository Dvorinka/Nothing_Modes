package com.tdvorak.nothingmodes

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tdvorak.nothingmodes.capabilities.controllers.RealActionExecutor
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AodMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import com.tdvorak.nothingmodes.shizuku.PrivilegedShellFactory
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuPermissionResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device Shizuku action matrix.
 *
 * Runs the actions that usually need Shizuku through [RealActionExecutor] with
 * a live [PrivilegedShellFactory]. The result of every action is logged and the
 * test fails only if the executor crashes, so the output is a capability matrix
 * rather than a pass/fail assertion for each toggle.
 */
@RunWith(AndroidJUnit4::class)
class ShizukuActionMatrixTest {

    @Test
    fun shizukuActionsExecuteAndReportResults() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val gateway = ShizukuGateway(context)

            if (gateway.status() != com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus.AUTHORIZED) {
                val result = withTimeout(60_000) { gateway.requestPermission() }
                if (result != ShizukuPermissionResult.GRANTED) {
                    Log.w(TAG, "Shizuku not granted: ${gateway.status()}; matrix will show fallback results")
                }
            }

            val shellFactory = PrivilegedShellFactory(context, gateway)
            val executor = RealActionExecutor.create(context, shellFactory = shellFactory)

            val fireContext = FireContext(
                eventId = "shizuku-matrix",
                executionId = "matrix-1",
                automationId = AutomationId("shizuku-matrix"),
                actionIndex = 0,
                priority = 50,
            )

            val actions = listOf(
                Action.SetWifi(true),
                Action.SetWifi(false),
                Action.SetBluetooth(true),
                Action.SetBluetooth(false),
                Action.SetMobileData(false),
                Action.SetMobileData(true),
                Action.SetAirplaneMode(false),
                Action.SetDataSaver(false),
                Action.SetHotspot(false),
                Action.SetNfc(false),
                Action.SetAutoSync(false),
                Action.SetLocationMode(LocationMode.OFF),
                Action.SetAlwaysOnDisplay(AodMode.OFF),
                Action.SetBatterySaver(false),
                Action.SetExtraDim(false),
                Action.WriteSetting(com.tdvorak.nothingmodes.engine.model.SettingNamespace.GLOBAL, "mobile_data", "1"),
            )

            for ((index, action) in actions.withIndex()) {
                val result = executor.execute(action, fireContext.copy(actionIndex = index))
                Log.i(TAG, "[${action::class.simpleName}] -> $result")
            }

            gateway.close()
        }

    companion object {
        private const val TAG = "ShizukuMatrix"
    }
}

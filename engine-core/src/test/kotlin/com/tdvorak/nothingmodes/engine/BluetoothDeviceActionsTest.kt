package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.Trigger
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Bluetooth device control actions: serialization, capability derivation. */
class BluetoothDeviceActionsTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(action: Action): Action =
        json.decodeFromString(Action.serializer(), json.encodeToString(Action.serializer(), action))

    @Test
    fun `disconnect and forget survive serialization round-trip`() {
        val actions =
            listOf(
                Action.DisconnectBluetoothDevice(address = "AA:BB:CC:DD:EE:FF", deviceName = "Earbuds"),
                Action.ForgetBluetoothDevice(address = "AA:BB:CC:DD:EE:FF", deviceName = "Earbuds"),
            )
        actions.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `serial names are stable`() {
        val disconnect = json.encodeToString(Action.serializer(), Action.DisconnectBluetoothDevice("x"))
        assertTrue(disconnect.contains("\"type\":\"disconnect_bluetooth_device\""))
        val forget = json.encodeToString(Action.serializer(), Action.ForgetBluetoothDevice("x"))
        assertTrue(forget.contains("\"type\":\"forget_bluetooth_device\""))
    }

    @Test
    fun `both need shizuku`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                listOf(Action.DisconnectBluetoothDevice("x"), Action.ForgetBluetoothDevice("x")),
            )
        assertTrue(CapabilityIds.SHIZUKU_REQUIRED in caps)
        assertTrue(CapabilityIds.ACTION_DISCONNECT_BLUETOOTH_DEVICE in caps)
        assertTrue(CapabilityIds.ACTION_FORGET_BLUETOOTH_DEVICE in caps)
    }
}

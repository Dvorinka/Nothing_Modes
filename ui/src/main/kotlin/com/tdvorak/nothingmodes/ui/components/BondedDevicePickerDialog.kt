package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingListRow

/**
 * Lists the phone's bonded Bluetooth devices. Reached through a PermissionGate
 * by callers, but still distinguishes "permission denied" and "no adapter"
 * from a genuinely empty bond table instead of pretending the list is empty.
 */
@Composable
fun BondedDevicePickerDialog(
    onSelect: (name: String?, address: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Read once at composition; a SecurityException becomes a distinct state
    // instead of silently looking like "no paired devices".
    val devicesResult =
        remember {
            runCatching {
                val adapter =
                    (
                        context.getSystemService(android.content.Context.BLUETOOTH_SERVICE)
                            as? android.bluetooth.BluetoothManager
                    )?.adapter
                adapter?.bondedDevices?.map { it.name to it.address }
                    ?: throw IllegalStateException("no_adapter")
            }
        }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Paired devices", fontFamily = NothingFonts.mono())
        },
        text = {
            Column {
                NothingListRow(
                    title = "Any device",
                    subtitle = "Matches every Bluetooth device",
                    onClick = { onSelect(null, null) },
                )
                devicesResult
                    .onSuccess { devices ->
                        if (devices.isEmpty()) {
                            Text(
                                text = "No paired devices found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                        } else {
                            devices.forEach { (name, address) ->
                                NothingListRow(
                                    title = name ?: address,
                                    subtitle = address,
                                    onClick = { onSelect(name, address) },
                                )
                            }
                        }
                    }.onFailure { e ->
                        Text(
                            text =
                                when (e) {
                                    is SecurityException ->
                                        "Bluetooth permission missing — grant nearby-devices access, then reopen this picker."
                                    else ->
                                        "Couldn't read paired devices — is Bluetooth available on this phone?"
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

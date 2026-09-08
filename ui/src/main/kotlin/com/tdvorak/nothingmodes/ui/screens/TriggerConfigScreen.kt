package com.tdvorak.nothingmodes.ui.screens

import android.annotation.SuppressLint
import android.content.pm.PackageManager

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.BatteryDirection
import com.tdvorak.nothingmodes.engine.model.CalendarDirection
import com.tdvorak.nothingmodes.engine.model.ChargerSource
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.Transition
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.components.CustomTimePicker
import com.tdvorak.nothingmodes.ui.components.NothingDaySelector
import com.tdvorak.nothingmodes.ui.components.NothingTimeField
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private data class TriggerType(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val trigger: Trigger,
)

private fun triggerTypes(): List<TriggerType> =
    listOf(
        TriggerType("Time / Day", "Schedule", Icons.Outlined.Schedule, Trigger.Time(cron = "0 12 * * *", tz = defaultTimeZone())),
        TriggerType("Time window", "Schedule", Icons.Outlined.Alarm, Trigger.TimeWindow("22:00", "07:00", defaultTimeZone())),
        TriggerType("Manual", "Manual", Icons.Outlined.TouchApp, Trigger.Manual),
        TriggerType("Boot", "Device", Icons.Outlined.PowerSettingsNew, Trigger.Boot),
        TriggerType("Screen", "Device", Icons.Outlined.Devices, Trigger.ScreenStateTrigger(ScreenState.ON)),
        TriggerType("Battery", "Device", Icons.Outlined.BatteryFull, Trigger.BatteryLevel(20, BatteryDirection.CHARGING_STARTED)),
        TriggerType("Charger", "Device", Icons.Outlined.BatteryChargingFull, Trigger.ChargerConnected()),
        TriggerType("Device unlocked", "Device", Icons.Outlined.LockOpen, Trigger.DeviceUnlocked),
        TriggerType("Device locked", "Device", Icons.Outlined.Lock, Trigger.DeviceLocked),
        TriggerType("App opened", "Apps", Icons.Outlined.Apps, Trigger.AppOpened("")),
        TriggerType("Notification", "Apps", Icons.Outlined.Notifications, Trigger.Notification("")),
        TriggerType("Phone", "Connections", Icons.Outlined.Phone, Trigger.PhoneState(PhoneEvent.INCOMING_CALL)),
        TriggerType("Connectivity", "Connections", Icons.Outlined.Wifi, Trigger.Connectivity(ConnMedium.WIFI, ConnState.CONNECTED)),
        TriggerType("WiFi", "Connections", Icons.Outlined.Wifi, Trigger.WifiConnected()),
        TriggerType("Bluetooth", "Connections", Icons.Outlined.Bluetooth, Trigger.BluetoothDevice(ConnState.CONNECTED)),
        TriggerType("Geofence", "Location", Icons.Outlined.LocationOn, Trigger.Geofence(0.0, 0.0, 100.0, Transition.ENTER)),
    )

@Composable
fun TriggerConfigScreen(
    triggerJson: String,
    navController: NavController,
) {
    val initial =
        remember(triggerJson) {
            runCatching { Json.decodeFromString<Trigger>(triggerJson) }.getOrNull()
                ?: Trigger.Manual
        }
    var trigger by remember { mutableStateOf(initial) }
    var showTypePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Configure Trigger",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(NothingSpacing.md),
        ) {
            NothingCardLarge(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(NothingSpacing.lg))

                // Type row: current trigger type, tap to open the picker dialog.
                // The config fields stay directly below so nothing needs scrolling.
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showTypePicker = true }
                            .padding(vertical = NothingSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NothingLabel(text = "Type")
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = triggerTypeLabel(trigger).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = NothingFonts.mono(),
                            maxLines = 2,
                            softWrap = true,
                            textAlign = TextAlign.End,
                        )
                        Spacer(modifier = Modifier.width(NothingSpacing.sm))
                        Text(
                            text = "CHANGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingColors.accent,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }

                com.tdvorak.nothingmodes.ui.theme
                    .NothingDivider()

                TriggerConfigContent(
                    trigger = trigger,
                    onUpdate = { trigger = it },
                )

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
                NothingPillButton(
                    text = "Done",
                    onClick = {
                        val result = Json.encodeToString(trigger)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("trigger_result", result)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showTypePicker) {
                TriggerTypePickerDialog(
                    selected = trigger,
                    onSelect = { trigger = it },
                    onDismiss = { showTypePicker = false },
                )
            }
        }
    }
}

/** Short label for the currently selected trigger type. */
private fun triggerTypeLabel(trigger: Trigger): String =
    triggerTypes().firstOrNull { it.trigger::class == trigger::class }?.label
        ?: triggerDescription(trigger)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerTypePickerDialog(
    selected: Trigger,
    onSelect: (Trigger) -> Unit,
    onDismiss: () -> Unit,
) {
    val types = remember { triggerTypes() }
    val grouped = types.groupBy { it.category }

    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = NothingShapes.dialog,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(NothingSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TRIGGER TYPE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.mono(),
                    )
                    Text(
                        text = "CLOSE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                }
                com.tdvorak.nothingmodes.ui.theme
                    .NothingDivider()

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    grouped.forEach { (category, items) ->
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                            modifier =
                                Modifier.padding(
                                    start = NothingSpacing.md,
                                    top = NothingSpacing.sm,
                                    bottom = NothingSpacing.xs,
                                ),
                        )
                        items.forEach { type ->
                            val isSelected = selected::class == type.trigger::class
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelect(type.trigger)
                                            onDismiss()
                                        }.padding(
                                            horizontal = NothingSpacing.md,
                                            vertical = NothingSpacing.sm,
                                        ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                            ) {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = type.label,
                                    tint =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    fontFamily = NothingFonts.mono(),
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = NothingFonts.mono(),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                }
            }
        }
    }
}

@Composable
private fun TriggerConfigContent(
    trigger: Trigger,
    onUpdate: (Trigger) -> Unit,
) {
    when (val t = trigger) {
        is Trigger.Time ->
            CustomTimePicker(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.TimeWindow ->
            TimeWindowContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.Immediate,
        is Trigger.Manual,
        is Trigger.Boot,
        is Trigger.DeviceUnlocked,
        is Trigger.DeviceLocked,
        -> {
            Text(
                text = triggerDescription(trigger),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }

        is Trigger.ScreenStateTrigger ->
            ScreenStateContent(
                state = t.state,
                onUpdate = { onUpdate(t.copy(state = it)) },
            )

        is Trigger.BatteryLevel ->
            BatteryLevelContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.ChargerConnected ->
            ChargerConnectedContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.AppOpened ->
            AppPicker(
                currentPackage = t.pkg,
                onPkgChange = { onUpdate(t.copy(pkg = it)) },
            )

        is Trigger.Notification ->
            NotificationContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.PhoneState ->
            PhoneStateContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.Connectivity ->
            ConnectivityContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.WifiConnected ->
            WifiConnectedContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.BluetoothDevice ->
            BluetoothDeviceContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.Geofence ->
            GeofenceContent(
                trigger = t,
                onUpdate = onUpdate,
            )

        is Trigger.CalendarEvent ->
            CalendarEventContent(
                trigger = t,
                onUpdate = onUpdate,
            )
    }
}

@Composable
private fun TimeWindowContent(
    trigger: Trigger.TimeWindow,
    onUpdate: (Trigger.TimeWindow) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            NothingTimeField(
                label = "Starts",
                value = trigger.startLocal,
                onValueChange = { onUpdate(trigger.copy(startLocal = it)) },
                modifier = Modifier.weight(1f),
            )
            NothingTimeField(
                label = "Ends",
                value = trigger.endLocal,
                onValueChange = { onUpdate(trigger.copy(endLocal = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        NothingLabel(text = "Days (none selected = every day)")
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        NothingDaySelector(
            selected = (trigger.days ?: emptyList()).toSet(),
            onChange = { days ->
                onUpdate(
                    trigger.copy(
                        days = DayOfWeek.entries.filter { it in days }.ifEmpty { null },
                    ),
                )
            },
        )
    }
}

@Composable
private fun ScreenStateContent(
    state: ScreenState,
    onUpdate: (ScreenState) -> Unit,
) {
    ScreenState.entries.forEach { option ->
        RadioOption(
            text = option.name.enumLabel(),
            selected = state == option,
            onClick = { onUpdate(option) },
        )
    }
}

@Composable
private fun BatteryLevelContent(
    trigger: Trigger.BatteryLevel,
    onUpdate: (Trigger.BatteryLevel) -> Unit,
) {
    Column {
        NothingInput(
            value = trigger.level.toString(),
            onValueChange = { onUpdate(trigger.copy(level = it.toIntOrNull() ?: trigger.level)) },
            label = "Level (%)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "Direction",
            value = (trigger.direction ?: BatteryDirection.CHARGING_STARTED).name.enumLabel(),
            options = enumLabelList<BatteryDirection>(),
            onSelect = { dir ->
                onUpdate(trigger.copy(direction = enumByLabel<BatteryDirection>(dir)))
            },
        )
    }
}

@Composable
private fun ChargerConnectedContent(
    trigger: Trigger.ChargerConnected,
    onUpdate: (Trigger.ChargerConnected) -> Unit,
) {
    Column {
        BooleanRow(
            label = if (trigger.connected) "On connect" else "On disconnect",
            checked = trigger.connected,
            onChange = { onUpdate(trigger.copy(connected = it)) },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "Source (optional)",
            value = trigger.source?.name?.enumLabel() ?: "ANY",
            options = listOf("ANY") + enumLabelList<ChargerSource>(),
            onSelect = { src ->
                onUpdate(
                    trigger.copy(
                        source = if (src == "ANY") null else enumByLabel<ChargerSource>(src),
                    ),
                )
            },
        )
    }
}

@Composable
private fun NotificationContent(
    trigger: Trigger.Notification,
    onUpdate: (Trigger.Notification) -> Unit,
) {
    Column {
        NothingInput(
            value = trigger.pkg,
            onValueChange = { onUpdate(trigger.copy(pkg = it)) },
            label = "Package name",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.titleMatch ?: "",
            onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
            label = "Title contains",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.textMatch ?: "",
            onValueChange = { onUpdate(trigger.copy(textMatch = it.ifBlank { null })) },
            label = "Text contains",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.sender ?: "",
            onValueChange = { onUpdate(trigger.copy(sender = it.ifBlank { null })) },
            label = "Sender",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        BooleanRow(
            label = "Group conversation",
            checked = trigger.isGroup == true,
            onChange = { onUpdate(trigger.copy(isGroup = if (it) true else null)) },
        )
    }
}

@Composable
private fun PhoneStateContent(
    trigger: Trigger.PhoneState,
    onUpdate: (Trigger.PhoneState) -> Unit,
) {
    Column {
        PhoneEvent.entries.forEach { event ->
            RadioOption(
                text = event.name.enumLabel(),
                selected = trigger.event == event,
                onClick = { onUpdate(trigger.copy(event = event)) },
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.number ?: "",
            onValueChange = { onUpdate(trigger.copy(number = it.ifBlank { null })) },
            label = "Number (optional)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.textMatch ?: "",
            onValueChange = { onUpdate(trigger.copy(textMatch = it.ifBlank { null })) },
            label = "SMS text contains",
        )
    }
}

@Composable
private fun ConnectivityContent(
    trigger: Trigger.Connectivity,
    onUpdate: (Trigger.Connectivity) -> Unit,
) {
    Column {
        NothingEnumSelector(
            label = "Medium",
            value = trigger.medium.name.enumLabel(),
            options = enumLabelList<ConnMedium>(),
            onSelect = { onUpdate(trigger.copy(medium = enumByLabel(it))) },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "State",
            value = trigger.state.name.enumLabel(),
            options = enumLabelList<ConnState>(),
            onSelect = { onUpdate(trigger.copy(state = enumByLabel(it))) },
        )
        if (trigger.medium == ConnMedium.WIFI) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.match ?: "",
                onValueChange = { onUpdate(trigger.copy(match = it.ifBlank { null })) },
                label = "SSID (blank = any)",
            )
        }
    }
}

@Composable
private fun BluetoothDeviceContent(
    trigger: Trigger.BluetoothDevice,
    onUpdate: (Trigger.BluetoothDevice) -> Unit,
) {
    val context = LocalContext.current
    var showDevicePicker by remember { mutableStateOf(false) }
    Column {
        NothingEnumSelector(
            label = "State",
            value = trigger.state.name.enumLabel(),
            options = enumLabelList<ConnState>(),
            onSelect = { onUpdate(trigger.copy(state = enumByLabel(it))) },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        // Paired-device picker — no system picker exists, so list bonded devices.
        NothingPillButton(
            text = "Pick paired device",
            onClick = { showDevicePicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.deviceName ?: "",
            onValueChange = { onUpdate(trigger.copy(deviceName = it.ifBlank { null })) },
            label = "Device name (blank = any)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.deviceAddress ?: "",
            onValueChange = { onUpdate(trigger.copy(deviceAddress = it.ifBlank { null })) },
            label = "MAC address (blank = any)",
        )
    }
    if (showDevicePicker) {
        BondedDevicePickerDialog(
            onSelect = { name, address ->
                onUpdate(
                    trigger.copy(
                        deviceName = name,
                        deviceAddress = address,
                    ),
                )
                showDevicePicker = false
            },
            onDismiss = { showDevicePicker = false },
        )
    }
}

@Composable
private fun BondedDevicePickerDialog(
    onSelect: (name: String?, address: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val devices =
        remember {
            runCatching {
                val adapter =
                    (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE)
                        as? android.bluetooth.BluetoothManager)?.adapter
                @SuppressLint("MissingPermission")
                adapter?.bondedDevices?.map { it.name to it.address } ?: emptyList()
            }.getOrDefault(emptyList())
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

@Composable
private fun WifiConnectedContent(
    trigger: Trigger.WifiConnected,
    onUpdate: (Trigger.WifiConnected) -> Unit,
) {
    val context = LocalContext.current
    Column {
        // Android exposes no system Wi-Fi picker — offer the current network
        // as a one-tap fill, manual entry stays for anything else.
        NothingPillButton(
            text = "Use current network",
            onClick = {
                val ssid = currentSsid(context)
                if (ssid != null) {
                    onUpdate(trigger.copy(ssid = ssid))
                } else {
                    Toast.makeText(context, "No Wi-Fi network connected", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.ssid ?: "",
            onValueChange = { onUpdate(trigger.copy(ssid = it.ifBlank { null })) },
            label = "Network name / SSID (blank = any)",
        )
    }
}

/** SSID of the connected Wi-Fi network, or null. Needs location permission. */
private fun currentSsid(context: android.content.Context): String? =
    runCatching {
        val granted =
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        ssidFromConnectivity(context) ?: ssidFromWifiManager(context)
    }.getOrNull()

private fun ssidFromConnectivity(context: android.content.Context): String? {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return null
    val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return null
    val network = cm.activeNetwork ?: return null
    val caps = cm.getNetworkCapabilities(network) ?: return null
    val info = caps.transportInfo as? android.net.wifi.WifiInfo ?: return null
    return info.ssid?.takeUnless { it == android.net.wifi.WifiManager.UNKNOWN_SSID }?.removeSurrounding("\"")
}

@Suppress("DEPRECATION")
private fun ssidFromWifiManager(context: android.content.Context): String? {
    val wm = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    return wm?.connectionInfo?.ssid?.takeUnless { it == android.net.wifi.WifiManager.UNKNOWN_SSID }?.removeSurrounding("\"")
}

@Composable
private fun GeofenceContent(
    trigger: Trigger.Geofence,
    onUpdate: (Trigger.Geofence) -> Unit,
) {
    val context = LocalContext.current
    val mapView = remember { org.osmdroid.views.MapView(context) }
    var fenceOverlay by remember { mutableStateOf<org.osmdroid.views.overlay.Polygon?>(null) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
            userAgentValue = context.packageName
        }
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(
            org.osmdroid.util.GeoPoint(
                if (trigger.lat == 0.0 && trigger.lng == 0.0) 50.0755 else trigger.lat,
                if (trigger.lat == 0.0 && trigger.lng == 0.0) 14.4378 else trigger.lng,
            ),
        )
        // Tap on the map moves the fence center.
        mapView.overlays.add(
            object : org.osmdroid.views.overlay.Overlay() {
                override fun onSingleTapConfirmed(
                    e: android.view.MotionEvent,
                    mv: org.osmdroid.views.MapView,
                ): Boolean {
                    val p = mv.projection.fromPixels(e.x.toInt(), e.y.toInt()) as org.osmdroid.util.GeoPoint
                    onUpdate(trigger.copy(lat = p.latitude, lng = p.longitude))
                    return true
                }
            },
        )
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // Redraw the fence circle whenever the center or radius changes.
    androidx.compose.runtime.LaunchedEffect(trigger.lat, trigger.lng, trigger.radiusM) {
        if (trigger.lat != 0.0 || trigger.lng != 0.0) {
            val center = org.osmdroid.util.GeoPoint(trigger.lat, trigger.lng)
            fenceOverlay?.let { mapView.overlays.remove(it) }
            val poly =
                org.osmdroid.views.overlay.Polygon(mapView).apply {
                    points = org.osmdroid.views.overlay.Polygon.pointsAsCircle(center, trigger.radiusM)
                    fillPaint.color = android.graphics.Color.argb(40, 255, 60, 60)
                    outlinePaint.color = android.graphics.Color.rgb(255, 60, 60)
                    outlinePaint.strokeWidth = 4f
                }
            mapView.overlays.add(poly)
            fenceOverlay = poly
            mapView.controller.animateTo(center)
            mapView.invalidate()
        }
    }

    Column {
        Text(
            text = "Tap the map to place the fence.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { mapView },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(NothingShapes.input)
                    .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.input),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        // "Use current location" button — fetches last known location from FusedLocationProvider.
        NothingPillButton(
            text = "Use current location",
            onClick = {
                runCatching {
                    val fusedLocationClient =
                        com.google.android.gms.location.LocationServices
                            .getFusedLocationProviderClient(context)
                    if (context.checkSelfPermission(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { location ->
                                if (location != null) {
                                    onUpdate(trigger.copy(lat = location.latitude, lng = location.longitude))
                                }
                            }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(NothingSpacing.sm))

        // Show coordinates as read-only text (set by the button above or manually if needed).
        Text(
            text = "Location: ${String.format("%.4f", trigger.lat)}, ${String.format("%.4f", trigger.lng)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.radiusM.toString(),
            onValueChange = { onUpdate(trigger.copy(radiusM = it.toDoubleOrNull() ?: trigger.radiusM)) },
            label = "Radius (m)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "Transition",
            value = trigger.transition.name.enumLabel(),
            options = enumLabelList<Transition>(),
            onSelect = { onUpdate(trigger.copy(transition = enumByLabel(it))) },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.loiteringDelayMs.toString(),
            onValueChange = { onUpdate(trigger.copy(loiteringDelayMs = it.toLongOrNull() ?: trigger.loiteringDelayMs)) },
            label = "Loitering delay (ms)",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarEventContent(
    trigger: Trigger.CalendarEvent,
    onUpdate: (Trigger.CalendarEvent) -> Unit,
) {
    val context = LocalContext.current
    var showCalendarPicker by remember { mutableStateOf(false) }
    Column {
        NothingInput(
            value = trigger.titleMatch ?: "",
            onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
            label = "Title contains (blank = any)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingPillButton(
            text = "Pick calendar",
            onClick = { showCalendarPicker = true },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.calendarId ?: "",
            onValueChange = { onUpdate(trigger.copy(calendarId = it.ifBlank { null })) },
            label = "Calendar ID (blank = any)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "Direction",
            value = trigger.direction.name.enumLabel(),
            options = enumLabelList<CalendarDirection>(),
            onSelect = { onUpdate(trigger.copy(direction = enumByLabel(it))) },
        )
    }
    if (showCalendarPicker) {
        CalendarPickerDialog(
            onSelect = { id ->
                onUpdate(trigger.copy(calendarId = id))
                showCalendarPicker = false
            },
            onDismiss = { showCalendarPicker = false },
        )
    }
}

/** Device calendars via CalendarContract — needs READ_CALENDAR. */
@Composable
private fun CalendarPickerDialog(
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val calendars =
        remember {
            runCatching {
                val granted =
                    context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) ==
                        PackageManager.PERMISSION_GRANTED
                if (!granted) return@runCatching emptyList<Pair<String, String>>()
                val out = mutableListOf<Pair<String, String>>()
                context.contentResolver
                    .query(
                        android.provider.CalendarContract.Calendars.CONTENT_URI,
                        arrayOf(
                            android.provider.CalendarContract.Calendars._ID,
                            android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                        ),
                        null,
                        null,
                        null,
                    )?.use { c ->
                        while (c.moveToNext()) {
                            out += c.getString(0) to (c.getString(1) ?: "Calendar")
                        }
                    }
                out
            }.getOrDefault(emptyList())
        }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Calendars", fontFamily = NothingFonts.mono()) },
        text = {
            Column {
                NothingListRow(
                    title = "Any calendar",
                    subtitle = "Matches every calendar",
                    onClick = { onSelect(null) },
                )
                if (calendars.isEmpty()) {
                    Text(
                        text = "No calendars found — grant the calendar permission in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                } else {
                    calendars.forEach { (id, name) ->
                        NothingListRow(
                            title = name,
                            subtitle = "ID $id",
                            onClick = { onSelect(id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

@Composable
private fun BooleanRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(vertical = NothingSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = NothingFonts.mono(),
        )
        NothingToggle(
            checked = checked,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun NothingLabel(text: String) {
    com.tdvorak.nothingmodes.ui.theme
        .NothingLabel(text = text)
}



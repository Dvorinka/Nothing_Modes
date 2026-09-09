package com.tdvorak.nothingmodes.ui.screens

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.CapabilityResolver
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
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
import com.tdvorak.nothingmodes.ui.components.ContactNumberPickerButton
import com.tdvorak.nothingmodes.ui.components.CustomTimePicker
import com.tdvorak.nothingmodes.ui.components.NothingDaySelector
import com.tdvorak.nothingmodes.ui.components.NothingTimeField
import com.tdvorak.nothingmodes.ui.components.PermissionGate
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
        TriggerType("Torch", "Device", Icons.Outlined.FlashlightOn, Trigger.TorchState()),
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
    val context = LocalContext.current
    val initial =
        remember(triggerJson) {
            runCatching { Json.decodeFromString<Trigger>(triggerJson) }.getOrNull()
                ?: Trigger.Manual
        }
    var trigger by remember { mutableStateOf(initial) }
    var showTypePicker by remember { mutableStateOf(false) }
    var caps by remember { mutableStateOf(DeviceCapabilities()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { caps = CapabilityDetector(context).detect() }
    }

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
                    caps = caps,
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
    caps: DeviceCapabilities,
) {
    val types = remember { triggerTypes() }
    val grouped = types.groupBy { it.category }
    val resolver = remember(caps) { CapabilityResolver(caps) }
    var pendingType by remember { mutableStateOf<TriggerType?>(null) }

    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.md),
        properties =
            androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
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

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(480.dp),
                ) {
                    grouped.forEach { (category, groupItems) ->
                        item {
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
                        }
                        items(groupItems, key = { it.label }) { type ->
                            val isSelected = selected::class == type.trigger::class
                            val hint = triggerCapabilityHint(type.trigger, caps)
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = NothingSpacing.md,
                                            vertical = NothingSpacing.sm,
                                        )
                                        .clickable {
                                            val required = CapabilityRequirements.derive(type.trigger, emptyList())
                                            if (resolver.resolve(type.label, required).canRun) {
                                                onSelect(type.trigger)
                                                onDismiss()
                                            } else {
                                                pendingType = type
                                            }
                                        },
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                    )
                                    if (hint.isNotEmpty()) {
                                        Text(
                                            text = hint,
                                            style = MaterialTheme.typography.labelSmall,
                                            color =
                                                if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            fontFamily = NothingFonts.mono(),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                    }
                                }
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
                    item {
                        Spacer(modifier = Modifier.height(NothingSpacing.md))
                    }
                }
            }
        }
    }

    pendingType?.let { type ->
        val required = CapabilityRequirements.derive(type.trigger, emptyList())
        val resolution = resolver.resolve(type.label, required)
        CapabilityWarningDialog(
            missingReasons = resolution.missingReasons.values.distinct(),
            onDismiss = { pendingType = null },
            onConfirm = {
                onSelect(type.trigger)
                pendingType = null
                onDismiss()
            },
        )
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

        is Trigger.TorchState ->
            TorchStateContent(
                trigger = t,
                onUpdate = onUpdate,
            )

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
private fun TorchStateContent(
    trigger: Trigger.TorchState,
    onUpdate: (Trigger.TorchState) -> Unit,
) {
    Column {
        BooleanRow(
            label = "When torch turns on",
            checked = trigger.on,
            onChange = { onUpdate(trigger.copy(on = it)) },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Fires when the camera flashlight turns ${if (trigger.on) "on" else "off"}. No camera permission is needed to read the state.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val isSms = trigger.event == PhoneEvent.SMS_RECEIVED
    Column {
        PhoneEvent.entries.forEach { event ->
            RadioOption(
                text = event.name.enumLabel(),
                selected = trigger.event == event,
                onClick = {
                    val base = trigger.copy(event = event)
                    onUpdate(if (event == PhoneEvent.SMS_RECEIVED) base else base.copy(textMatch = null))
                },
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.number ?: "",
            onValueChange = { onUpdate(trigger.copy(number = it.ifBlank { null })) },
            label = if (isSms) "Sender number (optional)" else "Caller number (optional)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        ContactNumberPickerButton(
            onNumber = { onUpdate(trigger.copy(number = it)) },
            text = "Pick contact",
        )
        if (isSms) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.textMatch ?: "",
                onValueChange = { onUpdate(trigger.copy(textMatch = it.ifBlank { null })) },
                label = "SMS text contains",
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text =
                if (isSms) {
                    "Matches when an SMS arrives with the given sender and text. Leave both empty to match every SMS."
                } else {
                    "Matches when a call changes to ${trigger.event.name.enumLabel().lowercase()}. Leave number empty to match any caller."
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun triggerCapabilityHint(
    trigger: Trigger,
    caps: DeviceCapabilities,
): String {
    val static = triggerDescription(trigger)
    val required = CapabilityRequirements.derive(trigger, emptyList())
    val resolution = CapabilityResolver(caps).resolve("", required)
    return if (!resolution.canRun) {
        resolution.missingReasons.values.firstOrNull() ?: static
    } else {
        ""
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
        PermissionGate(
            permissions =
                listOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            rationale = "Geofence triggers need location access to place the fence and use your current location.",
            modifier = Modifier.fillMaxWidth(),
        ) {
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
                        val fine =
                            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        val coarse =
                            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (!fine && !coarse) return@NothingPillButton

                        runCatching {
                            val fusedLocationClient =
                                com.google.android.gms.location.LocationServices
                                    .getFusedLocationProviderClient(context)
                            fusedLocationClient.lastLocation
                                .addOnSuccessListener { location ->
                                    if (location != null) {
                                        onUpdate(trigger.copy(lat = location.latitude, lng = location.longitude))
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

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

private data class UpcomingEvent(
    val eventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val calendarId: String,
    val calendarName: String,
)

private fun loadCalendarName(context: android.content.Context, calendarId: String): String? {
    if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
    return runCatching {
        context.contentResolver
            .query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calendarId),
                null,
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
    }.getOrNull()
}

private fun loadUpcomingEvents(context: android.content.Context, calendarId: String?, limit: Int = 20): List<UpcomingEvent> {
    val granted = context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    if (!granted) return emptyList()

    val now = System.currentTimeMillis()
    val windowEnd = now + 30L * 24 * 60 * 60 * 1000

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now)
    ContentUris.appendId(builder, windowEnd)

    val projection =
        arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

    val selection = calendarId?.let { "${CalendarContract.Instances.CALENDAR_ID} = ?" }
    val selectionArgs = calendarId?.let { arrayOf(it) }

    return runCatching {
        val out = mutableListOf<UpcomingEvent>()
        context.contentResolver.query(
            builder.build(),
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val id = c.getLong(0)
                val title = c.getString(1) ?: "(no title)"
                val begin = c.getLong(2)
                val end = c.getLong(3)
                val calId = c.getString(4) ?: "0"
                val calName = c.getString(5) ?: "Calendar"
                out += UpcomingEvent(id, title, begin, end, calId, calName)
            }
        }
        out
    }.getOrDefault(emptyList())
}

private fun formatEventTime(millis: Long): String {
    val zdt = java.time.ZonedDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault(),
    )
    return zdt.format(
        java.time.format.DateTimeFormatter.ofPattern("MMM d HH:mm"),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarEventContent(
    trigger: Trigger.CalendarEvent,
    onUpdate: (Trigger) -> Unit,
) {
    val context = LocalContext.current
    var showCalendarPicker by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf(emptyList<UpcomingEvent>()) }
    var calendarName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trigger.calendarId) {
        withContext(Dispatchers.IO) {
            events = loadUpcomingEvents(context, trigger.calendarId)
            calendarName = trigger.calendarId?.let { loadCalendarName(context, it) }
        }
    }

    val sourceOptions = listOf("From calendar", "Clock")
    NothingEnumSelector(
        label = "Source",
        value = "From calendar",
        options = sourceOptions,
        onSelect = { if (it == "Clock") onUpdate(Trigger.Time(cron = "0 12 * * *", tz = defaultTimeZone())) },
    )
    Spacer(modifier = Modifier.height(NothingSpacing.md))

    PermissionGate(
        permissions = listOf(android.Manifest.permission.READ_CALENDAR),
        rationale = "Calendar triggers need read access to your calendars.",
    ) {
        Column {
            val pickerText =
                when {
                    trigger.calendarId == null -> "All calendars"
                    calendarName != null -> "Calendar: $calendarName"
                    else -> "Calendar: ${trigger.calendarId}"
                }
            NothingPillButton(
                text = pickerText,
                onClick = { showCalendarPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.titleMatch ?: "",
                onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
                label = "Title contains (blank = any)",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingEnumSelector(
                label = "Direction",
                value = trigger.direction.name.enumLabel(),
                options = enumLabelList<CalendarDirection>(),
                onSelect = { onUpdate(trigger.copy(direction = enumByLabel(it))) },
            )

            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingLabel(text = "Upcoming events (tap to use)")
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                events.forEach { event ->
                    NothingListRow(
                        title = event.title,
                        subtitle = "${formatEventTime(event.startMillis)} · ${event.calendarName}",
                        onClick = {
                            onUpdate(
                                trigger.copy(
                                    titleMatch = event.title,
                                    calendarId = event.calendarId,
                                ),
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                }
            } else if (trigger.calendarId != null) {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                Text(
                    text = "No upcoming events for this calendar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }
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
            PermissionGate(
                permissions = listOf(android.Manifest.permission.READ_CALENDAR),
                rationale = "Calendar triggers need read access to your calendars.",
            ) {
                Column {
                    NothingListRow(
                        title = "Any calendar",
                        subtitle = "Matches every calendar",
                        onClick = { onSelect(null) },
                    )
                    if (calendars.isEmpty()) {
                        Text(
                            text = "No calendars found.",
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



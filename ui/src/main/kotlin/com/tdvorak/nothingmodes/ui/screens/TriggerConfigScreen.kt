package com.tdvorak.nothingmodes.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.BatteryDirection
import com.tdvorak.nothingmodes.engine.model.CalendarDirection
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.Transition
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.components.CustomTimePicker
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTag
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private data class TriggerType(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val trigger: Trigger,
)

private fun triggerTypes(): List<TriggerType> = listOf(
    TriggerType("Time", "Schedule", Icons.Outlined.Schedule, Trigger.Time(cron = "0 12 * * *", tz = defaultTimeZone())),
    TriggerType("Time window", "Schedule", Icons.Outlined.Alarm, Trigger.TimeWindow("22:00", "07:00", defaultTimeZone())),
    TriggerType("Manual", "Manual", Icons.Outlined.TouchApp, Trigger.Manual),
    TriggerType("Immediate", "Manual", Icons.Outlined.PowerSettingsNew, Trigger.Immediate),
    TriggerType("Boot", "Device", Icons.Outlined.PowerSettingsNew, Trigger.Boot),
    TriggerType("Screen", "Device", Icons.Outlined.Devices, Trigger.ScreenStateTrigger(ScreenState.ON)),
    TriggerType("Battery", "Device", Icons.Outlined.BatteryFull, Trigger.BatteryLevel(20, BatteryDirection.CHARGING_STARTED)),
    TriggerType("App opened", "Apps", Icons.Outlined.Apps, Trigger.AppOpened("")),
    TriggerType("Notification", "Apps", Icons.Outlined.Notifications, Trigger.Notification("")),
    TriggerType("Phone", "Connections", Icons.Outlined.Phone, Trigger.PhoneState(PhoneEvent.INCOMING_CALL)),
    TriggerType("Connectivity", "Connections", Icons.Outlined.Wifi, Trigger.Connectivity(ConnMedium.WIFI, ConnState.CONNECTED)),
    TriggerType("WiFi", "Connections", Icons.Outlined.Wifi, Trigger.WifiConnected()),
    TriggerType("Bluetooth", "Connections", Icons.Outlined.Bluetooth, Trigger.BluetoothDevice(ConnState.CONNECTED)),
    TriggerType("Geofence", "Location", Icons.Outlined.LocationOn, Trigger.Geofence(0.0, 0.0, 100.0, Transition.ENTER)),
    TriggerType("Calendar", "Schedule", Icons.Outlined.CalendarMonth, Trigger.CalendarEvent()),
)

@Composable
fun TriggerConfigScreen(
    triggerJson: String,
    navController: NavController,
) {
    val initial = remember(triggerJson) {
        runCatching { Json.decodeFromString<Trigger>(triggerJson) }.getOrNull()
            ?: Trigger.Time(cron = "0 12 * * *", tz = defaultTimeZone())
    }
    var trigger by remember { mutableStateOf(initial) }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(NothingSpacing.md),
            ) {
            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Text(
                text = triggerDescription(trigger).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))

            TriggerTypeSelector(
                selected = trigger,
                onSelect = { trigger = it },
            )

            Spacer(modifier = Modifier.height(NothingSpacing.xl))

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
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriggerTypeSelector(
    selected: Trigger,
    onSelect: (Trigger) -> Unit,
) {
    val types = remember { triggerTypes() }
    // Group by category for visual catalog
    val grouped = types.groupBy { it.category }

    grouped.forEach { (category, items) ->
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = SpaceMono,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = NothingSpacing.md, bottom = NothingSpacing.xs),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            maxItemsInEachRow = 4,
        ) {
            items.forEach { type ->
                val isSelected = selected::class == type.trigger::class
                VisualTriggerChip(
                    label = type.label,
                    icon = type.icon,
                    selected = isSelected,
                    onClick = { onSelect(type.trigger) },
                )
            }
        }
    }
}

@Composable
private fun VisualTriggerChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) NothingColors.accent else MaterialTheme.colorScheme.outline
    val iconTint = if (selected) NothingColors.accent else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(NothingSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface, NothingShapes.compact)
                .border(width = 1.dp, color = borderColor, shape = NothingShapes.compact),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) NothingColors.accent else MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceMono,
        )
    }
}

@Composable
private fun TriggerConfigContent(
    trigger: Trigger,
    onUpdate: (Trigger) -> Unit,
) {
    when (val t = trigger) {
        is Trigger.Time -> CustomTimePicker(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.TimeWindow -> TimeWindowContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.Immediate,
        is Trigger.Manual,
        is Trigger.Boot -> {
            Text(
                text = triggerDescription(trigger),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
        }

        is Trigger.ScreenStateTrigger -> ScreenStateContent(
            state = t.state,
            onUpdate = { onUpdate(t.copy(state = it)) },
        )

        is Trigger.BatteryLevel -> BatteryLevelContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.AppOpened -> AppPickerContent(
            currentPackage = t.pkg,
            onPkgChange = { onUpdate(t.copy(pkg = it)) },
        )

        is Trigger.Notification -> NotificationContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.PhoneState -> PhoneStateContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.Connectivity -> ConnectivityContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.WifiConnected -> NothingInput(
            value = t.ssid ?: "",
            onValueChange = { onUpdate(t.copy(ssid = it.ifBlank { null })) },
            label = "SSID (blank = any)",
        )

        is Trigger.BluetoothDevice -> BluetoothDeviceContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.Geofence -> GeofenceContent(
            trigger = t,
            onUpdate = onUpdate,
        )

        is Trigger.CalendarEvent -> CalendarEventContent(
            trigger = t,
            onUpdate = onUpdate,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
            NothingInput(
                value = trigger.startLocal,
                onValueChange = { onUpdate(trigger.copy(startLocal = it)) },
                label = "Start (HH:mm)",
                modifier = Modifier.weight(1f),
            )
            NothingInput(
                value = trigger.endLocal,
                onValueChange = { onUpdate(trigger.copy(endLocal = it)) },
                label = "End (HH:mm)",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = trigger.tz,
            onValueChange = { onUpdate(trigger.copy(tz = it)) },
            label = "Timezone",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        NothingLabel(text = "Days (blank = every day)")
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        DayOfWeekFlowSelector(
            selected = trigger.days ?: emptyList(),
            onChange = { days ->
                onUpdate(trigger.copy(days = days.ifEmpty { null }))
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
            text = option.name.lowercase().replaceFirstChar { it.uppercase() },
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
            value = (trigger.direction ?: BatteryDirection.CHARGING_STARTED).name,
            options = BatteryDirection.entries.map { it.name },
            onSelect = { dir ->
                onUpdate(trigger.copy(direction = BatteryDirection.valueOf(dir)))
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
                text = event.name.lowercase().replaceFirstChar { it.uppercase() },
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
            value = trigger.medium.name,
            options = ConnMedium.entries.map { it.name },
            onSelect = { onUpdate(trigger.copy(medium = ConnMedium.valueOf(it))) },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "State",
            value = trigger.state.name,
            options = ConnState.entries.map { it.name },
            onSelect = { onUpdate(trigger.copy(state = ConnState.valueOf(it))) },
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
    Column {
        NothingEnumSelector(
            label = "State",
            value = trigger.state.name,
            options = ConnState.entries.map { it.name },
            onSelect = { onUpdate(trigger.copy(state = ConnState.valueOf(it))) },
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
}

@Composable
private fun GeofenceContent(
    trigger: Trigger.Geofence,
    onUpdate: (Trigger.Geofence) -> Unit,
) {
    val context = LocalContext.current

    Column {
        // "Use current location" button — fetches last known location from FusedLocationProvider.
        NothingPillButton(
            text = "Use current location",
            onClick = {
                runCatching {
                    val fusedLocationClient = com.google.android.gms.location.LocationServices
                        .getFusedLocationProviderClient(context)
                    if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
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
            fontFamily = SpaceMono,
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
            value = trigger.transition.name,
            options = Transition.entries.map { it.name },
            onSelect = { onUpdate(trigger.copy(transition = Transition.valueOf(it))) },
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
    Column {
        NothingInput(
            value = trigger.titleMatch ?: "",
            onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
            label = "Title contains (blank = any)",
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
            value = trigger.direction.name,
            options = CalendarDirection.entries.map { it.name },
            onSelect = { onUpdate(trigger.copy(direction = CalendarDirection.valueOf(it))) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOfWeekFlowSelector(
    selected: List<DayOfWeek>,
    onChange: (List<DayOfWeek>) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        DayOfWeek.entries.forEach { day ->
            val active = day in selected
            NothingTag(
                text = day.name.take(3).uppercase(),
                active = active,
                modifier = Modifier.clickable {
                    val updated = if (active) selected - day else selected + day
                    onChange(updated)
                },
            )
        }
    }
}

@Composable
private fun BooleanRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = NothingSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceMono,
        )
        NothingToggle(
            checked = checked,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun NothingLabel(text: String) {
    com.tdvorak.nothingmodes.ui.theme.NothingLabel(text = text)
}

// ─── Installed App Picker ────────────────────────────────────────────────────

private data class InstalledApp(
    val label: String,
    val pkg: String,
)

@Composable
private fun AppPickerContent(
    currentPackage: String,
    onPkgChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showList by remember { mutableStateOf(false) }

    val installedApps = remember {
        runCatching {
            val pm = context.packageManager
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0)
                .map { ri ->
                    InstalledApp(
                        label = ri.loadLabel(pm).toString(),
                        pkg = ri.activityInfo.packageName,
                    )
                }
                .sortedBy { it.label.lowercase() }
        }.getOrDefault(emptyList())
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.pkg.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedLabel = installedApps.find { it.pkg == currentPackage }?.label ?: currentPackage

    Column {
        Text(
            text = "Selected app",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = SpaceMono,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showList = !showList },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedLabel.ifBlank { "Tap to select an app" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                Text(
                    text = if (showList) "[CLOSE]" else "[OPEN]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }
        }

        if (showList) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search",
                placeholder = "Search apps...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                items(filteredApps, key = { it.pkg }) { app ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPkgChange(app.pkg)
                                showList = false
                                searchQuery = ""
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NothingSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (app.pkg == currentPackage) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(20.dp)
                                        .background(NothingColors.accent),
                                )
                                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                            }
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SpaceMono,
                            )
                        }
                    }
                }
            }
        }
    }
}

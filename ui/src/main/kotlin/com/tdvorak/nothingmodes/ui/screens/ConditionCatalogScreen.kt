package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.CapabilityResolver
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.engine.model.CallState
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.ChargerSource
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.NothingBottomActionBar
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingRequirementBadge
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.BOOLEAN_STATE_ITEMS
import com.tdvorak.nothingmodes.ui.util.NUMERIC_STATE_ITEMS
import com.tdvorak.nothingmodes.ui.util.capabilityGaps
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import com.tdvorak.nothingmodes.ui.util.requirementBadges
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private data class ConditionItem(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val condition: Condition,
)

@Composable
fun ConditionCatalogScreen(navController: NavController) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    // Conditions are configured in a bottom sheet before being added.
    var selected by remember { mutableStateOf<List<Condition>>(emptyList()) }
    var configCondition by remember { mutableStateOf<Condition?>(null) }
    var pendingCondition by remember { mutableStateOf<Condition?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var caps by remember { mutableStateOf(DeviceCapabilities()) }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) { caps = CapabilityDetector(context).detect() }
    }
    val resolver = remember(caps) { CapabilityResolver(caps) }

    val items =
        remember {
            listOf(
                ConditionItem(
                    label = "Battery level",
                    category = "Device status",
                    icon = Icons.Outlined.BatteryFull,
                    condition = Condition.BatteryLevel(CmpOp.LT, 20),
                ),
                ConditionItem(
                    label = "Charging status",
                    category = "Device status",
                    icon = Icons.Outlined.Power,
                    condition = Condition.Charging(true),
                ),
                ConditionItem(
                    label = "Screen state",
                    category = "Device status",
                    icon = Icons.Outlined.Devices,
                    condition = Condition.ScreenStateCondition(ScreenState.ON),
                ),
                ConditionItem(
                    label = "Wi-Fi",
                    category = "Connections",
                    icon = Icons.Outlined.Wifi,
                    condition = Condition.WifiConnected(),
                ),
                ConditionItem(
                    label = "Bluetooth",
                    category = "Connections",
                    icon = Icons.Outlined.Bluetooth,
                    condition = Condition.BluetoothConnected(),
                ),
                ConditionItem(
                    label = "Time period",
                    category = "Time",
                    icon = Icons.Outlined.Schedule,
                    condition = Condition.TimeWindow("22:00", "07:00", defaultTimeZone()),
                ),
                ConditionItem(
                    label = "Day of week",
                    category = "Time",
                    icon = Icons.Outlined.CalendarMonth,
                    condition = Condition.DayOfWeekCondition(DayOfWeek.entries),
                ),
                ConditionItem(
                    label = "App in foreground",
                    category = "Apps",
                    icon = Icons.Outlined.Devices,
                    condition = Condition.AppInForeground("com.example.app"),
                ),
                ConditionItem(
                    label = "Current mode active",
                    category = "Device status",
                    icon = Icons.Outlined.Star,
                    condition = Condition.CurrentModeActive("mode-id"),
                ),
                // ── Device status (extended) ──
                ConditionItem(
                    label = "Power saving",
                    category = "Device status",
                    icon = Icons.Outlined.PowerSettingsNew,
                    condition = Condition.PowerSaving(true),
                ),
                ConditionItem(
                    label = "Dark mode",
                    category = "Device status",
                    icon = Icons.Outlined.DarkMode,
                    condition = Condition.DarkModeActive(true),
                ),
                ConditionItem(
                    label = "Media playing",
                    category = "Device status",
                    icon = Icons.Outlined.GraphicEq,
                    condition = Condition.MediaPlaying(true),
                ),
                ConditionItem(
                    label = "Ringer mode",
                    category = "Device status",
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    condition = Condition.RingerMode("normal"),
                ),
                // ── Connections / system ──
                ConditionItem(
                    label = "Airplane mode",
                    category = "Connections",
                    icon = Icons.Outlined.Flight,
                    condition = Condition.AirplaneModeOn(true),
                ),
                ConditionItem(
                    label = "NFC",
                    category = "Connections",
                    icon = Icons.Outlined.Nfc,
                    condition = Condition.NfcEnabled(true),
                ),
                ConditionItem(
                    label = "Location",
                    category = "Connections",
                    icon = Icons.Outlined.LocationOn,
                    condition = Condition.LocationEnabled(true),
                ),
                ConditionItem(
                    label = "Call state",
                    category = "Device status",
                    icon = Icons.Outlined.PhoneAndroid,
                    condition = Condition.CallStateCondition(CallState.INCOMING),
                ),
                ConditionItem(
                    label = "Alarm ringing",
                    category = "Time",
                    icon = Icons.Outlined.Alarm,
                    condition = Condition.AlarmRinging(),
                ),
                ConditionItem(
                    label = "Screen time",
                    category = "Device status",
                    icon = Icons.Outlined.Timer,
                    condition = Condition.ScreenTime(CmpOp.GT, 120),
                ),
                // ── Device status (charger / audio / system toggles) ──
                ConditionItem(
                    label = "Headphones connected",
                    category = "Device status",
                    icon = Icons.Outlined.Headphones,
                    condition = Condition.HeadphonesConnected(true),
                ),
                ConditionItem(
                    label = "Data saver",
                    category = "Device status",
                    icon = Icons.Outlined.DataSaverOn,
                    condition = Condition.DataSaverOn(true),
                ),
                ConditionItem(
                    label = "Auto-sync",
                    category = "Device status",
                    icon = Icons.Outlined.Sync,
                    condition = Condition.AutoSyncOn(true),
                ),
                ConditionItem(
                    label = "Auto-rotate",
                    category = "Device status",
                    icon = Icons.Outlined.ScreenRotation,
                    condition = Condition.AutoRotateOn(true),
                ),
                ConditionItem(
                    label = "Volume level",
                    category = "Device status",
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    condition = Condition.VolumeLevel(VolumeStream.MEDIA, CmpOp.GT, 5),
                ),
                ConditionItem(
                    label = "Charging source",
                    category = "Device status",
                    icon = Icons.Outlined.ElectricBolt,
                    condition = Condition.ChargingSource(ChargerSource.WIRELESS),
                ),
                ConditionItem(
                    label = "Battery temperature",
                    category = "Device status",
                    icon = Icons.Outlined.Thermostat,
                    condition = Condition.BatteryTemp(CmpOp.GTE, 40.0),
                ),
                ConditionItem(
                    label = "Screen off for",
                    category = "Time",
                    icon = Icons.Outlined.Bedtime,
                    condition = Condition.ScreenOffFor(CmpOp.GTE, 30),
                ),
                ConditionItem(
                    label = "Thermal status",
                    category = "Device status",
                    icon = Icons.Outlined.DeviceThermostat,
                    condition = Condition.ThermalLevel(CmpOp.GTE, 3),
                ),
                ConditionItem(
                    label = "At location",
                    category = "Location",
                    icon = Icons.Outlined.LocationOn,
                    condition = Condition.AtLocation(50.0755, 14.4378, 500.0),
                ),
                ConditionItem(
                    label = "Calendar event active",
                    category = "Time",
                    icon = Icons.Outlined.Event,
                    condition = Condition.EventActive("meeting"),
                ),
                ConditionItem(
                    label = "Notification present",
                    category = "Notifications",
                    icon = Icons.Outlined.NotificationsActive,
                    condition = Condition.NotificationPresent(pkg = "com.example", titleMatch = ""),
                ),
            ) + BOOLEAN_STATE_ITEMS.map {
                ConditionItem(
                    label = it.label,
                    category = it.category,
                    icon = it.icon,
                    condition = Condition.BooleanState(it.key, true),
                )
            } + NUMERIC_STATE_ITEMS.map {
                ConditionItem(
                    label = it.label,
                    category = it.category,
                    icon = it.icon,
                    condition = Condition.NumericState(it.key, it.defaultOp, it.defaultValue),
                )
            }
        }

    val filtered =
        remember(search, items) {
            if (search.isBlank()) {
                items
            } else {
                items.filter {
                    it.label.contains(search, ignoreCase = true) ||
                        it.category.contains(search, ignoreCase = true)
                }
            }
        }

    val grouped = filtered.groupBy { it.category.uppercase() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Add Condition",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = NothingSpacing.md),
            ) {
                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    NothingInput(
                        value = search,
                        onValueChange = { search = it },
                        label = "Search",
                        placeholder = "Find a condition",
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                }

                if (selected.isNotEmpty()) {
                    item {
                        NothingSectionHeader(text = "Selected")
                        NothingCard {
                            selected.forEachIndexed { index, condition ->
                                if (index > 0) NothingDivider()
                                val item = items.find { it.condition::class == condition::class }
                                NothingListRow(
                                    title = item?.label ?: "Condition",
                                    subtitle = conditionDescription(condition),
                                    onClick = {
                                        editingIndex = index
                                        configCondition = condition
                                    },
                                    leading = {
                                        NothingIconCircle(size = 44f) {
                                            Icon(
                                                imageVector = item?.icon ?: Icons.AutoMirrored.Outlined.HelpOutline,
                                                contentDescription = item?.label,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    },
                                    trailing = {
                                        Text(
                                            text = "[X]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NothingColors.accent,
                                            fontFamily = NothingFonts.mono(),
                                            modifier =
                                                Modifier.clickable {
                                                    selected = selected.filterIndexed { i, _ -> i != index }
                                                },
                                        )
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    }
                }

                grouped.forEach { (category, conditions) ->
                    item {
                        NothingSectionHeader(text = category)
                        NothingCard {
                            conditions.forEachIndexed { index, conditionItem ->
                                if (index > 0) NothingDivider()
                                val (subtitle, badges) = conditionCatalogMeta(conditionItem.condition, caps)
                                CatalogListItem(
                                    label = conditionItem.label,
                                    icon = conditionItem.icon,
                                    subtitle = subtitle,
                                    badges = badges,
                                    onClick = {
                                        editingIndex = null
                                        val required = CapabilityRequirements.derive(Trigger.Immediate, emptyList(), conditionItem.condition)
                                        if (resolver.resolve(conditionItem.label, required).canRun) {
                                            configCondition = conditionItem.condition
                                        } else {
                                            pendingCondition = conditionItem.condition
                                        }
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    }
                }

                item {
                    // Room for the floating Done bar.
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }

            // Sticky bottom bar — confirms every configured condition in one shot.
            NothingBottomActionBar(
                text =
                    if (selected.isEmpty()) {
                        "Select at least one condition"
                    } else {
                        "Add ${selected.size} condition${if (selected.size > 1) "s" else ""}"
                    },
                onClick = {
                    if (selected.isNotEmpty()) {
                        val json = Json.encodeToString(selected)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("condition_results", json)
                    }
                    navController.popBackStack()
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    pendingCondition?.let { condition ->
        val required = CapabilityRequirements.derive(Trigger.Immediate, emptyList(), condition)
        val resolution = resolver.resolve(condition::class.simpleName ?: "", required)
        CapabilityWarningDialog(
            gaps = capabilityGaps(context, resolution.missingReasons, caps),
            onDismiss = { pendingCondition = null },
            onConfirm = {
                configCondition = condition
                pendingCondition = null
            },
        )
    }

    configCondition?.let { condition ->
        ConditionConfigSheet(
            condition = condition,
            onDone = { updated ->
                if (editingIndex != null) {
                    selected = selected.toMutableList().also { it[editingIndex!!] = updated }
                } else {
                    selected = selected + updated
                }
                configCondition = null
                editingIndex = null
            },
            onDismiss = {
                configCondition = null
                editingIndex = null
            },
        )
    }
}

@Composable
private fun CatalogListItem(
    label: String,
    icon: ImageVector,
    subtitle: String,
    badges: List<String>,
    onClick: () -> Unit,
) {
    NothingListRow(
        title = label,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            NothingIconCircle(size = 44f) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailing = if (badges.isNotEmpty()) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                    badges.take(2).forEach {
                        NothingRequirementBadge(text = it)
                    }
                }
            }
        } else {
            null
        },
    )
}

private fun conditionCatalogMeta(
    condition: Condition,
    caps: DeviceCapabilities,
): Pair<String, List<String>> {
    val static = conditionDescription(condition)
    val required = CapabilityRequirements.derive(Trigger.Immediate, emptyList(), condition)
    val resolution = CapabilityResolver(caps).resolve("", required)
    val subtitle = if (!resolution.canRun) {
        resolution.missingReasons.values.firstOrNull() ?: static
    } else {
        static
    }
    return subtitle to requirementBadges(resolution.missing)
}

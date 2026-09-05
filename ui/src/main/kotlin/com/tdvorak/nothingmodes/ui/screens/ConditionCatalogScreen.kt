package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.CallState
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.ui.theme.NothingDotGrid
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private data class ConditionItem(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val condition: Condition,
)

@Composable
fun ConditionCatalogScreen(
    navController: NavController,
) {
    var search by remember { mutableStateOf("") }

    val items = remember {
        listOf(
            ConditionItem(
                label = "Battery level",
                category = "Device status",
                icon = Icons.Default.BatteryFull,
                condition = Condition.BatteryLevel(CmpOp.LT, 20),
            ),
            ConditionItem(
                label = "Charging status",
                category = "Device status",
                icon = Icons.Default.Power,
                condition = Condition.Charging(true),
            ),
            ConditionItem(
                label = "Screen state",
                category = "Device status",
                icon = Icons.Default.Devices,
                condition = Condition.ScreenStateCondition(ScreenState.ON),
            ),
            ConditionItem(
                label = "Wi-Fi",
                category = "Connections",
                icon = Icons.Default.Wifi,
                condition = Condition.WifiConnected(),
            ),
            ConditionItem(
                label = "Bluetooth",
                category = "Connections",
                icon = Icons.Default.Bluetooth,
                condition = Condition.BluetoothConnected(),
            ),
            ConditionItem(
                label = "Time period",
                category = "Time",
                icon = Icons.Default.Schedule,
                condition = Condition.TimeWindow("22:00", "07:00", defaultTimeZone()),
            ),
            ConditionItem(
                label = "Day of week",
                category = "Time",
                icon = Icons.Default.CalendarMonth,
                condition = Condition.DayOfWeekCondition(DayOfWeek.entries),
            ),
            ConditionItem(
                label = "App in foreground",
                category = "Apps",
                icon = Icons.Default.Devices,
                condition = Condition.AppInForeground("com.example.app"),
            ),
            ConditionItem(
                label = "Current mode active",
                category = "Device status",
                icon = Icons.Default.Star,
                condition = Condition.CurrentModeActive("mode-id"),
            ),
            // ── Device status (extended) ──
            ConditionItem(
                label = "Power saving",
                category = "Device status",
                icon = Icons.Default.PowerSettingsNew,
                condition = Condition.PowerSaving(true),
            ),
            ConditionItem(
                label = "Dark mode",
                category = "Device status",
                icon = Icons.Default.DarkMode,
                condition = Condition.DarkModeActive(true),
            ),
            ConditionItem(
                label = "Media playing",
                category = "Device status",
                icon = Icons.Default.GraphicEq,
                condition = Condition.MediaPlaying(true),
            ),
            ConditionItem(
                label = "Ringer mode",
                category = "Device status",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                condition = Condition.RingerMode("normal"),
            ),
            // ── Connections / system ──
            ConditionItem(
                label = "Airplane mode",
                category = "Connections",
                icon = Icons.Default.Flight,
                condition = Condition.AirplaneModeOn(true),
            ),
            ConditionItem(
                label = "NFC",
                category = "Connections",
                icon = Icons.Default.Bluetooth,
                condition = Condition.NfcEnabled(true),
            ),
            ConditionItem(
                label = "Location",
                category = "Connections",
                icon = Icons.Default.LocationOn,
                condition = Condition.LocationEnabled(true),
            ),
            ConditionItem(
                label = "Call state",
                category = "Device status",
                icon = Icons.Default.PhoneAndroid,
                condition = Condition.CallStateCondition(CallState.INCOMING),
            ),
            ConditionItem(
                label = "Alarm ringing",
                category = "Time",
                icon = Icons.Default.Alarm,
                condition = Condition.AlarmRinging(),
            ),
        )
    }

    val filtered = remember(search, items) {
        if (search.isBlank()) items else items.filter { it.label.contains(search, ignoreCase = true) }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NothingDotGrid(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.04f,
            )
            LazyColumn(
                modifier = Modifier
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

            grouped.forEach { (category, conditions) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                }

                items(conditions, key = { it.label }) { item ->
                    ConditionListItem(
                        label = item.label,
                        icon = item.icon,
                        onClick = {
                            val json = Json.encodeToString(item.condition)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("condition_result", json)
                            navController.popBackStack()
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                }
            }
        }
    }
}
}

@Composable
private fun ConditionListItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = NothingSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surface, NothingShapes.compact)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = NothingShapes.compact),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

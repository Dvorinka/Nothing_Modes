package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
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
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
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
    // Multi-select: tapping toggles a condition instead of closing the catalog.
    var selected by remember { mutableStateOf<List<Condition>>(emptyList()) }

    val items = remember {
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
                icon = Icons.Outlined.Bluetooth,
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
                    val isPicked = item.condition in selected
                    ConditionListItem(
                        label = item.label,
                        icon = item.icon,
                        picked = isPicked,
                        onClick = {
                            selected = if (isPicked) selected - item.condition else selected + item.condition
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                }
            }

            item {
                // Room for the floating Done bar.
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        // Sticky bottom bar — confirms every picked condition in one shot.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(NothingSpacing.md)
                .navigationBarsPadding(),
        ) {
            com.tdvorak.nothingmodes.ui.theme.NothingPillButton(
                text = if (selected.isEmpty()) "Done" else "Add ${selected.size} condition${if (selected.size > 1) "s" else ""}",
                onClick = {
                    if (selected.isNotEmpty()) {
                        val json = Json.encodeToString(selected)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("condition_results", json)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
}

@Composable
private fun ConditionListItem(
    label: String,
    icon: ImageVector,
    picked: Boolean,
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
        NothingIconCircle(size = 44f) {
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
        if (picked) {
            Text(
                text = "ADDED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = SpaceMono,
            )
        }
        Icon(
            imageVector = if (picked) Icons.Outlined.CheckCircle
            else Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = if (picked) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (picked) 20.dp else 16.dp),
        )
    }
}

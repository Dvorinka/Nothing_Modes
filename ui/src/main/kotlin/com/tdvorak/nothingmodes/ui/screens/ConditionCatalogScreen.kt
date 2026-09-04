package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
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
    val iconBg: Color,
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
                iconBg = Color(0xFF4A9E5C),
                condition = Condition.BatteryLevel(CmpOp.LT, 20),
            ),
            ConditionItem(
                label = "Charging status",
                category = "Device status",
                icon = Icons.Default.Power,
                iconBg = Color(0xFFD4A843),
                condition = Condition.Charging(true),
            ),
            ConditionItem(
                label = "Screen state",
                category = "Device status",
                icon = Icons.Default.Devices,
                iconBg = Color(0xFF5B9BF6),
                condition = Condition.ScreenStateCondition(ScreenState.ON),
            ),
            ConditionItem(
                label = "Wi-Fi",
                category = "Connections",
                icon = Icons.Default.Wifi,
                iconBg = Color(0xFF4A9E5C),
                condition = Condition.WifiConnected(),
            ),
            ConditionItem(
                label = "Bluetooth",
                category = "Connections",
                icon = Icons.Default.Bluetooth,
                iconBg = Color(0xFF5B9BF6),
                condition = Condition.BluetoothConnected(),
            ),
            ConditionItem(
                label = "Time period",
                category = "Time",
                icon = Icons.Default.Schedule,
                iconBg = Color(0xFFD4A843),
                condition = Condition.TimeWindow("22:00", "07:00", defaultTimeZone()),
            ),
            ConditionItem(
                label = "Day of week",
                category = "Time",
                icon = Icons.Default.CalendarMonth,
                iconBg = Color(0xFFD71921),
                condition = Condition.DayOfWeekCondition(DayOfWeek.entries),
            ),
            ConditionItem(
                label = "App in foreground",
                category = "Apps",
                icon = Icons.Default.Devices,
                iconBg = Color(0xFF9B9B9B),
                condition = Condition.AppInForeground("com.example.app"),
            ),
            // ── Device status (extended) ──
            ConditionItem(
                label = "Power saving",
                category = "Device status",
                icon = Icons.Default.PowerSettingsNew,
                iconBg = Color(0xFFD71921),
                condition = Condition.PowerSaving(true),
            ),
            ConditionItem(
                label = "Dark mode",
                category = "Device status",
                icon = Icons.Default.DarkMode,
                iconBg = Color(0xFF5B9BF6),
                condition = Condition.DarkModeActive(true),
            ),
            ConditionItem(
                label = "Media playing",
                category = "Device status",
                icon = Icons.Default.GraphicEq,
                iconBg = Color(0xFFD4A843),
                condition = Condition.MediaPlaying(true),
            ),
            ConditionItem(
                label = "Ringer mode",
                category = "Device status",
                icon = Icons.Default.VolumeUp,
                iconBg = Color(0xFF4A9E5C),
                condition = Condition.RingerMode("normal"),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        iconBg = item.iconBg,
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

@Composable
private fun ConditionListItem(
    label: String,
    icon: ImageVector,
    iconBg: Color,
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
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.Black,
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

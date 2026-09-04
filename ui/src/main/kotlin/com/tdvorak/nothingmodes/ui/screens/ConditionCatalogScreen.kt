package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Schedule
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
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionCatalogScreen(
    navController: NavController,
) {
    var search by remember { mutableStateOf("") }

    val items = remember {
        listOf(
            ConditionItem("Battery Level", "Device Status", Icons.Default.BatteryFull, Condition.BatteryLevel(CmpOp.LT, 20)),
            ConditionItem("Charging", "Device Status", Icons.Default.BatteryFull, Condition.Charging(true)),
            ConditionItem("Screen On", "Device Status", Icons.Default.Devices, Condition.ScreenStateCondition(ScreenState.ON)),
            ConditionItem("Screen Off", "Device Status", Icons.Default.Devices, Condition.ScreenStateCondition(ScreenState.OFF)),
            ConditionItem("Wi-Fi Connected", "Connectivity", Icons.Default.Wifi, Condition.WifiConnected()),
            ConditionItem("Bluetooth Connected", "Connectivity", Icons.Default.Bluetooth, Condition.BluetoothConnected()),
            ConditionItem("Time Window", "Time", Icons.Default.Schedule, Condition.TimeWindow("22:00", "07:00", defaultTimeZone())),
            ConditionItem("Weekdays", "Time", Icons.Default.CalendarMonth, Condition.DayOfWeekCondition(DayOfWeek.entries.take(5))),
            ConditionItem("Weekends", "Time", Icons.Default.CalendarMonth, Condition.DayOfWeekCondition(DayOfWeek.entries.drop(5))),
        )
    }

    val filtered = remember(search, items) {
        if (search.isBlank()) items else items.filter { it.label.contains(search, ignoreCase = true) }
    }

    val grouped = filtered.groupBy { it.category }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Add Condition",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(NothingSpacing.md),
        ) {
            Spacer(modifier = Modifier.height(NothingSpacing.lg))
            NothingInput(
                value = search,
                onValueChange = { search = it },
                label = "Search",
                placeholder = "Find a condition",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            grouped.forEach { (category, conditions) ->
                NothingSectionHeader(text = category.uppercase())
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                ) {
                    conditions.forEach { item ->
                        ConditionTile(
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
                }
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
            }
        }
    }
}

@Composable
private fun ConditionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(NothingSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .fillMaxSize()
                .padding(NothingSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceMono,
        )
    }
}

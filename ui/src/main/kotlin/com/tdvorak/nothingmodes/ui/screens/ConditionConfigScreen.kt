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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Composable
fun ConditionConfigScreen(
    conditionJson: String,
    navController: NavController,
) {
    val initial = remember(conditionJson) {
        runCatching { Json.decodeFromString<Condition>(conditionJson) }.getOrNull()
            ?: Condition.BatteryLevel(CmpOp.LT, 20)
    }
    var condition by remember { mutableStateOf(initial) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Configure Condition",
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

            when (val c = condition) {
                is Condition.BatteryLevel -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        Box(modifier = Modifier.weight(0.35f)) {
                            NothingEnumSelector(
                                label = "Operator",
                                value = c.op.name,
                                options = CmpOp.entries.map { it.name },
                                onSelect = { op ->
                                    condition = c.copy(op = CmpOp.valueOf(op))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Box(modifier = Modifier.weight(0.65f)) {
                            NothingInput(
                                value = c.level.toString(),
                                onValueChange = { text ->
                                    condition = c.copy(level = text.toIntOrNull() ?: c.level)
                                },
                                label = "Level (%)",
                            )
                        }
                    }
                }

                is Condition.Charging -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Is charging",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SpaceMono,
                        )
                        NothingToggle(
                            checked = c.isCharging,
                            onCheckedChange = { condition = c.copy(isCharging = it) },
                        )
                    }
                }

                is Condition.ScreenStateCondition -> {
                    NothingEnumSelector(
                        label = "Screen state",
                        value = c.state.name,
                        options = ScreenState.entries.map { it.name },
                        onSelect = { state ->
                            condition = c.copy(state = ScreenState.valueOf(state))
                        },
                    )
                }

                is Condition.WifiConnected -> {
                    NothingInput(
                        value = c.ssid ?: "",
                        onValueChange = { condition = c.copy(ssid = it.ifBlank { null }) },
                        label = "SSID (blank = any)",
                    )
                }

                is Condition.BluetoothConnected -> {
                    NothingInput(
                        value = c.deviceName ?: "",
                        onValueChange = { condition = c.copy(deviceName = it.ifBlank { null }) },
                        label = "Device name (blank = any)",
                    )
                }

                is Condition.TimeWindow -> {
                    NothingInput(
                        value = c.startLocal,
                        onValueChange = { condition = c.copy(startLocal = it) },
                        label = "Start (HH:mm)",
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    NothingInput(
                        value = c.endLocal,
                        onValueChange = { condition = c.copy(endLocal = it) },
                        label = "End (HH:mm)",
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    NothingInput(
                        value = c.tz,
                        onValueChange = { condition = c.copy(tz = it) },
                        label = "Timezone",
                    )
                }

                is Condition.DayOfWeekCondition -> {
                    DayOfWeekSelector(
                        selected = c.days.toSet(),
                        onChange = { condition = c.copy(days = it.toList()) },
                    )
                }

                else -> {
                    Text(
                        text = conditionDescription(condition),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            NothingPillButton(
                text = "Done",
                onClick = {
                    val result = Json.encodeToString(condition)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("condition_config_result", result)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DayOfWeekSelector(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit,
) {
    Column {
        DayOfWeek.entries.forEach { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(if (day in selected) selected - day else selected + day) }
                    .padding(vertical = NothingSpacing.sm),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            ) {
                Text(
                    text = day.name.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                androidx.compose.material3.Checkbox(
                    checked = day in selected,
                    onCheckedChange = { checked ->
                        onChange(if (checked) selected + day else selected - day)
                    },
                )
            }
        }
    }
}

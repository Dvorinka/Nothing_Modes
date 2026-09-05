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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.CallState
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        com.tdvorak.nothingmodes.ui.components.NothingTimeField(
                            label = "Starts",
                            value = c.startLocal,
                            onValueChange = { condition = c.copy(startLocal = it) },
                            modifier = Modifier.weight(1f),
                        )
                        com.tdvorak.nothingmodes.ui.components.NothingTimeField(
                            label = "Ends",
                            value = c.endLocal,
                            onValueChange = { condition = c.copy(endLocal = it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    com.tdvorak.nothingmodes.ui.components.NothingTimeZoneField(
                        value = c.tz,
                        onValueChange = { condition = c.copy(tz = it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is Condition.DayOfWeekCondition -> {
                    com.tdvorak.nothingmodes.ui.components.NothingDaySelector(
                        selected = c.days.toSet(),
                        onChange = { days ->
                            condition = c.copy(
                                days = DayOfWeek.entries.filter { it in days },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is Condition.CurrentModeActive -> {
                    NothingInput(
                        value = c.modeId,
                        onValueChange = { condition = c.copy(modeId = it) },
                        label = "Mode ID",
                    )
                }

                is Condition.AppInForeground -> {
                    NothingInput(
                        value = c.pkg,
                        onValueChange = { condition = c.copy(pkg = it) },
                        label = "Package name",
                    )
                }

                is Condition.DarkModeActive -> {
                    BooleanRow(
                        label = "Dark mode active",
                        checked = c.active,
                        onChange = { condition = c.copy(active = it) },
                    )
                }

                is Condition.PowerSaving -> {
                    BooleanRow(
                        label = "Power saving on",
                        checked = c.on,
                        onChange = { condition = c.copy(on = it) },
                    )
                }

                is Condition.MediaPlaying -> {
                    BooleanRow(
                        label = "Media playing",
                        checked = c.playing,
                        onChange = { condition = c.copy(playing = it) },
                    )
                }

                is Condition.RingerMode -> {
                    RingerModeSelector(
                        mode = c.mode,
                        onChange = { condition = c.copy(mode = it) },
                    )
                }

                is Condition.AirplaneModeOn -> {
                    BooleanRow(
                        label = "Airplane mode on",
                        checked = c.on,
                        onChange = { condition = c.copy(on = it) },
                    )
                }

                is Condition.NfcEnabled -> {
                    BooleanRow(
                        label = "NFC enabled",
                        checked = c.enabled,
                        onChange = { condition = c.copy(enabled = it) },
                    )
                }

                is Condition.LocationEnabled -> {
                    BooleanRow(
                        label = "Location enabled",
                        checked = c.enabled,
                        onChange = { condition = c.copy(enabled = it) },
                    )
                }

                is Condition.CallStateCondition -> {
                    CallState.entries.forEach { s ->
                        RadioOption(
                            text = s.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = c.state == s,
                            onClick = { condition = c.copy(state = s) },
                        )
                    }
                }

                is Condition.AlarmRinging -> {
                    Text(
                        text = "Match alarm title (optional)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    NothingInput(
                        value = c.titleMatch ?: "",
                        onValueChange = { condition = c.copy(titleMatch = it.ifBlank { null }) },
                        label = "Title contains",
                        placeholder = "Leave blank for any alarm",
                        modifier = Modifier.fillMaxWidth(),
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

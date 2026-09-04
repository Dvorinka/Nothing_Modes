package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.SpaceMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionConfigSheet(
    condition: Condition,
    onDone: (Condition) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember(condition) { mutableStateOf(condition) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { SheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NothingSpacing.md)
                .padding(bottom = NothingSpacing.xl)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = conditionTitle(condition),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceMono,
                modifier = Modifier.padding(bottom = NothingSpacing.md),
            )

            when (val c = current) {
                is Condition.BatteryLevel -> BatteryLevelSheetContent(
                    condition = c,
                    onChange = { current = it },
                )

                is Condition.Charging -> ChargingSheetContent(
                    condition = c,
                    onChange = { current = it },
                )

                is Condition.ScreenStateCondition -> ScreenStateSheetContent(
                    condition = c,
                    onChange = { current = it },
                )

                is Condition.WifiConnected -> SsidSheetContent(
                    ssid = c.ssid ?: "",
                    onChange = { current = c.copy(ssid = it.ifBlank { null }) },
                )

                is Condition.BluetoothConnected -> DeviceNameSheetContent(
                    name = c.deviceName ?: "",
                    onChange = { current = c.copy(deviceName = it.ifBlank { null }) },
                )

                is Condition.AppInForeground -> PackageSheetContent(
                    pkg = c.pkg,
                    onChange = { current = c.copy(pkg = it) },
                )

                is Condition.CurrentModeActive -> ModeIdSheetContent(
                    modeId = c.modeId,
                    onChange = { current = c.copy(modeId = it) },
                )

                is Condition.DarkModeActive -> BooleanConditionContent(
                    label = "Dark mode active",
                    checked = c.active,
                    onChange = { current = c.copy(active = it) },
                )

                is Condition.PowerSaving -> BooleanConditionContent(
                    label = "Power saving on",
                    checked = c.on,
                    onChange = { current = c.copy(on = it) },
                )

                is Condition.MediaPlaying -> BooleanConditionContent(
                    label = "Media playing",
                    checked = c.playing,
                    onChange = { current = c.copy(playing = it) },
                )

                is Condition.RingerMode -> RingerModeSheetContent(
                    mode = c.mode,
                    onChange = { current = c.copy(mode = it) },
                )

                is Condition.TimeWindow -> TimeWindowSheetContent(
                    condition = c,
                    onChange = { current = it },
                )

                is Condition.DayOfWeekCondition -> DayOfWeekSheetContent(
                    condition = c,
                    onChange = { current = it },
                )

                else -> {
                    Text(
                        text = conditionDescription(current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                NothingPillButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                NothingPillButton(
                    text = "Done",
                    onClick = { onDone(current) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = NothingSpacing.sm)
            .size(32.dp, 4.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
    )
}

private fun conditionTitle(condition: Condition): String = when (condition) {
    is Condition.BatteryLevel -> "Battery level"
    is Condition.Charging -> "Charging status"
    is Condition.ScreenStateCondition -> "Screen state"
    is Condition.WifiConnected -> "Wi-Fi"
    is Condition.BluetoothConnected -> "Bluetooth"
    is Condition.TimeWindow -> "Time period"
    is Condition.DayOfWeekCondition -> "Day of week"
    is Condition.AppInForeground -> "App in foreground"
    is Condition.CurrentModeActive -> "Mode active"
    is Condition.DarkModeActive -> "Dark mode"
    is Condition.PowerSaving -> "Power saving"
    is Condition.MediaPlaying -> "Media playing"
    is Condition.RingerMode -> "Ringer mode"
    else -> "Condition"
}

@Composable
private fun BatteryLevelSheetContent(
    condition: Condition.BatteryLevel,
    onChange: (Condition.BatteryLevel) -> Unit,
) {
    Column {
        NothingInput(
            value = condition.level.toString(),
            onValueChange = { onChange(condition.copy(level = it.toIntOrNull() ?: condition.level)) },
            label = "Level (%)",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(NothingSpacing.md))

        val belowSelected = condition.op == CmpOp.LT
        RadioOption(
            text = "Below",
            selected = belowSelected,
            onClick = { onChange(condition.copy(op = CmpOp.LT)) },
        )
        RadioOption(
            text = "Equal to or above",
            selected = !belowSelected,
            onClick = { onChange(condition.copy(op = CmpOp.GTE)) },
        )
    }
}

@Composable
private fun ChargingSheetContent(
    condition: Condition.Charging,
    onChange: (Condition.Charging) -> Unit,
) {
    Column {
        RadioOption(
            text = "Charging",
            selected = condition.isCharging,
            onClick = { onChange(condition.copy(isCharging = true)) },
        )
        RadioOption(
            text = "Not charging",
            selected = !condition.isCharging,
            onClick = { onChange(condition.copy(isCharging = false)) },
        )
    }
}

@Composable
private fun ScreenStateSheetContent(
    condition: Condition.ScreenStateCondition,
    onChange: (Condition.ScreenStateCondition) -> Unit,
) {
    Column {
        RadioOption(
            text = "On",
            selected = condition.state == ScreenState.ON,
            onClick = { onChange(condition.copy(state = ScreenState.ON)) },
        )
        RadioOption(
            text = "Off",
            selected = condition.state == ScreenState.OFF,
            onClick = { onChange(condition.copy(state = ScreenState.OFF)) },
        )
    }
}

@Composable
private fun SsidSheetContent(
    ssid: String,
    onChange: (String) -> Unit,
) {
    NothingInput(
        value = ssid,
        onValueChange = onChange,
        label = "SSID (blank = any)",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeviceNameSheetContent(
    name: String,
    onChange: (String) -> Unit,
) {
    NothingInput(
        value = name,
        onValueChange = onChange,
        label = "Device name (blank = any)",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PackageSheetContent(
    pkg: String,
    onChange: (String) -> Unit,
) {
    NothingInput(
        value = pkg,
        onValueChange = onChange,
        label = "Package name",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ModeIdSheetContent(
    modeId: String,
    onChange: (String) -> Unit,
) {
    NothingInput(
        value = modeId,
        onValueChange = onChange,
        label = "Mode ID",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TimeWindowSheetContent(
    condition: Condition.TimeWindow,
    onChange: (Condition.TimeWindow) -> Unit,
) {
    Column {
        NothingInput(
            value = condition.startLocal,
            onValueChange = { onChange(condition.copy(startLocal = it)) },
            label = "Start (HH:mm)",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = condition.endLocal,
            onValueChange = { onChange(condition.copy(endLocal = it)) },
            label = "End (HH:mm)",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = condition.tz,
            onValueChange = { onChange(condition.copy(tz = it)) },
            label = "Timezone",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DayOfWeekSheetContent(
    condition: Condition.DayOfWeekCondition,
    onChange: (Condition.DayOfWeekCondition) -> Unit,
) {
    val selected = condition.days.toSet()
    Column {
        DayOfWeek.entries.forEach { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(condition.copy(days = if (day in selected) condition.days - day else condition.days + day)) }
                    .padding(vertical = NothingSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = day.name.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                Checkbox(
                    checked = day in selected,
                    onCheckedChange = { checked ->
                        onChange(condition.copy(days = if (checked) condition.days + day else condition.days - day))
                    },
                )
            }
        }
    }
}

@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
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
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceMono,
        )
    }
}

@Composable
private fun BooleanConditionContent(
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
private fun RingerModeSheetContent(
    mode: String,
    onChange: (String) -> Unit,
) {
    val modes = listOf("silent", "vibrate", "normal")
    modes.forEach { m ->
        RadioOption(
            text = m.replaceFirstChar { it.uppercase() },
            selected = mode == m,
            onClick = { onChange(m) },
        )
    }
}

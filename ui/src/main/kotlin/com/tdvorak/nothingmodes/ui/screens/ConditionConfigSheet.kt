package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.tdvorak.nothingmodes.engine.model.CallState
import com.tdvorak.nothingmodes.engine.model.ChargerSource
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingDragHandle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingRadio
import com.tdvorak.nothingmodes.ui.theme.NothingSecondaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
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
        shape = NothingShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.8f),
        dragHandle = { NothingDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(NothingSpacing.md)
                    .padding(bottom = NothingSpacing.xl)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = conditionTitle(condition),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = GeistSans,
                modifier = Modifier.padding(bottom = NothingSpacing.md),
            )

            when (val c = current) {
                is Condition.BatteryLevel ->
                    BatteryLevelSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.Charging ->
                    ChargingSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.ScreenStateCondition ->
                    ScreenStateSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.WifiConnected ->
                    SsidSheetContent(
                        ssid = c.ssid ?: "",
                        onChange = { current = c.copy(ssid = it.ifBlank { null }) },
                    )

                is Condition.BluetoothConnected ->
                    DeviceNameSheetContent(
                        name = c.deviceName ?: "",
                        onChange = { current = c.copy(deviceName = it.ifBlank { null }) },
                    )

                is Condition.AppInForeground ->
                    PackageSheetContent(
                        pkg = c.pkg,
                        onChange = { current = c.copy(pkg = it) },
                    )

                is Condition.CurrentModeActive ->
                    ModeIdSheetContent(
                        modeId = c.modeId,
                        onChange = { current = c.copy(modeId = it) },
                    )

                is Condition.DarkModeActive ->
                    BooleanConditionContent(
                        label = "Dark mode active",
                        checked = c.active,
                        onChange = { current = c.copy(active = it) },
                    )

                is Condition.PowerSaving ->
                    BooleanConditionContent(
                        label = "Power saving on",
                        checked = c.on,
                        onChange = { current = c.copy(on = it) },
                    )

                is Condition.MediaPlaying ->
                    BooleanConditionContent(
                        label = "Media playing",
                        checked = c.playing,
                        onChange = { current = c.copy(playing = it) },
                    )

                is Condition.RingerMode ->
                    RingerModeSheetContent(
                        mode = c.mode,
                        onChange = { current = c.copy(mode = it) },
                    )

                is Condition.AirplaneModeOn ->
                    BooleanConditionContent(
                        label = "Airplane mode on",
                        checked = c.on,
                        onChange = { current = c.copy(on = it) },
                    )

                is Condition.NfcEnabled ->
                    BooleanConditionContent(
                        label = "NFC enabled",
                        checked = c.enabled,
                        onChange = { current = c.copy(enabled = it) },
                    )

                is Condition.LocationEnabled ->
                    BooleanConditionContent(
                        label = "Location enabled",
                        checked = c.enabled,
                        onChange = { current = c.copy(enabled = it) },
                    )

                is Condition.CallStateCondition ->
                    CallStateSheetContent(
                        state = c.state,
                        onChange = { current = c.copy(state = it) },
                    )

                is Condition.AlarmRinging ->
                    AlarmRingingSheetContent(
                        titleMatch = c.titleMatch ?: "",
                        onChange = { current = c.copy(titleMatch = it.ifBlank { null }) },
                    )

                is Condition.TimeWindow ->
                    TimeWindowSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.DayOfWeekCondition ->
                    DayOfWeekSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.HeadphonesConnected ->
                    BooleanConditionContent(
                        label = "Connected",
                        checked = c.connected,
                        onChange = { current = c.copy(connected = it) },
                    )

                is Condition.DataSaverOn ->
                    BooleanConditionContent(
                        label = "Data saver on",
                        checked = c.on,
                        onChange = { current = c.copy(on = it) },
                    )

                is Condition.AutoSyncOn ->
                    BooleanConditionContent(
                        label = "Auto-sync on",
                        checked = c.on,
                        onChange = { current = c.copy(on = it) },
                    )

                is Condition.AutoRotateOn ->
                    BooleanConditionContent(
                        label = "Auto-rotate on",
                        checked = c.on,
                        onChange = { current = c.copy(on = it) },
                    )

                is Condition.VolumeLevel ->
                    VolumeLevelSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.ScreenOffFor ->
                    ScreenOffForSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.ChargingSource ->
                    ChargingSourceSheetContent(
                        source = c.source,
                        onChange = { current = c.copy(source = it) },
                    )

                is Condition.BatteryTemp ->
                    BatteryTempSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                is Condition.ThermalLevel ->
                    ThermalLevelSheetContent(
                        condition = c,
                        onChange = { current = it },
                    )

                else -> {
                    Text(
                        text = conditionDescription(current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                NothingSecondaryButton(
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

private fun conditionTitle(condition: Condition): String =
    when (condition) {
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
        is Condition.HeadphonesConnected -> "Headphones"
        is Condition.DataSaverOn -> "Data saver"
        is Condition.AutoSyncOn -> "Auto-sync"
        is Condition.AutoRotateOn -> "Auto-rotate"
        is Condition.VolumeLevel -> "Volume level"
        is Condition.ScreenOffFor -> "Screen off for"
        is Condition.ChargingSource -> "Charging source"
        is Condition.BatteryTemp -> "Battery temperature"
        is Condition.ThermalLevel -> "Thermal status"
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            com.tdvorak.nothingmodes.ui.components.NothingTimeField(
                label = "Starts",
                value = condition.startLocal,
                onValueChange = { onChange(condition.copy(startLocal = it)) },
                modifier = Modifier.weight(1f),
            )
            com.tdvorak.nothingmodes.ui.components.NothingTimeField(
                label = "Ends",
                value = condition.endLocal,
                onValueChange = { onChange(condition.copy(endLocal = it)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        com.tdvorak.nothingmodes.ui.components.NothingTimeZoneField(
            value = condition.tz,
            onValueChange = { onChange(condition.copy(tz = it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DayOfWeekSheetContent(
    condition: Condition.DayOfWeekCondition,
    onChange: (Condition.DayOfWeekCondition) -> Unit,
) {
    com.tdvorak.nothingmodes.ui.components.NothingDaySelector(
        selected = condition.days.toSet(),
        onChange = { days ->
            onChange(
                condition.copy(
                    days = DayOfWeek.entries.filter { it in days },
                ),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun RadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = NothingSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        NothingRadio(selected = selected, onClick = onClick)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = NothingFonts.mono(),
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

@Composable
private fun CallStateSheetContent(
    state: CallState,
    onChange: (CallState) -> Unit,
) {
    CallState.entries.forEach { s ->
        RadioOption(
            text = s.name.enumLabel(),
            selected = state == s,
            onClick = { onChange(s) },
        )
    }
}

@Composable
private fun AlarmRingingSheetContent(
    titleMatch: String,
    onChange: (String) -> Unit,
) {
    // ponytail: Alarm title matching is not yet wired to a live alarm provider.
    //          This input is stored for when the broadcast receiver is added.
    Text(
        text = "Match alarm title (optional)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = NothingFonts.mono(),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(NothingSpacing.xs))
    NothingInput(
        value = titleMatch,
        onValueChange = onChange,
        label = "Title contains",
        placeholder = "Leave blank for any alarm",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun VolumeLevelSheetContent(
    condition: Condition.VolumeLevel,
    onChange: (Condition.VolumeLevel) -> Unit,
) {
    Column {
        NothingInput(
            value = condition.level.toString(),
            onValueChange = { onChange(condition.copy(level = it.toIntOrNull() ?: condition.level)) },
            label = "Level",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Stream",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        VolumeStream.entries.forEach { stream ->
            RadioOption(
                text = stream.name.enumLabel(),
                selected = condition.stream == stream,
                onClick = { onChange(condition.copy(stream = stream)) },
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Operator",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        CmpOp.entries.forEach { op ->
            RadioOption(
                text = op.name.enumLabel(),
                selected = condition.op == op,
                onClick = { onChange(condition.copy(op = op)) },
            )
        }
    }
}

@Composable
private fun ScreenOffForSheetContent(
    condition: Condition.ScreenOffFor,
    onChange: (Condition.ScreenOffFor) -> Unit,
) {
    Column {
        NothingInput(
            value = condition.minutes.toString(),
            onValueChange = { onChange(condition.copy(minutes = it.toIntOrNull() ?: condition.minutes)) },
            label = "Minutes",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Operator",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        CmpOp.entries.forEach { op ->
            RadioOption(
                text = op.name.enumLabel(),
                selected = condition.op == op,
                onClick = { onChange(condition.copy(op = op)) },
            )
        }
    }
}

@Composable
private fun ChargingSourceSheetContent(
    source: ChargerSource,
    onChange: (ChargerSource) -> Unit,
) {
    ChargerSource.entries.forEach { s ->
        RadioOption(
            text = s.name.enumLabel(),
            selected = source == s,
            onClick = { onChange(s) },
        )
    }
}

@Composable
private fun BatteryTempSheetContent(
    condition: Condition.BatteryTemp,
    onChange: (Condition.BatteryTemp) -> Unit,
) {
    Column {
        NothingInput(
            value = condition.celsius.toString(),
            onValueChange = { onChange(condition.copy(celsius = it.toDoubleOrNull() ?: condition.celsius)) },
            label = "Celsius",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Operator",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        CmpOp.entries.forEach { op ->
            RadioOption(
                text = op.name.enumLabel(),
                selected = condition.op == op,
                onClick = { onChange(condition.copy(op = op)) },
            )
        }
    }
}

@Composable
private fun ThermalLevelSheetContent(
    condition: Condition.ThermalLevel,
    onChange: (Condition.ThermalLevel) -> Unit,
) {
    Column {
        NothingInput(
            value = condition.level.toString(),
            onValueChange = { onChange(condition.copy(level = it.toIntOrNull() ?: condition.level)) },
            label = "Level (0 none .. 6 shutdown)",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Operator",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.fillMaxWidth(),
        )
        CmpOp.entries.forEach { op ->
            RadioOption(
                text = op.name.enumLabel(),
                selected = condition.op == op,
                onClick = { onChange(condition.copy(op = op)) },
            )
        }
    }
}

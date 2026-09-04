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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.SpaceMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfigSheet(
    action: Action,
    onDone: (Action) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember(action) { mutableStateOf(action) }
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
                text = actionTitle(action),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceMono,
                modifier = Modifier.padding(bottom = NothingSpacing.md),
            )

            ActionConfigContent(
                action = current,
                onActionChange = { current = it },
            )

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
fun ActionConfigContent(
    action: Action,
    onActionChange: (Action) -> Unit,
) {
    when (val a = action) {
        is Action.SetWifi -> {
            BooleanRow(
                label = "Wi-Fi enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetBluetooth -> {
            BooleanRow(
                label = "Bluetooth enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetMobileData -> {
            BooleanRow(
                label = "Mobile data enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetAirplaneMode -> {
            BooleanRow(
                label = "Airplane mode enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetDarkMode -> {
            NothingEnumSelector(
                label = "Dark mode",
                value = a.mode.name,
                options = NightMode.entries.map { it.name },
                onSelect = { onActionChange(a.copy(mode = NightMode.valueOf(it))) },
            )
        }

        is Action.SetBrightness -> {
            // Percentage slider (0-100) mapped to 0-255 internally.
            val percent = (a.level.toFloat() / 255f * 100f).toInt().coerceIn(0, 100)
            Text(
                text = "BRIGHTNESS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                Text(
                    text = "${percent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = SpaceMono,
                    modifier = Modifier.width(56.dp),
                )
                androidx.compose.material3.Slider(
                    value = percent.toFloat(),
                    onValueChange = { v ->
                        onActionChange(a.copy(level = (v / 100f * 255f).toInt().coerceIn(0, 255)))
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            BooleanRow(
                label = "Restore previous",
                checked = a.restore,
                onChange = { onActionChange(a.copy(restore = it)) },
            )
        }

        is Action.SetAutoBrightness -> {
            BooleanRow(
                label = "Auto brightness enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetExtraDim -> {
            BooleanRow(
                label = "Extra dim enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
            BooleanRow(
                label = "Restore previous",
                checked = a.restore,
                onChange = { onActionChange(a.copy(restore = it)) },
            )
        }

        is Action.SetScreenTimeout -> {
            NothingInput(
                value = a.timeoutMs.toString(),
                onValueChange = { onActionChange(a.copy(timeoutMs = it.toIntOrNull() ?: a.timeoutMs)) },
                label = "Timeout (ms)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.SetAlwaysOnDisplay -> {
            BooleanRow(
                label = "Always-on display enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetDnd -> {
            NothingEnumSelector(
                label = "DND mode",
                value = a.mode.name,
                options = DndMode.entries.map { it.name },
                onSelect = { onActionChange(a.copy(mode = DndMode.valueOf(it))) },
            )
        }

        is Action.SetVolume -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                NothingEnumSelector(
                    label = "Stream",
                    value = a.stream.name,
                    options = VolumeStream.entries.map { it.name },
                    onSelect = { onActionChange(a.copy(stream = VolumeStream.valueOf(it))) },
                    modifier = Modifier.weight(1f),
                )
                NothingInput(
                    value = a.level.toString(),
                    onValueChange = { onActionChange(a.copy(level = it.toIntOrNull() ?: a.level)) },
                    label = "Level",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is Action.Vibrate -> {
            NothingInput(
                value = a.durationMs.toString(),
                onValueChange = { onActionChange(a.copy(durationMs = it.toIntOrNull() ?: a.durationMs)) },
                label = "Duration (ms)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.SetAutoRotate -> {
            BooleanRow(
                label = "Auto-rotate enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetBatterySaver -> {
            BooleanRow(
                label = "Battery saver enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetLocationMode -> {
            NothingEnumSelector(
                label = "Location mode",
                value = a.mode.name,
                options = LocationMode.entries.map { it.name },
                onSelect = { onActionChange(a.copy(mode = LocationMode.valueOf(it))) },
            )
        }

        is Action.OpenSettingsScreen -> {
            NothingEnumSelector(
                label = "Settings screen",
                value = a.screen.name,
                options = SettingsScreen.entries.map { it.name },
                onSelect = { onActionChange(a.copy(screen = SettingsScreen.valueOf(it))) },
            )
        }

        is Action.SetFlashlight -> {
            BooleanRow(
                label = "Flashlight enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.GlyphPreset -> {
            NothingInput(
                value = a.preset,
                onValueChange = { onActionChange(a.copy(preset = it)) },
                label = "Preset name",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.GlyphText -> {
            NothingInput(
                value = a.text,
                onValueChange = { onActionChange(a.copy(text = it)) },
                label = "Text",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.GlyphScrollingText -> {
            NothingInput(
                value = a.text,
                onValueChange = { onActionChange(a.copy(text = it)) },
                label = "Scrolling text",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.CopyText -> {
            NothingInput(
                value = a.text,
                onValueChange = { onActionChange(a.copy(text = it)) },
                label = "Text to copy",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.OpenUrl -> {
            NothingInput(
                value = a.url,
                onValueChange = { onActionChange(a.copy(url = it)) },
                label = "URL",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.LaunchApp -> {
            NothingInput(
                value = a.pkg,
                onValueChange = { onActionChange(a.copy(pkg = it)) },
                label = "Package name",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.ShowNotification -> {
            NothingInput(
                value = a.title,
                onValueChange = { onActionChange(a.copy(title = it)) },
                label = "Title",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.text,
                onValueChange = { onActionChange(a.copy(text = it)) },
                label = "Body",
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.Wait -> {
            NothingInput(
                value = a.durationMs.toString(),
                onValueChange = { onActionChange(a.copy(durationMs = it.toLongOrNull() ?: a.durationMs)) },
                label = "Duration (ms)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        else -> {
            Text(
                text = actionDescription(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
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

private fun actionTitle(action: Action): String = when (action) {
    is Action.SetWifi -> "Wi-Fi"
    is Action.SetBluetooth -> "Bluetooth"
    is Action.SetMobileData -> "Mobile data"
    is Action.SetAirplaneMode -> "Airplane mode"
    is Action.SetDarkMode -> "Dark mode"
    is Action.SetBrightness -> "Brightness"
    is Action.SetAutoBrightness -> "Auto brightness"
    is Action.SetExtraDim -> "Extra dim"
    is Action.SetScreenTimeout -> "Screen timeout"
    is Action.SetAlwaysOnDisplay -> "Always-on display"
    is Action.SetDnd -> "Do not disturb"
    is Action.SetVolume -> "Volume"
    is Action.Vibrate -> "Vibrate"
    is Action.SetAutoRotate -> "Auto-rotate"
    is Action.SetBatterySaver -> "Battery saver"
    is Action.SetLocationMode -> "Location mode"
    is Action.OpenSettingsScreen -> "Open settings"
    is Action.SetFlashlight -> "Flashlight"
    is Action.GlyphPreset -> "Glyph preset"
    is Action.GlyphText -> "Glyph text"
    is Action.GlyphScrollingText -> "Glyph scrolling text"
    is Action.CopyText -> "Copy text"
    is Action.OpenUrl -> "Open URL"
    is Action.LaunchApp -> "Launch app"
    is Action.ShowNotification -> "Show notification"
    is Action.Wait -> "Wait"
    else -> "Action"
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

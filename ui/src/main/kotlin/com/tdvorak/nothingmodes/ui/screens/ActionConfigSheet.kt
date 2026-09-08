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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.MusicVisualizerStyles
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDragHandle
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSecondaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
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
                text = actionTitle(action),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = GeistSans,
                modifier = Modifier.padding(bottom = NothingSpacing.sm),
            )

            // Requirement note — tells the user upfront what this action needs
            // (Shizuku, Nothing hardware, a permission, a panel tap).
            actionRequirementHint(action)?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                    modifier = Modifier.padding(bottom = NothingSpacing.sm),
                )
            }

            ActionConfigContent(
                action = current,
                onActionChange = { current = it },
            )

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
                value = a.mode.name.enumLabel(),
                options = enumLabelList<NightMode>(),
                onSelect = { onActionChange(a.copy(mode = enumByLabel<NightMode>(it))) },
            )
        }

        is Action.SetBrightness -> {
            // Percentage slider (0-100) mapped to 0-255 internally.
            val percent = (a.level.toFloat() / 255f * 100f).toInt().coerceIn(0, 100)
            Text(
                text = "BRIGHTNESS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.mono(),
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
        }

        is Action.SetScreenTimeout -> {
            NothingInput(
                value = a.timeoutMs.toString(),
                onValueChange = { onActionChange(a.copy(timeoutMs = it.toIntOrNull() ?: a.timeoutMs)) },
                label = "Timeout (ms) — 2147483647 = never",
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
                value = a.mode.displayName(),
                options = DndMode.entries.map { it.displayName() },
                onSelect = { sel ->
                    onActionChange(
                        a.copy(mode = DndMode.entries.first { it.displayName() == sel }),
                    )
                },
            )
        }

        is Action.SetVolume -> {
            VolumeRow(
                action = a,
                onChange = onActionChange,
            )
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
                value = a.mode.name.enumLabel(),
                options = enumLabelList<LocationMode>(),
                onSelect = { onActionChange(a.copy(mode = enumByLabel<LocationMode>(it))) },
            )
        }

        is Action.OpenSettingsScreen -> {
            NothingEnumSelector(
                label = "Settings screen",
                value = a.screen.name.enumLabel(),
                options = enumLabelList<SettingsScreen>(),
                onSelect = { onActionChange(a.copy(screen = enumByLabel<SettingsScreen>(it))) },
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
            NothingEnumSelector(
                label = "Preset",
                value = a.preset,
                options = GLYPH_PRESET_NAMES,
                onSelect = { onActionChange(a.copy(preset = it)) },
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

        is Action.GlyphIcon -> {
            var showIconPicker by remember { mutableStateOf(false) }
            GlyphIconField(
                value = a.name,
                onClick = { showIconPicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (showIconPicker) {
                GlyphIconPickerDialog(
                    initial = a.name,
                    onSelect = { onActionChange(a.copy(name = it)); showIconPicker = false },
                    onDismiss = { showIconPicker = false },
                )
            }
        }

        is Action.GlyphNumber -> {
            NothingInput(
                value = a.number.toString(),
                onValueChange = { onActionChange(a.copy(number = it.toIntOrNull() ?: a.number)) },
                label = "Number (0-99)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.GlyphCountdown -> {
            NothingInput(
                value = a.seconds.toString(),
                onValueChange = { onActionChange(a.copy(seconds = it.toIntOrNull() ?: a.seconds)) },
                label = "Seconds (1-599)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.GlyphMusic -> {
            Text(
                text = "Live music-reactive visualizer. Requires RECORD_AUDIO permission. Stays active until Glyph off or another glyph action.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            MusicStyleSelector(
                style = a.style,
                onChange = { onActionChange(a.copy(style = MusicVisualizerStyles.normalize(it))) },
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
            AppPicker(
                currentPackage = a.pkg,
                onPkgChange = { onActionChange(a.copy(pkg = it)) },
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

        is Action.SetRinger -> {
            RingerModeSelector(
                mode = a.mode,
                onChange = { onActionChange(a.copy(mode = it)) },
            )
        }

        is Action.SetNfc -> {
            BooleanRow(
                label = "NFC enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetDataSaver -> {
            BooleanRow(
                label = "Data saver enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetHotspot -> {
            BooleanRow(
                label = "Hotspot enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetAutoSync -> {
            BooleanRow(
                label = "Auto-sync enabled",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
        }

        is Action.SetRefreshRate -> {
            NothingInput(
                value = a.hz.toString(),
                onValueChange = { onActionChange(a.copy(hz = it.toIntOrNull() ?: a.hz)) },
                label = "Hz",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.SetScreenRotation -> {
            NothingEnumSelector(
                label = "Orientation",
                value = a.orientation.name.enumLabel(),
                options = enumLabelList<ScreenOrientation>(),
                onSelect = { onActionChange(a.copy(orientation = enumByLabel<ScreenOrientation>(it))) },
            )
        }

        is Action.MediaControl -> {
            NothingEnumSelector(
                label = "Media command",
                value = a.command.name.enumLabel(),
                options = enumLabelList<MediaCommand>(),
                onSelect = { onActionChange(a.copy(command = enumByLabel<MediaCommand>(it))) },
            )
        }

        is Action.SendSms -> {
            NothingInput(
                value = a.number,
                onValueChange = { onActionChange(a.copy(number = it)) },
                label = "Number",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.text,
                onValueChange = { onActionChange(a.copy(text = it)) },
                label = "Message",
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.WriteSetting -> {
            NothingEnumSelector(
                label = "Namespace",
                value = a.namespace.name.enumLabel(),
                options = enumLabelList<SettingNamespace>(),
                onSelect = { onActionChange(a.copy(namespace = enumByLabel<SettingNamespace>(it))) },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.key,
                onValueChange = { onActionChange(a.copy(key = it)) },
                label = "Key",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.value,
                onValueChange = { onActionChange(a.copy(value = it)) },
                label = "Value",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.SetGlyph -> {
            BooleanRow(
                label = "Glyph on",
                checked = a.on,
                onChange = { onActionChange(a.copy(on = it)) },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.channels?.joinToString(",") ?: "",
                onValueChange = { text ->
                    val list = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                    onActionChange(a.copy(channels = list.ifEmpty { null }))
                },
                label = "LED zones (comma separated, blank = all)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.SetGlyphMatrix -> {
            // Friendly presets instead of raw hex lists; custom stays available.
            var matrixMode by remember(a) {
                mutableStateOf(
                    when {
                        a.restore -> "Off"
                        a.colors.isNullOrEmpty() -> "Off"
                        a.colors!!.all { it == 0xFFFFFF.toInt() } -> "All on"
                        else -> "Custom"
                    },
                )
            }
            NothingEnumSelector(
                label = "Matrix",
                value = matrixMode,
                options = listOf("All on", "Off", "Custom"),
                onSelect = { sel ->
                    matrixMode = sel
                    when (sel) {
                        "All on" ->
                            onActionChange(
                                a.copy(
                                    colors = List(25 * 25) { 0xFFFFFF.toInt() },
                                    restore = false,
                                ),
                            )
                        "Off" -> onActionChange(a.copy(colors = null, restore = false))
                        else -> Unit
                    }
                },
            )
            if (matrixMode == "Custom") {
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                NothingInput(
                    value = a.colors?.joinToString(",") { String.format("#%06X", 0xFFFFFF and it) } ?: "",
                    onValueChange = { text ->
                        val list = text.split(",").mapNotNull { parseColorHex(it.trim()) }
                        onActionChange(a.copy(colors = list.ifEmpty { null }, restore = false))
                    },
                    label = "Colors (comma separated hex, row-major)",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        is Action.GlyphProgress -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                Text(
                    text = "${a.progress}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.mono(),
                    modifier = Modifier.width(56.dp),
                )
                androidx.compose.material3.Slider(
                    value = a.progress.toFloat(),
                    onValueChange = { onActionChange(a.copy(progress = it.toInt().coerceIn(0, 100))) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                )
            }
            BooleanRow(
                label = "Reverse fill",
                checked = a.reverse,
                onChange = { onActionChange(a.copy(reverse = it)) },
            )
        }

        is Action.GlyphAnimate -> {
            NothingEnumSelector(
                label = "Zone",
                value = a.zone ?: "All",
                options = listOf("All", "A", "B", "C", "D", "E"),
                onSelect = { onActionChange(a.copy(zone = if (it == "All") null else it)) },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.periodMs.toString(),
                onValueChange = { onActionChange(a.copy(periodMs = it.toIntOrNull() ?: a.periodMs)) },
                label = "Blink speed (ms per cycle)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.cycles.toString(),
                onValueChange = { onActionChange(a.copy(cycles = it.toIntOrNull() ?: a.cycles)) },
                label = "Repeats",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.channels?.joinToString(",") ?: "",
                onValueChange = { text ->
                    val list = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                    onActionChange(a.copy(channels = list.ifEmpty { null }))
                },
                label = "Advanced — raw LED zone numbers (overrides the zone above)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.GlyphTurnOff,
        is Action.LockScreen,
        is Action.ClearNotifications,
        is Action.TakeScreenshot,
        -> {
            Text(
                text = actionDescription(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }

        else -> {
            Text(
                text = actionDescription(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }
    }
}

/** Named glyph presets understood by the executor's presetFor() — canonical names only. */
internal val GLYPH_PRESET_NAMES =
    listOf(
        "sleep",
        "morning",
        "work",
        "dnd",
        "dnd_off",
        "fired",
        "error",
        "success",
        "charging_start",
        "charging_complete",
        "call",
        "sms",
        "timer",
        "timer_done",
        "notification_low",
        "notification_high",
        "notification_critical",
        "off",
    )



private fun actionTitle(action: Action): String =
    when (action) {
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
        is Action.GlyphIcon -> "Glyph icon"
        is Action.GlyphNumber -> "Glyph number"
        is Action.GlyphCountdown -> "Glyph countdown"
        is Action.GlyphMusic -> "Glyph music"
        is Action.CopyText -> "Copy text"
        is Action.OpenUrl -> "Open URL"
        is Action.LaunchApp -> "Launch app"
        is Action.ShowNotification -> "Show notification"
        is Action.Wait -> "Wait"
        is Action.SetRinger -> "Ringer mode"
        is Action.SetNfc -> "NFC"
        is Action.SetDataSaver -> "Data saver"
        is Action.SetHotspot -> "Hotspot"
        is Action.SetAutoSync -> "Auto-sync"
        is Action.SetRefreshRate -> "Refresh rate"
        is Action.SetScreenRotation -> "Screen rotation"
        is Action.MediaControl -> "Media control"
        is Action.SendSms -> "Send SMS"
        is Action.LockScreen -> "Lock screen"
        is Action.ClearNotifications -> "Clear notifications"
        is Action.TakeScreenshot -> "Take screenshot"
        is Action.WriteSetting -> "Write setting"
        is Action.SetGlyph -> "Glyph"
        is Action.SetGlyphMatrix -> "Glyph matrix"
        is Action.GlyphProgress -> "Glyph progress"
        is Action.GlyphAnimate -> "Glyph animate"
        is Action.GlyphTurnOff -> "Glyph off"
        else -> "Action"
    }

@Composable
private fun BooleanRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val displayLabel =
        if (label.endsWith(" enabled", ignoreCase = true)) {
            label.removeSuffix(" enabled")
        } else {
            label
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onChange(!checked) }
                .padding(vertical = NothingSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = NothingFonts.mono(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            Text(
                text = if (checked) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = NothingColors.accent,
                fontFamily = NothingFonts.mono(),
            )
            NothingToggle(
                checked = checked,
                onCheckedChange = onChange,
            )
        }
    }
}

@Composable
internal fun RingerModeSelector(
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
internal fun MusicStyleSelector(
    style: String,
    onChange: (String) -> Unit,
) {
    val styles = MusicVisualizerStyles.ALL
    styles.forEach { s ->
        RadioOption(
            text = s.replaceFirstChar { it.uppercase() },
            selected = style == s,
            onClick = { onChange(s) },
        )
    }
}

/** Real device maximum for a stream — Media/Ring/Alarm/Notification differ. */
internal fun streamMaxLevel(
    context: android.content.Context,
    stream: VolumeStream,
): Int {
    val audio = context.getSystemService(android.media.AudioManager::class.java)
    return runCatching {
        audio?.getStreamMaxVolume(
            when (stream) {
                VolumeStream.MEDIA -> android.media.AudioManager.STREAM_MUSIC
                VolumeStream.RING -> android.media.AudioManager.STREAM_RING
                VolumeStream.ALARM -> android.media.AudioManager.STREAM_ALARM
                VolumeStream.NOTIFICATION -> android.media.AudioManager.STREAM_NOTIFICATION
            },
        )
    }.getOrNull() ?: 15
}

/** Volume level as a percentage slider — shared by actions and conditions. */
@Composable
internal fun VolumePercentSlider(
    stream: VolumeStream,
    level: Int,
    onLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val maxLevel = remember(stream) { streamMaxLevel(context, stream) }
    val percent = if (maxLevel > 0) (level * 100 / maxLevel).coerceIn(0, 100) else 0

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.width(56.dp),
        )
        androidx.compose.material3.Slider(
            value = percent.toFloat(),
            onValueChange = { v ->
                onLevel((v / 100f * maxLevel).toInt().coerceIn(0, maxLevel))
            },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Volume as a percentage slider against the real stream maximum. */
@Composable
internal fun VolumeRow(
    action: Action.SetVolume,
    onChange: (Action.SetVolume) -> Unit,
) {
    Column {
        NothingEnumSelector(
            label = "Volume for",
            value = action.stream.name.enumLabel(),
            options = enumLabelList<VolumeStream>(),
            onSelect = { onChange(action.copy(stream = enumByLabel<VolumeStream>(it))) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        VolumePercentSlider(
            stream = action.stream,
            level = action.level,
            onLevel = { onChange(action.copy(level = it)) },
        )
    }
}

internal fun parseColorHex(text: String): Int? =
    runCatching {
        val hex = text.removePrefix("#").removePrefix("0x")
        if (hex.length != 6) return@runCatching null
        Integer.parseInt(hex, 16) or 0xFF000000.toInt()
    }.getOrNull()

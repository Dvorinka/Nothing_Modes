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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AodMode
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.MusicVisualizerStyles
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.ui.components.ContactNumberPickerButton
import com.tdvorak.nothingmodes.ui.components.RefreshRateSelector
import com.tdvorak.nothingmodes.ui.components.WallpaperActionEditor
import com.tdvorak.nothingmodes.ui.components.WriteSettingSelector
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun ActionConfigScreen(
    actionJson: String,
    navController: NavController,
) {
    val initial =
        remember(actionJson) {
            runCatching { Json.decodeFromString<Action>(actionJson) }.getOrNull()
                ?: Action.SetWifi(true)
        }
    var action by remember { mutableStateOf(initial) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Configure Action",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(NothingSpacing.md),
        ) {
            NothingCardLarge(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(NothingSpacing.lg))

                when (val a = action) {
                    is Action.SetWifi -> {
                        BooleanRow(
                            label = "Wi-Fi enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetBluetooth -> {
                        BooleanRow(
                            label = "Bluetooth enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetMobileData -> {
                        BooleanRow(
                            label = "Mobile data enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetAirplaneMode -> {
                        BooleanRow(
                            label = "Airplane mode enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetDarkMode -> {
                        BooleanRow(
                            label = "Dark mode enabled",
                            checked = a.mode == NightMode.ON,
                            onChange = { action = a.copy(mode = if (it) NightMode.ON else NightMode.OFF) },
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
                            Slider(
                                value = percent.toFloat(),
                                onValueChange = { v ->
                                    action = a.copy(level = (v / 100f * 255f).toInt().coerceIn(0, 255))
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
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetExtraDim -> {
                        BooleanRow(
                            label = "Extra dim enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetScreenTimeout -> {
                        NothingInput(
                            value = a.timeoutMs.toString(),
                            onValueChange = { action = a.copy(timeoutMs = it.toIntOrNull() ?: a.timeoutMs) },
                            label = "Timeout (ms)",
                        )
                    }

                    is Action.SetWallpaper -> {
                        WallpaperActionEditor(
                            action = a,
                            onActionChange = { action = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is Action.SetAlwaysOnDisplay -> {
                        NothingEnumSelector(
                            label = "AOD mode",
                            value = a.mode.name,
                            options = AodMode.entries.map { it.name },
                            onSelect = { sel ->
                                action = a.copy(mode = AodMode.valueOf(sel))
                            },
                        )
                    }

                    is Action.SetDnd -> {
                        NothingEnumSelector(
                            label = "DND mode",
                            value = a.mode.displayName(),
                            options = DndMode.entries.map { it.displayName() },
                            onSelect = { sel ->
                                action =
                                    a.copy(mode = DndMode.entries.first { it.displayName() == sel })
                            },
                        )
                    }

                    is Action.SetVolume -> {
                        VolumeRow(
                            action = a,
                            onChange = { action = it },
                        )
                    }

                    is Action.Vibrate -> {
                        NothingInput(
                            value = a.durationMs.toString(),
                            onValueChange = { action = a.copy(durationMs = it.toIntOrNull() ?: a.durationMs) },
                            label = "Duration (ms)",
                        )
                    }

                    is Action.SetAutoRotate -> {
                        BooleanRow(
                            label = "Auto-rotate enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetBatterySaver -> {
                        BooleanRow(
                            label = "Battery saver enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.SetLocationMode -> {
                        NothingEnumSelector(
                            label = "Location mode",
                            value = a.mode.name.enumLabel(),
                            options = enumLabelList<LocationMode>(),
                            onSelect = { action = a.copy(mode = enumByLabel<LocationMode>(it)) },
                        )
                    }

                    is Action.OpenSettingsScreen -> {
                        NothingEnumSelector(
                            label = "Settings screen",
                            value = a.screen.name.enumLabel(),
                            options = enumLabelList<SettingsScreen>(),
                            onSelect = { action = a.copy(screen = enumByLabel<SettingsScreen>(it)) },
                        )
                    }

                    is Action.SetFlashlight -> {
                        BooleanRow(
                            label = "Flashlight enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                    }

                    is Action.GlyphPreset -> {
                        NothingEnumSelector(
                            label = "Preset",
                            value = a.preset,
                            options = GLYPH_PRESET_NAMES,
                            onSelect = { action = a.copy(preset = it) },
                        )
                    }

                    is Action.GlyphText -> {
                        NothingInput(
                            value = a.text,
                            onValueChange = { action = a.copy(text = it) },
                            label = "Text",
                        )
                    }

                    is Action.GlyphScrollingText -> {
                        NothingInput(
                            value = a.text,
                            onValueChange = { action = a.copy(text = it) },
                            label = "Scrolling text",
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
                                onSelect = {
                                    action = a.copy(name = it)
                                    showIconPicker = false
                                },
                                onDismiss = { showIconPicker = false },
                            )
                        }
                    }

                    is Action.GlyphNumber -> {
                        NothingInput(
                            value = a.number.toString(),
                            onValueChange = { action = a.copy(number = it.toIntOrNull() ?: a.number) },
                            label = "Number (0-99)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is Action.GlyphCountdown -> {
                        NothingInput(
                            value = a.seconds.toString(),
                            onValueChange = { action = a.copy(seconds = it.toIntOrNull() ?: a.seconds) },
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
                            onChange = { action = a.copy(style = MusicVisualizerStyles.normalize(it)) },
                        )
                    }

                    is Action.CopyText -> {
                        NothingInput(
                            value = a.text,
                            onValueChange = { action = a.copy(text = it) },
                            label = "Text to copy",
                        )
                    }

                    is Action.OpenUrl -> {
                        NothingInput(
                            value = a.url,
                            onValueChange = { action = a.copy(url = it) },
                            label = "URL",
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        AppPicker(
                            currentPackage = a.packageName ?: "",
                            onPkgChange = { pkg ->
                                action = a.copy(packageName = pkg.takeIf { it.isNotBlank() })
                            },
                            browserOnly = true,
                        )
                    }

                    is Action.LaunchApp -> {
                        MultiAppPicker(
                            currentPackages = a.packages,
                            onChange = { action = a.copy(packages = it) },
                        )
                    }

                    is Action.ShowNotification -> {
                        NothingInput(
                            value = a.title,
                            onValueChange = { action = a.copy(title = it) },
                            label = "Title",
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.text,
                            onValueChange = { action = a.copy(text = it) },
                            label = "Body",
                            singleLine = false,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingEnumSelector(
                            label = "Glyph pattern (optional)",
                            value = a.glyphPreset.orEmpty(),
                            options = listOf("") + GLYPH_PRESET_NAMES,
                            onSelect = { action = a.copy(glyphPreset = it.ifBlank { null }) },
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.glyphTimeoutMs.toString(),
                            onValueChange = { action = a.copy(glyphTimeoutMs = it.toIntOrNull()?.coerceIn(0, 60_000) ?: a.glyphTimeoutMs) },
                            label = "Glyph timeout (ms, 0 = manual)",
                        )
                    }

                    is Action.Wait -> {
                        NothingInput(
                            value = a.durationMs.toString(),
                            onValueChange = { action = a.copy(durationMs = it.toLongOrNull() ?: a.durationMs) },
                            label = "Duration (ms)",
                        )
                    }

                    is Action.SetRinger -> {
                        RingerModeSelector(
                            mode = a.mode,
                            onChange = { action = a.copy(mode = it) },
                        )
                    }

                    is Action.SetNfc ->
                        BooleanRow(
                            label = "NFC enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )

                    is Action.SetDataSaver ->
                        BooleanRow(
                            label = "Data saver enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )

                    is Action.SetHotspot ->
                        BooleanRow(
                            label = "Hotspot enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )

                    is Action.SetAutoSync ->
                        BooleanRow(
                            label = "Auto-sync enabled",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )

                    is Action.SetRefreshRate ->
                        RefreshRateSelector(
                            hz = a.hz,
                            onChange = { action = a.copy(hz = it) },
                        )

                    is Action.SetScreenRotation ->
                        NothingEnumSelector(
                            label = "Orientation",
                            value = a.orientation.name.enumLabel(),
                            options = enumLabelList<ScreenOrientation>(),
                            onSelect = { action = a.copy(orientation = enumByLabel<ScreenOrientation>(it)) },
                        )

                    is Action.MediaControl ->
                        NothingEnumSelector(
                            label = "Media command",
                            value = a.command.name.enumLabel(),
                            options = enumLabelList<MediaCommand>(),
                            onSelect = { action = a.copy(command = enumByLabel<MediaCommand>(it)) },
                        )

                    is Action.SendSms -> {
                        NothingInput(
                            value = a.number,
                            onValueChange = { action = a.copy(number = it) },
                            label = "Number",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        ContactNumberPickerButton(
                            onNumber = { action = a.copy(number = it) },
                            text = "Pick contact",
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.text,
                            onValueChange = { action = a.copy(text = it) },
                            label = "Message",
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is Action.WriteSetting -> {
                        WriteSettingSelector(
                            action = a,
                            onChange = { action = it },
                        )
                    }

                    is Action.SetGlyph -> {
                        BooleanRow(
                            label = "Glyph on",
                            checked = a.on,
                            onChange = { action = a.copy(on = it) },
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.channels?.joinToString(",") ?: "",
                            onValueChange = { text ->
                                val list = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                                action = a.copy(channels = list.ifEmpty { null })
                            },
                            label = "LED zones (comma separated)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is Action.SetGlyphMatrix -> {
                        NothingInput(
                            value = a.colors?.joinToString(",") { String.format("#%06X", 0xFFFFFF and it) } ?: "",
                            onValueChange = { text ->
                                val list = text.split(",").mapNotNull { parseColorHex(it.trim()) }
                                action = a.copy(colors = list.ifEmpty { null })
                            },
                            label = "Colors (comma separated hex)",
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                            Slider(
                                value = a.progress.toFloat(),
                                onValueChange = { action = a.copy(progress = it.toInt().coerceIn(0, 100)) },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        BooleanRow(
                            label = "Reverse fill",
                            checked = a.reverse,
                            onChange = { action = a.copy(reverse = it) },
                        )
                    }

                    is Action.GlyphAnimate -> {
                        NothingInput(
                            value = a.periodMs.toString(),
                            onValueChange = { action = a.copy(periodMs = it.toIntOrNull() ?: a.periodMs) },
                            label = "Period (ms)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.cycles.toString(),
                            onValueChange = { action = a.copy(cycles = it.toIntOrNull() ?: a.cycles) },
                            label = "Cycles",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.intervalMs.toString(),
                            onValueChange = { action = a.copy(intervalMs = it.toIntOrNull() ?: a.intervalMs) },
                            label = "Interval (ms)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.zone ?: "",
                            onValueChange = { action = a.copy(zone = it.ifBlank { null }) },
                            label = "Zone (A/B/C/D/E, blank = all)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        NothingInput(
                            value = a.channels?.joinToString(",") ?: "",
                            onValueChange = { text ->
                                val list = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                                action = a.copy(channels = list.ifEmpty { null })
                            },
                            label = "LED zones (comma separated, overrides zone)",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is Action.GlyphTurnOff,
                    is Action.ClearNotifications,
                    -> {
                        Text(
                            text = actionDescription(action),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                    }

                    is Action.LockScreen -> {
                        Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm)) {
                            Text(
                                text = actionDescription(action),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                            BooleanRow(
                                label = "Override: try anyway",
                                checked = a.force,
                                onChange = { action = a.copy(force = it) },
                            )
                        }
                    }

                    is Action.TakeScreenshot -> {
                        Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm)) {
                            Text(
                                text = actionDescription(action),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                            BooleanRow(
                                label = "Override: try anyway",
                                checked = a.force,
                                onChange = { action = a.copy(force = it) },
                            )
                        }
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

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
                NothingPillButton(
                    text = "Done",
                    onClick = {
                        val result = Json.encodeToString(action)
                        navController.previousBackStackEntry?.savedStateHandle?.set("action_result", result)
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

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
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
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
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
                fontFamily = GeistSans,
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
            AppPickerField(
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
                value = a.orientation.name,
                options = ScreenOrientation.entries.map { it.name },
                onSelect = { onActionChange(a.copy(orientation = ScreenOrientation.valueOf(it))) },
            )
        }

        is Action.MediaControl -> {
            NothingEnumSelector(
                label = "Media command",
                value = a.command.name,
                options = MediaCommand.entries.map { it.name },
                onSelect = { onActionChange(a.copy(command = MediaCommand.valueOf(it))) },
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
                value = a.namespace.name,
                options = SettingNamespace.entries.map { it.name },
                onSelect = { onActionChange(a.copy(namespace = SettingNamespace.valueOf(it))) },
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
                label = "Channels (comma separated)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.SetGlyphMatrix -> {
            NothingInput(
                value = a.colors?.joinToString(",") { String.format("#%06X", 0xFFFFFF and it) } ?: "",
                onValueChange = { text ->
                    val list = text.split(",").mapNotNull { parseColorHex(it.trim()) }
                    onActionChange(a.copy(colors = list.ifEmpty { null }))
                },
                label = "Colors (comma separated hex)",
                modifier = Modifier.fillMaxWidth(),
            )
            BooleanRow(
                label = "Restore previous",
                checked = a.restore,
                onChange = { onActionChange(a.copy(restore = it)) },
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
                    fontFamily = SpaceMono,
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
            NothingInput(
                value = a.periodMs.toString(),
                onValueChange = { onActionChange(a.copy(periodMs = it.toIntOrNull() ?: a.periodMs)) },
                label = "Period (ms)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.cycles.toString(),
                onValueChange = { onActionChange(a.copy(cycles = it.toIntOrNull() ?: a.cycles)) },
                label = "Cycles",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.intervalMs.toString(),
                onValueChange = { onActionChange(a.copy(intervalMs = it.toIntOrNull() ?: a.intervalMs)) },
                label = "Interval (ms)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.zone ?: "",
                onValueChange = { onActionChange(a.copy(zone = it.ifBlank { null })) },
                label = "Zone (A/B/C/D/E, blank = all)",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = a.channels?.joinToString(",") ?: "",
                onValueChange = { text ->
                    val list = text.split(",").mapNotNull { it.trim().toIntOrNull() }
                    onActionChange(a.copy(channels = list.ifEmpty { null }))
                },
                label = "Channels (comma separated, overrides zone)",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is Action.GlyphTurnOff,
        is Action.LockScreen,
        is Action.ClearNotifications,
        is Action.TakeScreenshot -> {
            Text(
                text = actionDescription(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
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
            .size(32.dp, 2.dp)
            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(1.dp))
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

internal fun parseColorHex(text: String): Int? = runCatching {
    val hex = text.removePrefix("#").removePrefix("0x")
    if (hex.length != 6) return@runCatching null
    Integer.parseInt(hex, 16) or 0xFF000000.toInt()
}.getOrNull()

// ─── Installed App Picker Field ──────────────────────────────────────────────

@Composable
private fun AppPickerField(
    currentPackage: String,
    onPkgChange: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showList by remember { mutableStateOf(false) }

    val installedApps = remember {
        runCatching {
            val pm = context.packageManager
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0)
                .map { ri ->
                    ri.loadLabel(pm).toString() to ri.activityInfo.packageName
                }
                .sortedBy { it.first.lowercase() }
        }.getOrDefault(emptyList())
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.first.contains(searchQuery, ignoreCase = true) ||
                it.second.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedLabel = installedApps.find { it.second == currentPackage }?.first ?: currentPackage

    Column {
        Text(
            text = "Selected app",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = SpaceMono,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showList = !showList },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedLabel.ifBlank { "Tap to select an app" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                Text(
                    text = if (showList) "[CLOSE]" else "[OPEN]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }
        }

        if (showList) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search",
                placeholder = "Search apps...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                items(filteredApps, key = { it.second }) { (label, pkg) ->
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPkgChange(pkg)
                                showList = false
                                searchQuery = ""
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(NothingSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (pkg == currentPackage) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(20.dp)
                                        .background(NothingColors.accent),
                                )
                                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SpaceMono,
                            )
                        }
                    }
                }
            }
        }
    }
}

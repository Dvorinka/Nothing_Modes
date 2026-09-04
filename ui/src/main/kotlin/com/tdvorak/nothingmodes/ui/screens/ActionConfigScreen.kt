package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Composable
fun ActionConfigScreen(
    actionJson: String,
    navController: NavController,
) {
    val initial = remember(actionJson) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(NothingSpacing.md),
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
                    NothingEnumSelector(
                        label = "Dark mode",
                        value = a.mode.name,
                        options = NightMode.entries.map { it.name },
                        onSelect = { action = a.copy(mode = NightMode.valueOf(it)) },
                    )
                }

                is Action.SetBrightness -> {
                    NothingInput(
                        value = a.level.toString(),
                        onValueChange = { action = a.copy(level = it.toIntOrNull() ?: a.level) },
                        label = "Level (0..255)",
                    )
                    BooleanRow(
                        label = "Restore previous",
                        checked = a.restore,
                        onChange = { action = a.copy(restore = it) },
                    )
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
                    BooleanRow(
                        label = "Restore previous",
                        checked = a.restore,
                        onChange = { action = a.copy(restore = it) },
                    )
                }

                is Action.SetScreenTimeout -> {
                    NothingInput(
                        value = a.timeoutMs.toString(),
                        onValueChange = { action = a.copy(timeoutMs = it.toIntOrNull() ?: a.timeoutMs) },
                        label = "Timeout (ms)",
                    )
                }

                is Action.SetAlwaysOnDisplay -> {
                    BooleanRow(
                        label = "Always-on display enabled",
                        checked = a.on,
                        onChange = { action = a.copy(on = it) },
                    )
                }

                is Action.SetDnd -> {
                    NothingEnumSelector(
                        label = "DND mode",
                        value = a.mode.name,
                        options = DndMode.entries.map { it.name },
                        onSelect = { action = a.copy(mode = DndMode.valueOf(it)) },
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
                            onSelect = { action = a.copy(stream = VolumeStream.valueOf(it)) },
                            modifier = Modifier.weight(1f),
                        )
                        NothingInput(
                            value = a.level.toString(),
                            onValueChange = { action = a.copy(level = it.toIntOrNull() ?: a.level) },
                            label = "Level",
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                        value = a.mode.name,
                        options = LocationMode.entries.map { it.name },
                        onSelect = { action = a.copy(mode = LocationMode.valueOf(it)) },
                    )
                }

                is Action.OpenSettingsScreen -> {
                    NothingEnumSelector(
                        label = "Settings screen",
                        value = a.screen.name,
                        options = SettingsScreen.entries.map { it.name },
                        onSelect = { action = a.copy(screen = SettingsScreen.valueOf(it)) },
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
                    NothingInput(
                        value = a.preset,
                        onValueChange = { action = a.copy(preset = it) },
                        label = "Preset name",
                    )
                }

                is Action.GlyphText -> {
                    NothingInput(
                        value = a.text,
                        onValueChange = { action = a.copy(text = it) },
                        label = "Text",
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
                }

                is Action.LaunchApp -> {
                    NothingInput(
                        value = a.pkg,
                        onValueChange = { action = a.copy(pkg = it) },
                        label = "Package name",
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
                }

                is Action.Wait -> {
                    NothingInput(
                        value = a.durationMs.toString(),
                        onValueChange = { action = a.copy(durationMs = it.toLongOrNull() ?: a.durationMs) },
                        label = "Duration (ms)",
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

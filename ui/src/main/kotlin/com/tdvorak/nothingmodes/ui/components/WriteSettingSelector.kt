package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.ui.screens.enumByLabel
import com.tdvorak.nothingmodes.ui.screens.enumLabel
import com.tdvorak.nothingmodes.ui.screens.enumLabelList
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

internal val writeSettingPresets =
    listOf(
        "Custom" to null,
        "Animation scale 0.5x" to Action.WriteSetting(SettingNamespace.GLOBAL, "animator_duration_scale", "0.5"),
        "Animation scale 1x" to Action.WriteSetting(SettingNamespace.GLOBAL, "animator_duration_scale", "1.0"),
        "Font scale 1.0" to Action.WriteSetting(SettingNamespace.GLOBAL, "font_scale", "1.0"),
        "Font scale 1.25" to Action.WriteSetting(SettingNamespace.GLOBAL, "font_scale", "1.25"),
        "Show taps" to Action.WriteSetting(SettingNamespace.SYSTEM, "show_touches", "1"),
        "Hide taps" to Action.WriteSetting(SettingNamespace.SYSTEM, "show_touches", "0"),
    )

@Composable
fun WriteSettingSelector(
    action: Action.WriteSetting,
    onChange: (Action.WriteSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customLabel = "Custom"
    val selected = writeSettingPresets.firstOrNull { it.second == action }?.first ?: customLabel
    Column(modifier = modifier) {
        NothingEnumSelector(
            label = "Preset",
            value = selected,
            options = writeSettingPresets.map { it.first },
            onSelect = { label ->
                writeSettingPresets.firstOrNull { it.first == label }?.second?.let { onChange(it) }
            },
        )
        if (selected == customLabel) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Text(
                text = "Custom key/value. Global/secure writes need Shizuku; system writes need Write Settings permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingEnumSelector(
                label = "Namespace",
                value = action.namespace.name.enumLabel(),
                options = enumLabelList<SettingNamespace>(),
                onSelect = { onChange(action.copy(namespace = enumByLabel<SettingNamespace>(it))) },
                infoText = "System = per-user settings. Secure/Global = device-wide — these need Shizuku.",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = action.key,
                onValueChange = { onChange(action.copy(key = it)) },
                label = "Key",
                infoText = "The Android settings key to write, e.g. animator_duration_scale or font_scale.",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = action.value,
                onValueChange = { onChange(action.copy(value = it)) },
                label = "Value",
                infoText = "The value to write — usually a number or 0/1 for on/off.",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

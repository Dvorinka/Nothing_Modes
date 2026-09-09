package com.tdvorak.nothingmodes.ui.components

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

private val fallbackRates = listOf(60, 90, 120, 144)

/** Device-supported refresh rates, falling back to common presets. */
@Composable
fun rememberRefreshRatePresets(): List<Pair<String, Int>> {
    val context = LocalContext.current
    return remember {
        val rates =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
                display
                    ?.supportedModes
                    ?.map { it.refreshRate.toInt() }
                    ?.distinct()
                    ?.sorted()
                    ?.takeIf { it.isNotEmpty() }
            } else {
                null
            } ?: fallbackRates
        rates.map { "$it Hz" to it }
    }
}

@Composable
fun RefreshRateSelector(
    hz: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customLabel = "Custom"
    val presets = rememberRefreshRatePresets()
    val selected = presets.firstOrNull { it.second == hz }?.first ?: customLabel
    var customHz by remember(hz) { mutableStateOf(hz.toString()) }

    Column(modifier = modifier) {
        NothingEnumSelector(
            label = "Refresh rate",
            value = selected,
            options = presets.map { it.first } + customLabel,
            onSelect = { label ->
                presets.firstOrNull { it.first == label }?.let { onChange(it.second) }
            },
        )
        if (selected == customLabel) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = customHz,
                onValueChange = {
                    customHz = it
                    it.toIntOrNull()?.let { v -> onChange(v) }
                },
                label = "Custom Hz",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

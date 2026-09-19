package com.tdvorak.nothingmodes.quicksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingModesThemeDynamic
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

/**
 * Floating panel for live ultra-dim intensity — opened from the persistent
 * notification. Translucent so the user watches the screen dim as they drag.
 * Adjustments are temporary: "Reset" snaps back to the level the mode (or
 * tile) set, "Turn off" removes the overlay. Tap outside to dismiss.
 */
class DimAdjustActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Stale notification after process death — nothing to adjust.
        if (!UltraDimController.isActive) {
            finish()
            return
        }
        setContent { NothingModesThemeDynamic { DimAdjustPanel() } }
    }

    @Composable
    private fun DimAdjustPanel() {
        var percent by remember { mutableIntStateOf(UltraDimController.percent.coerceIn(5, 95)) }
        val scrimInteraction = remember { MutableInteractionSource() }
        val cardInteraction = remember { MutableInteractionSource() }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = scrimInteraction, indication = null) { finish() },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        // Consume taps so the scrim doesn't close the panel.
                        .clickable(interactionSource = cardInteraction, indication = null) {},
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ULTRA DIM",
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
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = NothingFonts.mono(),
                        )
                        Slider(
                            value = percent.toFloat(),
                            onValueChange = { v ->
                                percent = v.toInt().coerceIn(5, 95)
                                UltraDimController.adjustTo(this@DimAdjustActivity, percent)
                            },
                            valueRange = 5f..95f,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val baseline = UltraDimController.baselinePercent
                        TextButton(
                            onClick = {
                                UltraDimController.resetToBaseline(this@DimAdjustActivity)
                                percent = UltraDimController.baselinePercent.coerceIn(5, 95)
                            },
                            enabled = baseline > 0 && baseline != UltraDimController.percent,
                        ) {
                            Text("Reset to $baseline%")
                        }
                        TextButton(
                            onClick = {
                                UltraDimController.hide(this@DimAdjustActivity)
                                finish()
                            },
                        ) {
                            Text("Turn off")
                        }
                    }
                }
            }
        }
    }
}

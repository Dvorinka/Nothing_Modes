package com.tdvorak.nothingmodes.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.StateKeys

internal data class NumericStateItem(
    val key: String,
    val label: String,
    val unit: String,
    val category: String,
    val icon: ImageVector,
    val defaultValue: Double,
    val defaultOp: CmpOp,
)

internal val NUMERIC_STATE_ITEMS: List<NumericStateItem> =
    listOf(
        NumericStateItem(
            StateKeys.BRIGHTNESS,
            "Brightness",
            "0-255",
            "Display",
            Icons.Outlined.BrightnessLow,
            128.0,
            CmpOp.GTE,
        ),
        NumericStateItem(
            StateKeys.REFRESH_RATE,
            "Refresh rate",
            "Hz",
            "Display",
            Icons.Outlined.Speed,
            60.0,
            CmpOp.GTE,
        ),
        NumericStateItem(
            StateKeys.SCREEN_TIMEOUT,
            "Screen timeout",
            "ms",
            "Display",
            Icons.Outlined.Bedtime,
            30000.0,
            CmpOp.GTE,
        ),
    )

internal fun numericStateLabel(key: String): String =
    NUMERIC_STATE_ITEMS.find { it.key == key }?.label
        ?: key.replace("_", " ").replaceFirstChar { it.uppercase() }

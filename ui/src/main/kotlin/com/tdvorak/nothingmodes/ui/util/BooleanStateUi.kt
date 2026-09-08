package com.tdvorak.nothingmodes.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.tdvorak.nothingmodes.engine.model.StateKeys

internal data class BooleanStateItem(
    val key: String,
    val label: String,
    val category: String,
    val icon: ImageVector,
)

internal val BOOLEAN_STATE_ITEMS: List<BooleanStateItem> =
    listOf(
        BooleanStateItem(
            StateKeys.DEVICE_LOCKED,
            "Device locked",
            "Device status",
            Icons.Outlined.ScreenLockPortrait,
        ),
        BooleanStateItem(
            StateKeys.WIFI_RADIO,
            "Wi-Fi radio on",
            "Connections",
            Icons.Outlined.Wifi,
        ),
        BooleanStateItem(
            StateKeys.BLUETOOTH_RADIO,
            "Bluetooth radio on",
            "Connections",
            Icons.Outlined.Bluetooth,
        ),
        BooleanStateItem(
            StateKeys.MOBILE_DATA,
            "Mobile data on",
            "Connections",
            Icons.Outlined.SignalCellularAlt,
        ),
        BooleanStateItem(
            StateKeys.AOD_ENABLED,
            "Always-on display on",
            "Device status",
            Icons.Outlined.WbSunny,
        ),
    )

internal fun booleanStateLabel(key: String): String =
    BOOLEAN_STATE_ITEMS.find { it.key == key }?.label
        ?: key.replace("_", " ").replaceFirstChar { it.uppercase() }

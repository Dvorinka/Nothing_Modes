package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MobileScreenShare
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.NothingDotGrid
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private data class ActionItem(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val iconBg: Color,
    val action: Action,
)

@Composable
fun ActionCatalogScreen(
    navController: NavController,
) {
    var search by remember { mutableStateOf("") }

    val items = remember {
        listOf(
            ActionItem(
                label = "Wi-Fi",
                category = "Connections",
                icon = Icons.Default.Wifi,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SetWifi(true),
            ),
            ActionItem(
                label = "Bluetooth",
                category = "Connections",
                icon = Icons.Default.Bluetooth,
                iconBg = Color(0xFF5B9BF6),
                action = Action.SetBluetooth(true),
            ),
            ActionItem(
                label = "Mobile data",
                category = "Connections",
                icon = Icons.Default.SignalCellular4Bar,
                iconBg = Color(0xFFD4A843),
                action = Action.SetMobileData(true),
            ),
            ActionItem(
                label = "Airplane mode",
                category = "Connections",
                icon = Icons.Default.Flight,
                iconBg = Color(0xFFD71921),
                action = Action.SetAirplaneMode(true),
            ),
            ActionItem(
                label = "Dark mode",
                category = "Display",
                icon = Icons.Default.DarkMode,
                iconBg = Color(0xFF5B9BF6),
                action = Action.SetDarkMode(NightMode.OFF),
            ),
            ActionItem(
                label = "Brightness",
                category = "Display",
                icon = Icons.Default.Brightness6,
                iconBg = Color(0xFFD4A843),
                action = Action.SetBrightness(128, restore = true),
            ),
            ActionItem(
                label = "Auto brightness",
                category = "Display",
                icon = Icons.Default.Lightbulb,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SetAutoBrightness(true),
            ),
            ActionItem(
                label = "Extra dim",
                category = "Display",
                icon = Icons.Default.Brightness6,
                iconBg = Color(0xFF9B9B9B),
                action = Action.SetExtraDim(true, restore = true),
            ),
            ActionItem(
                label = "Screen timeout",
                category = "Display",
                icon = Icons.Default.Timer,
                iconBg = Color(0xFFD71921),
                action = Action.SetScreenTimeout(30_000),
            ),
            ActionItem(
                label = "Always-on display",
                category = "Display",
                icon = Icons.Default.PhoneAndroid,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SetAlwaysOnDisplay(true),
            ),
            ActionItem(
                label = "Do not disturb",
                category = "Sound",
                icon = Icons.Default.Notifications,
                iconBg = Color(0xFFD71921),
                action = Action.SetDnd(DndMode.OFF),
            ),
            ActionItem(
                label = "Volume",
                category = "Sound",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBg = Color(0xFF5B9BF6),
                action = Action.SetVolume(VolumeStream.MEDIA, 8),
            ),
            ActionItem(
                label = "Vibrate",
                category = "Sound",
                icon = Icons.Default.Vibration,
                iconBg = Color(0xFFD4A843),
                action = Action.Vibrate(500),
            ),
            ActionItem(
                label = "Ringer mode",
                category = "Sound",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SetRinger("normal"),
            ),
            ActionItem(
                label = "Flashlight",
                category = "Sound",
                icon = Icons.Default.FlashlightOn,
                iconBg = Color(0xFF9B9B9B),
                action = Action.SetFlashlight(true),
            ),
            ActionItem(
                label = "Auto-rotate",
                category = "System",
                icon = Icons.Default.ScreenRotation,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SetAutoRotate(true),
            ),
            ActionItem(
                label = "Battery saver",
                category = "System",
                icon = Icons.Default.PowerSettingsNew,
                iconBg = Color(0xFFD71921),
                action = Action.SetBatterySaver(true),
            ),
            ActionItem(
                label = "Data saver",
                category = "System",
                icon = Icons.Default.SignalCellular4Bar,
                iconBg = Color(0xFFD4A843),
                action = Action.SetDataSaver(true),
            ),
            ActionItem(
                label = "Mobile hotspot",
                category = "System",
                icon = Icons.Default.Wifi,
                iconBg = Color(0xFF5B9BF6),
                action = Action.SetHotspot(true),
            ),
            ActionItem(
                label = "NFC",
                category = "System",
                icon = Icons.Default.Bluetooth,
                iconBg = Color(0xFF9B9B9B),
                action = Action.SetNfc(true),
            ),
            ActionItem(
                label = "Location mode",
                category = "System",
                icon = Icons.Default.LocationOn,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SetLocationMode(LocationMode.HIGH_ACCURACY),
            ),
            ActionItem(
                label = "Screen rotation",
                category = "System",
                icon = Icons.Default.ScreenRotation,
                iconBg = Color(0xFF5B9BF6),
                action = Action.SetScreenRotation(ScreenOrientation.AUTO),
            ),
            ActionItem(
                label = "Refresh rate",
                category = "System",
                icon = Icons.Default.Settings,
                iconBg = Color(0xFFD71921),
                action = Action.SetRefreshRate(60),
            ),
            ActionItem(
                label = "Auto-sync",
                category = "System",
                icon = Icons.Default.Snooze,
                iconBg = Color(0xFFD4A843),
                action = Action.SetAutoSync(true),
            ),
            ActionItem(
                label = "Lock screen",
                category = "System",
                icon = Icons.Default.Lock,
                iconBg = Color(0xFF4A9E5C),
                action = Action.LockScreen,
            ),
            ActionItem(
                label = "Screenshot",
                category = "System",
                icon = Icons.AutoMirrored.Filled.MobileScreenShare,
                iconBg = Color(0xFF5B9BF6),
                action = Action.TakeScreenshot,
            ),
            ActionItem(
                label = "Clear notifications",
                category = "System",
                icon = Icons.Default.Notifications,
                iconBg = Color(0xFFD71921),
                action = Action.ClearNotifications,
            ),
            ActionItem(
                label = "Show notification",
                category = "Apps",
                icon = Icons.Default.Campaign,
                iconBg = Color(0xFFD4A843),
                action = Action.ShowNotification("", ""),
            ),
            ActionItem(
                label = "Copy text",
                category = "Apps",
                icon = Icons.Default.ContentCopy,
                iconBg = Color(0xFF9B9B9B),
                action = Action.CopyText(""),
            ),
            ActionItem(
                label = "Open URL",
                category = "Apps",
                icon = Icons.Default.Link,
                iconBg = Color(0xFF5B9BF6),
                action = Action.OpenUrl(""),
            ),
            ActionItem(
                label = "Launch app",
                category = "Apps",
                icon = Icons.Default.OpenInBrowser,
                iconBg = Color(0xFF4A9E5C),
                action = Action.LaunchApp(""),
            ),
            ActionItem(
                label = "Open settings",
                category = "Apps",
                icon = Icons.Default.Settings,
                iconBg = Color(0xFF9B9B9B),
                action = Action.OpenSettingsScreen(SettingsScreen.SETTINGS, null),
            ),
            ActionItem(
                label = "Media control",
                category = "Apps",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBg = Color(0xFFD71921),
                action = Action.MediaControl(MediaCommand.PLAY_PAUSE),
            ),
            ActionItem(
                label = "Wait",
                category = "Apps",
                icon = Icons.Default.Snooze,
                iconBg = Color(0xFFD4A843),
                action = Action.Wait(1000),
            ),
            ActionItem(
                label = "Send SMS",
                category = "Apps",
                icon = Icons.Default.Sms,
                iconBg = Color(0xFF4A9E5C),
                action = Action.SendSms("", ""),
            ),
            ActionItem(
                label = "Glyph",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                iconBg = Color(0xFF9B9B9B),
                action = Action.SetGlyph(true),
            ),
            ActionItem(
                label = "Glyph matrix",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                iconBg = Color(0xFF5B9BF6),
                action = Action.SetGlyphMatrix(null, restore = false),
            ),
            ActionItem(
                label = "Glyph preset",
                category = "Glyph",
                icon = Icons.Default.Lightbulb,
                iconBg = Color(0xFFD4A843),
                action = Action.GlyphPreset("sleep"),
            ),
            ActionItem(
                label = "Glyph text",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                iconBg = Color(0xFF5B9BF6),
                action = Action.GlyphText(""),
            ),
            ActionItem(
                label = "Glyph scrolling",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                iconBg = Color(0xFF4A9E5C),
                action = Action.GlyphScrollingText(""),
            ),
            ActionItem(
                label = "Glyph progress",
                category = "Glyph",
                icon = Icons.Default.Timer,
                iconBg = Color(0xFFD71921),
                action = Action.GlyphProgress(50),
            ),
            ActionItem(
                label = "Glyph animate",
                category = "Glyph",
                icon = Icons.Default.Lightbulb,
                iconBg = Color(0xFF5B9BF6),
                action = Action.GlyphAnimate(),
            ),
            ActionItem(
                label = "Glyph turn off",
                category = "Glyph",
                icon = Icons.Default.PowerSettingsNew,
                iconBg = Color(0xFF9B9B9B),
                action = Action.GlyphTurnOff,
            ),
            ActionItem(
                label = "Write setting",
                category = "Advanced",
                icon = Icons.Default.Settings,
                iconBg = Color(0xFFD71921),
                action = Action.WriteSetting(SettingNamespace.SYSTEM, "", ""),
            ),
        )
    }

    val filtered = remember(search, items) {
        if (search.isBlank()) items else items.filter { it.label.contains(search, ignoreCase = true) }
    }

    val grouped = filtered.groupBy { it.category.uppercase() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Add Action",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NothingDotGrid(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.04f,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NothingSpacing.md),
            ) {
            item {
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
                NothingInput(
                    value = search,
                    onValueChange = { search = it },
                    label = "Search",
                    placeholder = "Find an action",
                )
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
            }

            grouped.forEach { (category, actions) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                }

                items(actions, key = { it.label }) { item ->
                    ActionListItem(
                        label = item.label,
                        icon = item.icon,
                        iconBg = item.iconBg,
                        onClick = {
                            val json = Json.encodeToString(item.action)
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("action_result", json)
                            navController.popBackStack()
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                }
            }
        }
    }
}
}

@Composable
private fun ActionListItem(
    label: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = NothingSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

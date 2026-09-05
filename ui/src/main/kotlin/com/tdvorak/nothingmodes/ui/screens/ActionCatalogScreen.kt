package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                action = Action.SetWifi(true),
            ),
            ActionItem(
                label = "Bluetooth",
                category = "Connections",
                icon = Icons.Default.Bluetooth,
                action = Action.SetBluetooth(true),
            ),
            ActionItem(
                label = "Mobile data",
                category = "Connections",
                icon = Icons.Default.SignalCellular4Bar,
                action = Action.SetMobileData(true),
            ),
            ActionItem(
                label = "Airplane mode",
                category = "Connections",
                icon = Icons.Default.Flight,
                action = Action.SetAirplaneMode(true),
            ),
            ActionItem(
                label = "Dark mode",
                category = "Display",
                icon = Icons.Default.DarkMode,
                action = Action.SetDarkMode(NightMode.OFF),
            ),
            ActionItem(
                label = "Brightness",
                category = "Display",
                icon = Icons.Default.Brightness6,
                action = Action.SetBrightness(128, restore = true),
            ),
            ActionItem(
                label = "Auto brightness",
                category = "Display",
                icon = Icons.Default.Lightbulb,
                action = Action.SetAutoBrightness(true),
            ),
            ActionItem(
                label = "Extra dim",
                category = "Display",
                icon = Icons.Default.Brightness6,
                action = Action.SetExtraDim(true, restore = true),
            ),
            ActionItem(
                label = "Screen timeout",
                category = "Display",
                icon = Icons.Default.Timer,
                action = Action.SetScreenTimeout(30_000),
            ),
            ActionItem(
                label = "Always-on display",
                category = "Display",
                icon = Icons.Default.PhoneAndroid,
                action = Action.SetAlwaysOnDisplay(true),
            ),
            ActionItem(
                label = "Do not disturb",
                category = "Sound",
                icon = Icons.Default.Notifications,
                action = Action.SetDnd(DndMode.OFF),
            ),
            ActionItem(
                label = "Volume",
                category = "Sound",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                action = Action.SetVolume(VolumeStream.MEDIA, 8),
            ),
            ActionItem(
                label = "Vibrate",
                category = "Sound",
                icon = Icons.Default.Vibration,
                action = Action.Vibrate(500),
            ),
            ActionItem(
                label = "Ringer mode",
                category = "Sound",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                action = Action.SetRinger("normal"),
            ),
            ActionItem(
                label = "Flashlight",
                category = "Sound",
                icon = Icons.Default.FlashlightOn,
                action = Action.SetFlashlight(true),
            ),
            ActionItem(
                label = "Auto-rotate",
                category = "System",
                icon = Icons.Default.ScreenRotation,
                action = Action.SetAutoRotate(true),
            ),
            ActionItem(
                label = "Battery saver",
                category = "System",
                icon = Icons.Default.PowerSettingsNew,
                action = Action.SetBatterySaver(true),
            ),
            ActionItem(
                label = "Data saver",
                category = "System",
                icon = Icons.Default.SignalCellular4Bar,
                action = Action.SetDataSaver(true),
            ),
            ActionItem(
                label = "Mobile hotspot",
                category = "System",
                icon = Icons.Default.Wifi,
                action = Action.SetHotspot(true),
            ),
            ActionItem(
                label = "NFC",
                category = "System",
                icon = Icons.Default.Bluetooth,
                action = Action.SetNfc(true),
            ),
            ActionItem(
                label = "Location mode",
                category = "System",
                icon = Icons.Default.LocationOn,
                action = Action.SetLocationMode(LocationMode.HIGH_ACCURACY),
            ),
            ActionItem(
                label = "Screen rotation",
                category = "System",
                icon = Icons.Default.ScreenRotation,
                action = Action.SetScreenRotation(ScreenOrientation.AUTO),
            ),
            ActionItem(
                label = "Refresh rate",
                category = "System",
                icon = Icons.Default.Settings,
                action = Action.SetRefreshRate(60),
            ),
            ActionItem(
                label = "Auto-sync",
                category = "System",
                icon = Icons.Default.Snooze,
                action = Action.SetAutoSync(true),
            ),
            ActionItem(
                label = "Lock screen",
                category = "System",
                icon = Icons.Default.Lock,
                action = Action.LockScreen,
            ),
            ActionItem(
                label = "Screenshot",
                category = "System",
                icon = Icons.AutoMirrored.Filled.MobileScreenShare,
                action = Action.TakeScreenshot,
            ),
            ActionItem(
                label = "Clear notifications",
                category = "System",
                icon = Icons.Default.Notifications,
                action = Action.ClearNotifications,
            ),
            ActionItem(
                label = "Show notification",
                category = "Apps",
                icon = Icons.Default.Campaign,
                action = Action.ShowNotification("", ""),
            ),
            ActionItem(
                label = "Copy text",
                category = "Apps",
                icon = Icons.Default.ContentCopy,
                action = Action.CopyText(""),
            ),
            ActionItem(
                label = "Open URL",
                category = "Apps",
                icon = Icons.Default.Link,
                action = Action.OpenUrl(""),
            ),
            ActionItem(
                label = "Launch app",
                category = "Apps",
                icon = Icons.Default.OpenInBrowser,
                action = Action.LaunchApp(""),
            ),
            ActionItem(
                label = "Open settings",
                category = "Apps",
                icon = Icons.Default.Settings,
                action = Action.OpenSettingsScreen(SettingsScreen.SETTINGS, null),
            ),
            ActionItem(
                label = "Media control",
                category = "Apps",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                action = Action.MediaControl(MediaCommand.PLAY_PAUSE),
            ),
            ActionItem(
                label = "Wait",
                category = "Apps",
                icon = Icons.Default.Snooze,
                action = Action.Wait(1000),
            ),
            ActionItem(
                label = "Send SMS",
                category = "Apps",
                icon = Icons.Default.Sms,
                action = Action.SendSms("", ""),
            ),
            ActionItem(
                label = "Glyph",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                action = Action.SetGlyph(true),
            ),
            ActionItem(
                label = "Glyph matrix",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                action = Action.SetGlyphMatrix(null, restore = false),
            ),
            ActionItem(
                label = "Glyph preset",
                category = "Glyph",
                icon = Icons.Default.Lightbulb,
                action = Action.GlyphPreset("sleep"),
            ),
            ActionItem(
                label = "Glyph text",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                action = Action.GlyphText(""),
            ),
            ActionItem(
                label = "Glyph scrolling",
                category = "Glyph",
                icon = Icons.Default.PhoneAndroid,
                action = Action.GlyphScrollingText(""),
            ),
            ActionItem(
                label = "Glyph progress",
                category = "Glyph",
                icon = Icons.Default.Timer,
                action = Action.GlyphProgress(50),
            ),
            ActionItem(
                label = "Glyph animate",
                category = "Glyph",
                icon = Icons.Default.Lightbulb,
                action = Action.GlyphAnimate(),
            ),
            ActionItem(
                label = "Glyph turn off",
                category = "Glyph",
                icon = Icons.Default.PowerSettingsNew,
                action = Action.GlyphTurnOff,
            ),
            ActionItem(
                label = "Write setting",
                category = "Advanced",
                icon = Icons.Default.Settings,
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
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
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

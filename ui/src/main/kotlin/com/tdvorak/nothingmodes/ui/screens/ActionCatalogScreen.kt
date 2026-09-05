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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
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
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
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
                icon = Icons.Outlined.Wifi,
                action = Action.SetWifi(true),
            ),
            ActionItem(
                label = "Bluetooth",
                category = "Connections",
                icon = Icons.Outlined.Bluetooth,
                action = Action.SetBluetooth(true),
            ),
            ActionItem(
                label = "Mobile data",
                category = "Connections",
                icon = Icons.Outlined.SignalCellular4Bar,
                action = Action.SetMobileData(true),
            ),
            ActionItem(
                label = "Airplane mode",
                category = "Connections",
                icon = Icons.Outlined.Flight,
                action = Action.SetAirplaneMode(true),
            ),
            ActionItem(
                label = "Dark mode",
                category = "Display",
                icon = Icons.Outlined.DarkMode,
                action = Action.SetDarkMode(NightMode.OFF),
            ),
            ActionItem(
                label = "Brightness",
                category = "Display",
                icon = Icons.Outlined.Brightness6,
                action = Action.SetBrightness(128, restore = true),
            ),
            ActionItem(
                label = "Auto brightness",
                category = "Display",
                icon = Icons.Outlined.Lightbulb,
                action = Action.SetAutoBrightness(true),
            ),
            ActionItem(
                label = "Extra dim",
                category = "Display",
                icon = Icons.Outlined.Brightness6,
                action = Action.SetExtraDim(true, restore = true),
            ),
            ActionItem(
                label = "Screen timeout",
                category = "Display",
                icon = Icons.Outlined.Timer,
                action = Action.SetScreenTimeout(30_000),
            ),
            ActionItem(
                label = "Always-on display",
                category = "Display",
                icon = Icons.Outlined.PhoneAndroid,
                action = Action.SetAlwaysOnDisplay(true),
            ),
            ActionItem(
                label = "Do not disturb",
                category = "Sound",
                icon = Icons.Outlined.Notifications,
                action = Action.SetDnd(DndMode.OFF),
            ),
            ActionItem(
                label = "Volume",
                category = "Sound",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                action = Action.SetVolume(VolumeStream.MEDIA, 8),
            ),
            ActionItem(
                label = "Vibrate",
                category = "Sound",
                icon = Icons.Outlined.Vibration,
                action = Action.Vibrate(500),
            ),
            ActionItem(
                label = "Ringer mode",
                category = "Sound",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                action = Action.SetRinger("normal"),
            ),
            ActionItem(
                label = "Flashlight",
                category = "Sound",
                icon = Icons.Outlined.FlashlightOn,
                action = Action.SetFlashlight(true),
            ),
            ActionItem(
                label = "Auto-rotate",
                category = "System",
                icon = Icons.Outlined.ScreenRotation,
                action = Action.SetAutoRotate(true),
            ),
            ActionItem(
                label = "Battery saver",
                category = "System",
                icon = Icons.Outlined.PowerSettingsNew,
                action = Action.SetBatterySaver(true),
            ),
            ActionItem(
                label = "Data saver",
                category = "System",
                icon = Icons.Outlined.SignalCellular4Bar,
                action = Action.SetDataSaver(true),
            ),
            ActionItem(
                label = "Mobile hotspot",
                category = "System",
                icon = Icons.Outlined.Wifi,
                action = Action.SetHotspot(true),
            ),
            ActionItem(
                label = "NFC",
                category = "System",
                icon = Icons.Outlined.Bluetooth,
                action = Action.SetNfc(true),
            ),
            ActionItem(
                label = "Location mode",
                category = "System",
                icon = Icons.Outlined.LocationOn,
                action = Action.SetLocationMode(LocationMode.HIGH_ACCURACY),
            ),
            ActionItem(
                label = "Screen rotation",
                category = "System",
                icon = Icons.Outlined.ScreenRotation,
                action = Action.SetScreenRotation(ScreenOrientation.AUTO),
            ),
            ActionItem(
                label = "Refresh rate",
                category = "System",
                icon = Icons.Outlined.Settings,
                action = Action.SetRefreshRate(60),
            ),
            ActionItem(
                label = "Auto-sync",
                category = "System",
                icon = Icons.Outlined.Snooze,
                action = Action.SetAutoSync(true),
            ),
            ActionItem(
                label = "Lock screen",
                category = "System",
                icon = Icons.Outlined.Lock,
                action = Action.LockScreen,
            ),
            ActionItem(
                label = "Screenshot",
                category = "System",
                icon = Icons.AutoMirrored.Outlined.MobileScreenShare,
                action = Action.TakeScreenshot,
            ),
            ActionItem(
                label = "Clear notifications",
                category = "System",
                icon = Icons.Outlined.Notifications,
                action = Action.ClearNotifications,
            ),
            ActionItem(
                label = "Show notification",
                category = "Apps",
                icon = Icons.Outlined.Campaign,
                action = Action.ShowNotification("", ""),
            ),
            ActionItem(
                label = "Copy text",
                category = "Apps",
                icon = Icons.Outlined.ContentCopy,
                action = Action.CopyText(""),
            ),
            ActionItem(
                label = "Open URL",
                category = "Apps",
                icon = Icons.Outlined.Link,
                action = Action.OpenUrl(""),
            ),
            ActionItem(
                label = "Launch app",
                category = "Apps",
                icon = Icons.Outlined.OpenInBrowser,
                action = Action.LaunchApp(""),
            ),
            ActionItem(
                label = "Open settings",
                category = "Apps",
                icon = Icons.Outlined.Settings,
                action = Action.OpenSettingsScreen(SettingsScreen.SETTINGS, null),
            ),
            ActionItem(
                label = "Media control",
                category = "Apps",
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                action = Action.MediaControl(MediaCommand.PLAY_PAUSE),
            ),
            ActionItem(
                label = "Wait",
                category = "Apps",
                icon = Icons.Outlined.Snooze,
                action = Action.Wait(1000),
            ),
            ActionItem(
                label = "Send SMS",
                category = "Apps",
                icon = Icons.Outlined.Sms,
                action = Action.SendSms("", ""),
            ),
            ActionItem(
                label = "Glyph",
                category = "Glyph",
                icon = Icons.Outlined.PhoneAndroid,
                action = Action.SetGlyph(true),
            ),
            ActionItem(
                label = "Glyph matrix",
                category = "Glyph",
                icon = Icons.Outlined.PhoneAndroid,
                action = Action.SetGlyphMatrix(null, restore = false),
            ),
            ActionItem(
                label = "Glyph preset",
                category = "Glyph",
                icon = Icons.Outlined.Lightbulb,
                action = Action.GlyphPreset("sleep"),
            ),
            ActionItem(
                label = "Glyph text",
                category = "Glyph",
                icon = Icons.Outlined.PhoneAndroid,
                action = Action.GlyphText(""),
            ),
            ActionItem(
                label = "Glyph scrolling",
                category = "Glyph",
                icon = Icons.Outlined.PhoneAndroid,
                action = Action.GlyphScrollingText(""),
            ),
            ActionItem(
                label = "Glyph progress",
                category = "Glyph",
                icon = Icons.Outlined.Timer,
                action = Action.GlyphProgress(50),
            ),
            ActionItem(
                label = "Glyph animate",
                category = "Glyph",
                icon = Icons.Outlined.Lightbulb,
                action = Action.GlyphAnimate(),
            ),
            ActionItem(
                label = "Glyph turn off",
                category = "Glyph",
                icon = Icons.Outlined.PowerSettingsNew,
                action = Action.GlyphTurnOff,
            ),
            ActionItem(
                label = "Write setting",
                category = "Advanced",
                icon = Icons.Outlined.Settings,
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
        NothingIconCircle(size = 44f) {
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
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

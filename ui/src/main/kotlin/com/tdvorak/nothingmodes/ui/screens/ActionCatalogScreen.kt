package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MobileScreenShare
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private data class ActionItem(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val action: Action,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionCatalogScreen(
    navController: NavController,
) {
    var search by remember { mutableStateOf("") }

    // Forward action result from config back to the builder, then pop.
    val resultFlow = remember(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.getStateFlow("action_result", "")
            ?: MutableStateFlow("")
    }
    val result by resultFlow.collectAsState()
    LaunchedEffect(result) {
        if (result.isNotEmpty()) {
            navController.previousBackStackEntry?.savedStateHandle?.set("action_result", result)
            navController.currentBackStackEntry?.savedStateHandle?.set("action_result", "")
            navController.popBackStack()
        }
    }

    val items = remember {
        listOf(
            ActionItem("Wi-Fi On", "Connectivity", Icons.Default.Wifi, Action.SetWifi(true)),
            ActionItem("Wi-Fi Off", "Connectivity", Icons.Default.Wifi, Action.SetWifi(false)),
            ActionItem("Bluetooth On", "Connectivity", Icons.Default.Bluetooth, Action.SetBluetooth(true)),
            ActionItem("Bluetooth Off", "Connectivity", Icons.Default.Bluetooth, Action.SetBluetooth(false)),
            ActionItem("Mobile Data On", "Connectivity", Icons.Default.SignalCellular4Bar, Action.SetMobileData(true)),
            ActionItem("Mobile Data Off", "Connectivity", Icons.Default.SignalCellular4Bar, Action.SetMobileData(false)),
            ActionItem("Airplane On", "Connectivity", Icons.Default.Flight, Action.SetAirplaneMode(true)),
            ActionItem("Airplane Off", "Connectivity", Icons.Default.Flight, Action.SetAirplaneMode(false)),

            ActionItem("Dark Mode On", "Display", Icons.Default.DarkMode, Action.SetDarkMode(NightMode.ON)),
            ActionItem("Dark Mode Off", "Display", Icons.Default.DarkMode, Action.SetDarkMode(NightMode.OFF)),
            ActionItem("Brightness 50%", "Display", Icons.Default.Brightness6, Action.SetBrightness(128, restore = true)),
            ActionItem("Auto Brightness On", "Display", Icons.Default.Brightness6, Action.SetAutoBrightness(true)),
            ActionItem("Extra Dim On", "Display", Icons.Default.Lightbulb, Action.SetExtraDim(true, restore = true)),
            ActionItem("Screen Timeout 30s", "Display", Icons.Default.Timer, Action.SetScreenTimeout(30_000)),
            ActionItem("AOD On", "Display", Icons.Default.PhoneAndroid, Action.SetAlwaysOnDisplay(true)),

            ActionItem("DND Priority", "Sound", Icons.Default.Notifications, Action.SetDnd(DndMode.PRIORITY)),
            ActionItem("DND Off", "Sound", Icons.Default.Notifications, Action.SetDnd(DndMode.OFF)),
            ActionItem("Volume Media 50%", "Sound", Icons.Default.VolumeUp, Action.SetVolume(VolumeStream.MEDIA, 8)),
            ActionItem("Vibrate 500ms", "Sound", Icons.Default.Vibration, Action.Vibrate(500)),

            ActionItem("Auto-rotate On", "System", Icons.Default.ScreenRotation, Action.SetAutoRotate(true)),
            ActionItem("Battery Saver On", "System", Icons.Default.PowerSettingsNew, Action.SetBatterySaver(true)),
            ActionItem("Location High", "System", Icons.Default.LocationOn, Action.SetLocationMode(LocationMode.HIGH_ACCURACY)),
            ActionItem("Lock Screen", "System", Icons.Default.Lock, Action.LockScreen),
            ActionItem("Screenshot", "System", Icons.Default.MobileScreenShare, Action.TakeScreenshot),

            ActionItem("Show Notification", "Apps", Icons.Default.Campaign, Action.ShowNotification("Title", "Text")),
            ActionItem("Copy Text", "Apps", Icons.Default.ContentCopy, Action.CopyText("")),
            ActionItem("Open URL", "Apps", Icons.Default.OpenInBrowser, Action.OpenUrl("https://example.com")),
            ActionItem("Open Settings", "Apps", Icons.Default.Settings, Action.OpenSettingsScreen(SettingsScreen.SETTINGS)),

            ActionItem("Glyph On", "Glyph", Icons.Default.Lightbulb, Action.SetGlyph(true)),
            ActionItem("Glyph Off", "Glyph", Icons.Default.Lightbulb, Action.SetGlyph(false)),
            ActionItem("Glyph Preset: Sleep", "Glyph", Icons.Default.Snooze, Action.GlyphPreset("sleep")),
            ActionItem("Glyph Text", "Glyph", Icons.Default.Campaign, Action.GlyphText("Hello")),
        )
    }

    val filtered = remember(search, items) {
        if (search.isBlank()) items else items.filter { it.label.contains(search, ignoreCase = true) }
    }

    val grouped = filtered.groupBy { it.category }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Add Action",
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
            NothingInput(
                value = search,
                onValueChange = { search = it },
                label = "Search",
                placeholder = "Find an action",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            grouped.forEach { (category, actions) ->
                NothingSectionHeader(text = category.uppercase())
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                ) {
                    actions.forEach { item ->
                        ActionTile(
                            label = item.label,
                            icon = item.icon,
                            onClick = {
                                val json = Json.encodeToString(item.action)
                                navController.navigate("action_config?action=$json")
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(NothingSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .padding(NothingSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceMono,
        )
    }
}

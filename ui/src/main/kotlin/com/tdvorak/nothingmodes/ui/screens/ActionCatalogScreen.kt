package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AodMode
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import com.tdvorak.nothingmodes.ui.theme.NothingBottomActionBar
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private data class ActionItem(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val action: Action,
)

/** Combined add-action screen. Every action type is available in one place,
 *  organized by category and searchable. Tapping a row opens its config sheet;
 *  the bottom bar adds the whole configured selection at once. */
@Composable
fun ActionCatalogScreen(navController: NavController) {
    var search by remember { mutableStateOf("") }
    // Actions the user has configured and wants to add.
    var selected by remember { mutableStateOf<List<Action>>(emptyList()) }
    // Index of the selected action currently being edited, or null for a new action.
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    // The action currently shown in the configuration sheet.
    var configAction by remember { mutableStateOf<Action?>(null) }

    val items =
        remember {
            listOf(
                ActionItem("Wi-Fi", "Connections", Icons.Outlined.Wifi, Action.SetWifi(true)),
                ActionItem("Bluetooth", "Connections", Icons.Outlined.Bluetooth, Action.SetBluetooth(true)),
                ActionItem("Mobile data", "Connections", Icons.Outlined.SignalCellular4Bar, Action.SetMobileData(true)),
                ActionItem("Mobile hotspot", "Connections", Icons.Outlined.Wifi, Action.SetHotspot(true)),
                ActionItem("Airplane mode", "Connections", Icons.Outlined.Flight, Action.SetAirplaneMode(true)),
                ActionItem("Dark mode", "Display", Icons.Outlined.DarkMode, Action.SetDarkMode(NightMode.OFF)),
                ActionItem("Brightness", "Display", Icons.Outlined.Brightness6, Action.SetBrightness(128, restore = true)),
                ActionItem("Auto brightness", "Display", Icons.Outlined.Lightbulb, Action.SetAutoBrightness(true)),
                ActionItem("Extra dim", "Display", Icons.Outlined.Brightness6, Action.SetExtraDim(true, restore = true)),
                ActionItem("Screen timeout", "Display", Icons.Outlined.Timer, Action.SetScreenTimeout(30_000)),
                ActionItem("Always-on display", "Display", Icons.Outlined.PhoneAndroid, Action.SetAlwaysOnDisplay(AodMode.OFF)),
                ActionItem("Do not disturb", "Sound", Icons.Outlined.Notifications, Action.SetDnd(DndMode.OFF)),
                ActionItem("Volume", "Sound", Icons.AutoMirrored.Outlined.VolumeUp, Action.SetVolume(mapOf(VolumeStream.MEDIA to 8))),
                ActionItem("Vibrate", "Sound", Icons.Outlined.Vibration, Action.Vibrate(500)),
                ActionItem("Ringer mode", "Sound", Icons.AutoMirrored.Outlined.VolumeUp, Action.SetRinger("normal")),
                ActionItem("Flashlight", "Sound", Icons.Outlined.FlashlightOn, Action.SetFlashlight(true)),
                ActionItem("Auto-rotate", "System", Icons.Outlined.ScreenRotation, Action.SetAutoRotate(true)),
                ActionItem("Screen rotation", "System", Icons.Outlined.ScreenRotation, Action.SetScreenRotation(ScreenOrientation.AUTO)),
                ActionItem("Battery saver", "System", Icons.Outlined.PowerSettingsNew, Action.SetBatterySaver(true)),
                ActionItem("Data saver", "System", Icons.Outlined.SignalCellular4Bar, Action.SetDataSaver(true)),
                ActionItem("NFC", "System", Icons.Outlined.Nfc, Action.SetNfc(true)),
                ActionItem("Location mode", "System", Icons.Outlined.LocationOn, Action.SetLocationMode(LocationMode.HIGH_ACCURACY)),
                ActionItem("Refresh rate", "System", Icons.Outlined.Settings, Action.SetRefreshRate(60)),
                ActionItem("Auto-sync", "System", Icons.Outlined.Snooze, Action.SetAutoSync(true)),
                ActionItem("Lock screen", "System", Icons.Outlined.Lock, Action.LockScreen),
                ActionItem("Screenshot", "System", Icons.AutoMirrored.Outlined.MobileScreenShare, Action.TakeScreenshot),
                ActionItem("Clear notifications", "System", Icons.Outlined.Notifications, Action.ClearNotifications),
                ActionItem("Show notification", "Apps", Icons.Outlined.Campaign, Action.ShowNotification("", "")),
                ActionItem("Open URL", "Apps", Icons.Outlined.Link, Action.OpenUrl("")),
                ActionItem("Launch app", "Apps", Icons.Outlined.OpenInBrowser, Action.LaunchApp("")),
                ActionItem("Open settings", "Apps", Icons.Outlined.Settings, Action.OpenSettingsScreen(SettingsScreen.SETTINGS, null)),
                ActionItem("Media control", "Apps", Icons.AutoMirrored.Outlined.VolumeUp, Action.MediaControl(MediaCommand.PLAY_PAUSE)),
                ActionItem("Wait", "Apps", Icons.Outlined.Snooze, Action.Wait(1000)),
                ActionItem("Send SMS", "Apps", Icons.Outlined.Sms, Action.SendSms("", "")),
                ActionItem("Glyph", "Glyph", Icons.Outlined.PhoneAndroid, Action.SetGlyph(true)),
                ActionItem("Glyph matrix", "Glyph", Icons.Outlined.PhoneAndroid, Action.SetGlyphMatrix(null, restore = false)),
                ActionItem("Glyph preset", "Glyph", Icons.Outlined.Lightbulb, Action.GlyphPreset("sleep")),
                ActionItem("Glyph text", "Glyph", Icons.Outlined.PhoneAndroid, Action.GlyphText("")),
                ActionItem("Glyph scrolling", "Glyph", Icons.Outlined.PhoneAndroid, Action.GlyphScrollingText("")),
                ActionItem("Glyph icon", "Glyph", Icons.Outlined.Star, Action.GlyphIcon("check")),
                ActionItem("Glyph number", "Glyph", Icons.Outlined.Tag, Action.GlyphNumber(0)),
                ActionItem("Glyph countdown", "Glyph", Icons.Outlined.Timer, Action.GlyphCountdown(30)),
                ActionItem("Glyph progress", "Glyph", Icons.Outlined.Timer, Action.GlyphProgress(50)),
                ActionItem("Glyph animate", "Glyph", Icons.Outlined.Lightbulb, Action.GlyphAnimate()),
                ActionItem("Glyph music", "Glyph", Icons.Outlined.MusicNote, Action.GlyphMusic()),
                ActionItem("Glyph turn off", "Glyph", Icons.Outlined.PowerSettingsNew, Action.GlyphTurnOff),
                ActionItem("Write setting", "Advanced", Icons.Outlined.Settings, Action.WriteSetting(SettingNamespace.GLOBAL, "animator_duration_scale", "1.0")),
            ).filter { it.action !is Action.LockScreen || FeatureFlags.enableLockScreen }
        }

    val iconForAction =
        remember(items) {
            { action: Action ->
                items.find { it.action::class == action::class }?.icon ?: Icons.Outlined.Settings
            }
        }

    val filtered =
        remember(search, items) {
            if (search.isBlank()) {
                items
            } else {
                items.filter { it.label.contains(search, ignoreCase = true) }
            }
        }

    val grouped = filtered.groupBy { it.category.uppercase() }
    val orderedCategories =
        remember(filtered) {
            listOf("CONNECTIONS", "DISPLAY", "SOUND", "SYSTEM", "APPS", "GLYPH", "ADVANCED")
                .filter { it in grouped.keys }
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Add action",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = NothingSpacing.md, end = NothingSpacing.md, bottom = 160.dp),
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

                if (selected.isNotEmpty()) {
                    item {
                        NothingSectionHeader(text = "Selected")
                        NothingCard {
                            selected.forEachIndexed { index, action ->
                                if (index > 0) NothingDivider()
                                NothingListRow(
                                    title = items.find { it.action::class == action::class }?.label ?: "Action",
                                    subtitle = actionDescription(action),
                                    onClick = {
                                        editingIndex = index
                                        configAction = action
                                    },
                                    leading = {
                                        val icon = iconForAction(action)
                                        NothingIconCircle(size = 44f) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    },
                                    trailing = {
                                        Text(
                                            text = "[X]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NothingColors.accent,
                                            fontFamily = NothingFonts.mono(),
                                            modifier = Modifier.clickableNoRipple { selected = selected.filterIndexed { i, _ -> i != index } },
                                        )
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    }
                }

                orderedCategories.forEach { category ->
                    val categoryItems = grouped[category] ?: emptyList()
                    item {
                        NothingSectionHeader(text = category)
                        NothingCard {
                            categoryItems.forEachIndexed { index, actionItem ->
                                if (index > 0) NothingDivider()
                                CatalogListItem(
                                    label = actionItem.label,
                                    icon = actionItem.icon,
                                    subtitle = actionRequirementHint(actionItem.action) ?: actionDescription(actionItem.action),
                                    onClick = {
                                        editingIndex = null
                                        configAction = actionItem.action
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    }
                }
            }

            // Sticky bottom bar — drawn on top of the list so content never shows through.
            NothingBottomActionBar(
                text =
                    if (selected.isEmpty()) {
                        "Select at least one action"
                    } else {
                        "Add ${selected.size} action${if (selected.size > 1) "s" else ""}"
                    },
                onClick = {
                    if (selected.isNotEmpty()) {
                        val json = Json.encodeToString(selected)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("action_results", json)
                    }
                    navController.popBackStack()
                },
                enabled = selected.isNotEmpty(),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(1f),
            )
        }
    }

    configAction?.let { action ->
        ActionConfigSheet(
            action = action,
            onDone = { updated ->
                if (editingIndex != null) {
                    selected = selected.toMutableList().also { it[editingIndex!!] = updated }
                } else {
                    selected = selected + updated
                }
                configAction = null
                editingIndex = null
            },
            onDismiss = {
                configAction = null
                editingIndex = null
            },
        )
    }
}

@Composable
private fun CatalogListItem(
    label: String,
    icon: ImageVector,
    subtitle: String,
    onClick: () -> Unit,
) {
    NothingListRow(
        title = label,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            NothingIconCircle(size = 44f) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    )
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        ),
    )

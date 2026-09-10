package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.capabilities.CapabilitiesCache
import com.tdvorak.nothingmodes.capabilities.CapabilityResolver
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AodMode
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import com.tdvorak.nothingmodes.ui.components.CatalogEntry
import com.tdvorak.nothingmodes.ui.components.CatalogFilter
import com.tdvorak.nothingmodes.ui.components.CatalogPickerContent
import com.tdvorak.nothingmodes.ui.theme.NothingBottomActionBar
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.util.capabilityGaps
import com.tdvorak.nothingmodes.ui.util.isHardwareBlocked
import com.tdvorak.nothingmodes.ui.util.missingCapabilityHint
import com.tdvorak.nothingmodes.ui.util.requirementBadges
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tdvorak.nothingmodes.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private enum class RowKind { CONFIG, GLYPH_PICKER, DIRECT }

private data class ActionItem(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val action: Action?,
    val kind: RowKind = RowKind.CONFIG,
    val desc: String? = null,
)

/** Combined add-action screen. Every action type is available in one place,
 *  organized by category and searchable. Tapping a row opens its config sheet;
 *  the bottom bar adds the whole configured selection at once. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActionCatalogScreen(navController: NavController) {
    val context = LocalContext.current
    var caps by remember { mutableStateOf(CapabilitiesCache.peek() ?: DeviceCapabilities()) }
    LaunchedEffect(Unit) { caps = CapabilitiesCache.refresh(context) }
    // Actions the user has configured and wants to add.
    var selected by remember { mutableStateOf<List<Action>>(emptyList()) }
    // Index of the selected action currently being edited, or null for a new action.
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    // The action currently shown in the configuration sheet.
    var configAction by remember { mutableStateOf<Action?>(null) }
    // Action waiting for a capability-warning confirmation.
    var pendingAction by remember { mutableStateOf<Action?>(null) }

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
                ActionItem("Stay awake", "Display", Icons.Outlined.Bedtime, Action.SetStayAwake(true)),
                ActionItem("Wallpaper", "Display", Icons.Outlined.Wallpaper, Action.SetWallpaper("")),
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
                ActionItem("Lock screen", "System", Icons.Outlined.Lock, Action.LockScreen()),
                ActionItem("Screenshot", "System", Icons.AutoMirrored.Outlined.MobileScreenShare, Action.TakeScreenshot()),
                ActionItem("Clear notifications", "System", Icons.Outlined.Notifications, Action.ClearNotifications),
                ActionItem("Show notification", "Apps", Icons.Outlined.Campaign, Action.ShowNotification("", "")),
                ActionItem("Open URL", "Apps", Icons.Outlined.Link, Action.OpenUrl("")),
                ActionItem("Launch app", "Apps", Icons.Outlined.OpenInBrowser, Action.LaunchApp(emptyList())),
                ActionItem("Open settings", "Apps", Icons.Outlined.Settings, Action.OpenSettingsScreen(SettingsScreen.SETTINGS, null)),
                ActionItem("Media control", "Apps", Icons.AutoMirrored.Outlined.VolumeUp, Action.MediaControl(MediaCommand.PLAY_PAUSE)),
                ActionItem("Wait", "Apps", Icons.Outlined.Snooze, Action.Wait(1000)),
                ActionItem("Send SMS", "Apps", Icons.Outlined.Sms, Action.SendSms("", "")),
                // One entry into every Glyph design type — the picker below.
                ActionItem(
                    "Glyph",
                    "Glyph",
                    Icons.Outlined.WbTwilight,
                    action = Action.GlyphTurnOff, // meta carrier — picks open the design picker
                    kind = RowKind.GLYPH_PICKER,
                    desc = "Show a design, animation, or the music visualizer on the Glyph.",
                ),
                ActionItem(
                    "Glyph flashlight",
                    "Glyph",
                    Icons.Outlined.FlashlightOn,
                    action =
                        Action.Group(
                            name = "Glyph flashlight",
                            actions =
                                listOf(
                                    Action.SetFlashlight(true),
                                    Action.SetGlyphMatrix(colors = List(625) { 255 }),
                                ),
                        ),
                    kind = RowKind.DIRECT,
                    desc = "Torch plus Glyph at full brightness — an always-available torch.",
                ),
                ActionItem(
                    "Glyph off",
                    "Glyph",
                    Icons.Outlined.PowerSettingsNew,
                    action = Action.GlyphTurnOff,
                    kind = RowKind.DIRECT,
                    desc = "Clears anything currently on the Glyph.",
                ),
                ActionItem("Write setting", "Advanced", Icons.Outlined.Settings, Action.WriteSetting(SettingNamespace.GLOBAL, "animator_duration_scale", "1.0")),
            ).filter { it.action !is Action.LockScreen || FeatureFlags.enableLockScreen }
        }

    val iconForAction =
        remember(items) {
            { action: Action ->
                items.firstOrNull { it.action != null && it.action::class == action::class }?.icon
                    ?: if (action is Action.Group) Icons.Outlined.FlashlightOn else Icons.Outlined.Settings
            }
        }

    val resolver = remember(caps) { CapabilityResolver(caps) }

    val catalogEntries =
        remember(items, caps) {
            items.mapNotNull { item ->
                val metaAction = item.action ?: return@mapNotNull null
                val (subtitle, badges, blocked) = actionCatalogMeta(metaAction, caps)
                if (blocked) {
                    null
                } else {
                    CatalogEntry(
                        label = item.label,
                        category = item.category,
                        icon = item.icon,
                        description = item.desc ?: subtitle,
                        badges = badges,
                        accent = item.category == "Glyph",
                    )
                }
            }
        }

    val actionFilters =
        remember {
            listOf(
                CatalogFilter("No Shizuku") { "SHIZUKU" !in it.badges },
                CatalogFilter("Needs Shizuku") { "SHIZUKU" in it.badges },
                CatalogFilter("Needs setup") { it.badges.isNotEmpty() },
            )
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = stringResource(R.string.picker_add_action),
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
            CatalogPickerContent(
                entries = catalogEntries,
                onSelect = { entry ->
                    val actionItem = items.firstOrNull { it.label == entry.label } ?: return@CatalogPickerContent
                    editingIndex = null
                    when (actionItem.kind) {
                        RowKind.GLYPH_PICKER -> {
                            val action = Action.GlyphPreset("sleep")
                            val required = CapabilityRequirements.derive(Trigger.Immediate, listOf(action))
                            if (resolver.resolve(actionItem.label, required).canRun) {
                                configAction = action
                            } else {
                                pendingAction = action
                            }
                        }
                        RowKind.DIRECT -> actionItem.action?.let { selected = selected + it }
                        RowKind.CONFIG -> {
                            val action = actionItem.action ?: return@CatalogPickerContent
                            val required = CapabilityRequirements.derive(Trigger.Immediate, listOf(action))
                            if (resolver.resolve(actionItem.label, required).canRun) {
                                configAction = action
                            } else {
                                pendingAction = action
                            }
                        }
                    }
                },
                searchPlaceholder = stringResource(R.string.picker_find_action),
                categoryOrder = listOf("Connections", "Display", "Sound", "System", "Apps", "Glyph", "Advanced"),
                extraFilters = actionFilters,
                horizontalPadding = NothingSpacing.md,
                bottomPadding = 160.dp,
                selectedTray = {
                    if (selected.isNotEmpty()) {
                        NothingSectionHeader(text = stringResource(R.string.picker_selected))
                        NothingCard {
                            selected.forEachIndexed { index, action ->
                                if (index > 0) NothingDivider()
                                NothingListRow(
                                    title =
                                        items.firstOrNull { it.action != null && it.action::class == action::class }?.label
                                            ?: (action as? Action.Group)?.name
                                            ?: "Action",
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
                },
            )

            // Sticky bottom bar — drawn on top of the list so content never shows through.
            NothingBottomActionBar(
                text =
                    if (selected.isEmpty()) {
                        stringResource(R.string.picker_select_at_least_one_action)
                    } else {
                        pluralStringResource(R.plurals.picker_add_n_actions, selected.size, selected.size)
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

    pendingAction?.let { action ->
        val required = CapabilityRequirements.derive(Trigger.Immediate, listOf(action))
        val resolution = resolver.resolve(action::class.simpleName ?: "", required)
        CapabilityWarningDialog(
            gaps = capabilityGaps(context, resolution.missingReasons, caps),
            onDismiss = { pendingAction = null },
            onConfirm = {
                configAction = action
                pendingAction = null
            },
        )
    }

    configAction?.let { action ->
        ActionConfigSheet(
            action = action,
            caps = caps,
            onOpenGlyphStudio = { navController.navigate("glyph_editor") },
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

private fun actionCatalogMeta(
    action: Action,
    caps: DeviceCapabilities,
): Triple<String, List<String>, Boolean> {
    val required = CapabilityRequirements.derive(Trigger.Immediate, listOf(action))
    val resolution = CapabilityResolver(caps).resolve("", required)
    val badges = requirementBadges(resolution.missing)
    val hint = actionRequirementHint(action)
    val static =
        if (resolution.canRun && hint != null && (hint.startsWith("Needs") || hint.startsWith("Detected"))) {
            actionFeatureDescription(action) ?: actionDescription(action)
        } else {
            hint ?: actionDescription(action)
        }
    val subtitle =
        if (!resolution.canRun) {
            missingCapabilityHint(badges, resolution.missingReasons.values.firstOrNull() ?: static)
        } else {
            static
        }
    return Triple(subtitle, badges, isHardwareBlocked(resolution.missing, caps))
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        ),
    )

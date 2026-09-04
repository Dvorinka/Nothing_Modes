package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.ui.theme.NothingDestructiveButton
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono

private val iconOptions = listOf(
    "star" to Icons.Default.Star,
    "home" to Icons.Default.Home,
    "wifi" to Icons.Default.Wifi,
    "bluetooth" to Icons.Default.Bluetooth,
    "sun" to Icons.Default.WbSunny,
    "dark_mode" to Icons.Default.DarkMode,
    "brightness" to Icons.Default.Brightness6,
    "lightbulb" to Icons.Default.Lightbulb,
    "notification" to Icons.Default.Notifications,
    "volume" to Icons.Default.VolumeUp,
    "music" to Icons.Default.MusicNote,
    "alarm" to Icons.Default.Alarm,
    "timer" to Icons.Default.Timer,
    "location" to Icons.Default.LocationOn,
    "language" to Icons.Default.Language,
    "airplane" to Icons.Default.Flight,
    "power" to Icons.Default.PowerSettingsNew,
    "battery" to Icons.Default.BatteryFull,
    "flashlight" to Icons.Default.FlashlightOn,
    "campaign" to Icons.Default.Campaign,
    "message" to Icons.Default.Textsms,
    "settings" to Icons.Default.Settings,
    "speed" to Icons.Default.Speed,
)

private val colorOptions = listOf(
    "#4A9E5C",
    "#D4A843",
    "#5B9BF6",
    "#D71921",
    "#9B59B6",
    "#1ABC9C",
    "#E67E22",
    "#2C3E50",
    "#7F8C8D",
    "#C0392B",
    "#2980B9",
    "#27AE60",
)

fun iconForName(name: String): ImageVector =
    iconOptions.find { it.first == name }?.second ?: Icons.Default.Star

fun colorForHex(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF9B9B9B))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IconColorPickerSheet(
    initialIcon: String,
    initialColor: String,
    onDone: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIcon by remember { mutableStateOf(initialIcon.ifBlank { "star" }) }
    var selectedColor by remember { mutableStateOf(initialColor.ifBlank { colorOptions.first() }) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NothingSpacing.md)
                .padding(bottom = NothingSpacing.xl)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Choose icon & color",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceMono,
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(colorForHex(selectedColor), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForName(selectedIcon),
                    contentDescription = selectedIcon,
                    tint = if (colorForHex(selectedColor).luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Text(
                text = "ICON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                maxItemsInEachRow = 6,
            ) {
                iconOptions.forEach { (name, icon) ->
                    IconOption(
                        icon = icon,
                        selected = selectedIcon == name,
                        onClick = { selectedIcon = name },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Text(
                text = "COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                maxItemsInEachRow = 6,
            ) {
                colorOptions.forEach { hex ->
                    ColorOption(
                        color = colorForHex(hex),
                        selected = selectedColor == hex,
                        onClick = { selectedColor = hex },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                NothingPillButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                NothingPillButton(
                    text = "Save",
                    onClick = { onDone(selectedIcon, selectedColor) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun IconOption(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}

// ─── Save Sheet (icon/color + save actions) ──────────────────────────────────

/**
 * Save bottom sheet: icon/color selection plus save action options.
 * Actions: Save, Save as, Go back, Cancel, Cancel this card.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SaveSheet(
    initialIcon: String,
    initialColor: String,
    isEditing: Boolean,
    onSave: (String, String) -> Unit,
    onSaveAs: (String, String) -> Unit,
    onGoBack: () -> Unit,
    onCancel: () -> Unit,
) {
    var selectedIcon by remember { mutableStateOf(initialIcon.ifBlank { "star" }) }
    var selectedColor by remember { mutableStateOf(initialColor.ifBlank { colorOptions.first() }) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onGoBack,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NothingSpacing.md)
                .padding(bottom = NothingSpacing.xl)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SAVE",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceMono,
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(colorForHex(selectedColor), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForName(selectedIcon),
                    contentDescription = selectedIcon,
                    tint = if (colorForHex(selectedColor).luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Text(
                text = "ICON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                maxItemsInEachRow = 6,
            ) {
                iconOptions.forEach { (name, icon) ->
                    IconOption(
                        icon = icon,
                        selected = selectedIcon == name,
                        onClick = { selectedIcon = name },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            Text(
                text = "COLOR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                maxItemsInEachRow = 6,
            ) {
                colorOptions.forEach { hex ->
                    ColorOption(
                        color = colorForHex(hex),
                        selected = selectedColor == hex,
                        onClick = { selectedColor = hex },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xl))

            // Primary save action
            NothingPillButton(
                text = if (isEditing) "Save" else "Save",
                onClick = { onSave(selectedIcon, selectedColor) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))

            // Save as copy
            NothingPillButton(
                text = "Save As",
                onClick = { onSaveAs(selectedIcon, selectedColor) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                NothingPillButton(
                    text = "Go Back",
                    onClick = onGoBack,
                    modifier = Modifier.weight(1f),
                )
                NothingPillButton(
                    text = "Cancel Card",
                    onClick = onGoBack,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(NothingSpacing.sm))

            NothingDestructiveButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

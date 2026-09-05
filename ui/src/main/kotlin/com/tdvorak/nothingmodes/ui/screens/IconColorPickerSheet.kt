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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono

private data class IconEntry(
    val name: String,
    val icon: ImageVector,
    val keywords: List<String>,
)

private val iconOptions = listOf(
    IconEntry("star", Icons.Default.Star, listOf("star", "favorite", "rate")),
    IconEntry("home", Icons.Default.Home, listOf("home", "house", "start")),
    IconEntry("wifi", Icons.Default.Wifi, listOf("wifi", "wireless", "network", "internet")),
    IconEntry("bluetooth", Icons.Default.Bluetooth, listOf("bluetooth", "wireless", "bt")),
    IconEntry("sun", Icons.Default.WbSunny, listOf("sun", "day", "light", "bright", "morning")),
    IconEntry("dark_mode", Icons.Default.DarkMode, listOf("dark", "night", "moon", "sleep")),
    IconEntry("brightness", Icons.Default.Brightness6, listOf("brightness", "screen", "dim")),
    IconEntry("lightbulb", Icons.Default.Lightbulb, listOf("light", "bulb", "idea", "lamp")),
    IconEntry("notification", Icons.Default.Notifications, listOf("notification", "alert", "bell", "notify")),
    IconEntry("volume", Icons.AutoMirrored.Filled.VolumeUp, listOf("volume", "sound", "audio", "loud")),
    IconEntry("music", Icons.Default.MusicNote, listOf("music", "song", "audio", "play", "media")),
    IconEntry("alarm", Icons.Default.Alarm, listOf("alarm", "wake", "clock", "time")),
    IconEntry("timer", Icons.Default.Timer, listOf("timer", "countdown", "stopwatch", "time")),
    IconEntry("clock", Icons.Default.AccessTime, listOf("clock", "time", "watch", "hour")),
    IconEntry("location", Icons.Default.LocationOn, listOf("location", "gps", "map", "place", "pin")),
    IconEntry("language", Icons.Default.Language, listOf("language", "translate", "global", "world")),
    IconEntry("airplane", Icons.Default.Flight, listOf("airplane", "flight", "travel", "plane")),
    IconEntry("power", Icons.Default.PowerSettingsNew, listOf("power", "off", "shutdown", "reboot")),
    IconEntry("battery", Icons.Default.BatteryFull, listOf("battery", "charge", "power", "energy")),
    IconEntry("flashlight", Icons.Default.FlashlightOn, listOf("flashlight", "torch", "light", "led")),
    IconEntry("campaign", Icons.Default.Campaign, listOf("campaign", "announce", "broadcast", "megaphone")),
    IconEntry("message", Icons.Default.Textsms, listOf("message", "sms", "text", "chat")),
    IconEntry("email", Icons.Default.Email, listOf("email", "mail", "inbox", "letter")),
    IconEntry("phone", Icons.Default.Phone, listOf("phone", "call", "dial", "ring")),
    IconEntry("settings", Icons.Default.Settings, listOf("settings", "gear", "config", "preferences")),
    IconEntry("speed", Icons.Default.Speed, listOf("speed", "fast", "performance", "gauge")),
    IconEntry("lock", Icons.Default.Lock, listOf("lock", "secure", "password", "screen lock")),
    IconEntry("vibration", Icons.Default.Vibration, listOf("vibration", "vibrate", "haptic", "buzz")),
    IconEntry("mic", Icons.Default.Mic, listOf("mic", "microphone", "record", "voice")),
    IconEntry("cloud", Icons.Default.Cloud, listOf("cloud", "weather", "sync", "sky")),
    IconEntry("air", Icons.Default.Air, listOf("air", "wind", "breeze", "fan")),
    IconEntry("work", Icons.Default.Work, listOf("work", "office", "business", "briefcase", "job")),
    IconEntry("fitness", Icons.Default.FitnessCenter, listOf("fitness", "gym", "workout", "exercise", "health")),
    IconEntry("food", Icons.Default.Fastfood, listOf("food", "eat", "meal", "lunch", "dinner", "restaurant")),
    IconEntry("shopping", Icons.Default.ShoppingCart, listOf("shopping", "cart", "buy", "store", "shop")),
    IconEntry("game", Icons.Default.Gamepad, listOf("game", "play", "controller", "gaming")),
    IconEntry("person", Icons.Default.AccountCircle, listOf("person", "user", "account", "profile", "contact")),
    IconEntry("check", Icons.Default.CheckCircle, listOf("check", "done", "complete", "confirm", "ok")),
)

private val colorOptions = listOf(
    "#FF3030", // Nothing accent red
    "#FFFFFF", // White
    "#000000", // Black
    "#555555", // Muted gray
    "#999999", // Secondary gray
    "#1A1A1A", // Surface-2
)

fun iconForName(name: String): ImageVector =
    iconOptions.find { it.name == name }?.icon ?: Icons.Default.Star

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
    var searchQuery by remember { mutableStateOf("") }
    var customColorHex by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredIcons = remember(searchQuery) {
        if (searchQuery.isBlank()) iconOptions
        else iconOptions.filter { entry ->
            entry.name.contains(searchQuery, ignoreCase = true) ||
                entry.keywords.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = NothingShapes.sheet,
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
                fontFamily = GeistSans,
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            // Live preview — shows the selected Nothing palette color.
            val previewColor = colorForHex(selectedColor)
            val previewIconColor = if (previewColor.luminance() > 0.5f) Color.Black else Color.White
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(previewColor, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForName(selectedIcon),
                    contentDescription = selectedIcon,
                    tint = previewIconColor,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            // Icon search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search icons...", fontFamily = SpaceMono) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.sm))

            Text(
                text = "ICON (${filteredIcons.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                maxItemsInEachRow = 6,
            ) {
                filteredIcons.forEach { entry ->
                    IconOption(
                        icon = entry.icon,
                        selected = selectedIcon == entry.name,
                        onClick = { selectedIcon = entry.name },
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
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                maxItemsInEachRow = 8,
            ) {
                colorOptions.forEach { hex ->
                    ColorOption(
                        color = colorForHex(hex),
                        selected = selectedColor.equals(hex, ignoreCase = true),
                        onClick = { selectedColor = hex },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.sm))

            // Custom color input
            OutlinedTextField(
                value = customColorHex,
                onValueChange = { customColorHex = it },
                placeholder = { Text("#RRGGBB", fontFamily = SpaceMono) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingPillButton(
                text = "Apply custom color",
                onClick = {
                    val trimmed = customColorHex.trim()
                    if (trimmed.matches(Regex("^#?[0-9A-Fa-f]{6}$"))) {
                        val normalized = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
                        selectedColor = normalized
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

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

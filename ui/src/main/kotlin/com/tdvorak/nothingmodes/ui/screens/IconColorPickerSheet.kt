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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
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
    IconEntry("star", Icons.Outlined.Star, listOf("star", "favorite", "rate")),
    IconEntry("home", Icons.Outlined.Home, listOf("home", "house", "start")),
    IconEntry("wifi", Icons.Outlined.Wifi, listOf("wifi", "wireless", "network", "internet")),
    IconEntry("bluetooth", Icons.Outlined.Bluetooth, listOf("bluetooth", "wireless", "bt")),
    IconEntry("sun", Icons.Outlined.WbSunny, listOf("sun", "day", "light", "bright", "morning")),
    IconEntry("dark_mode", Icons.Outlined.DarkMode, listOf("dark", "night", "moon", "sleep")),
    IconEntry("brightness", Icons.Outlined.Brightness6, listOf("brightness", "screen", "dim")),
    IconEntry("lightbulb", Icons.Outlined.Lightbulb, listOf("light", "bulb", "idea", "lamp")),
    IconEntry("notification", Icons.Outlined.Notifications, listOf("notification", "alert", "bell", "notify")),
    IconEntry("volume", Icons.AutoMirrored.Outlined.VolumeUp, listOf("volume", "sound", "audio", "loud")),
    IconEntry("music", Icons.Outlined.MusicNote, listOf("music", "song", "audio", "play", "media")),
    IconEntry("alarm", Icons.Outlined.Alarm, listOf("alarm", "wake", "clock", "time")),
    IconEntry("timer", Icons.Outlined.Timer, listOf("timer", "countdown", "stopwatch", "time")),
    IconEntry("clock", Icons.Outlined.AccessTime, listOf("clock", "time", "watch", "hour")),
    IconEntry("location", Icons.Outlined.LocationOn, listOf("location", "gps", "map", "place", "pin")),
    IconEntry("language", Icons.Outlined.Language, listOf("language", "translate", "global", "world")),
    IconEntry("airplane", Icons.Outlined.Flight, listOf("airplane", "flight", "travel", "plane")),
    IconEntry("power", Icons.Outlined.PowerSettingsNew, listOf("power", "off", "shutdown", "reboot")),
    IconEntry("battery", Icons.Outlined.BatteryFull, listOf("battery", "charge", "power", "energy")),
    IconEntry("flashlight", Icons.Outlined.FlashlightOn, listOf("flashlight", "torch", "light", "led")),
    IconEntry("campaign", Icons.Outlined.Campaign, listOf("campaign", "announce", "broadcast", "megaphone")),
    IconEntry("message", Icons.Outlined.Textsms, listOf("message", "sms", "text", "chat")),
    IconEntry("email", Icons.Outlined.Email, listOf("email", "mail", "inbox", "letter")),
    IconEntry("phone", Icons.Outlined.Phone, listOf("phone", "call", "dial", "ring")),
    IconEntry("settings", Icons.Outlined.Settings, listOf("settings", "gear", "config", "preferences")),
    IconEntry("speed", Icons.Outlined.Speed, listOf("speed", "fast", "performance", "gauge")),
    IconEntry("lock", Icons.Outlined.Lock, listOf("lock", "secure", "password", "screen lock")),
    IconEntry("vibration", Icons.Outlined.Vibration, listOf("vibration", "vibrate", "haptic", "buzz")),
    IconEntry("mic", Icons.Outlined.Mic, listOf("mic", "microphone", "record", "voice")),
    IconEntry("cloud", Icons.Outlined.Cloud, listOf("cloud", "weather", "sync", "sky")),
    IconEntry("air", Icons.Outlined.Air, listOf("air", "wind", "breeze", "fan")),
    IconEntry("work", Icons.Outlined.Work, listOf("work", "office", "business", "briefcase", "job")),
    IconEntry("fitness", Icons.Outlined.FitnessCenter, listOf("fitness", "gym", "workout", "exercise", "health")),
    IconEntry("food", Icons.Outlined.Fastfood, listOf("food", "eat", "meal", "lunch", "dinner", "restaurant")),
    IconEntry("shopping", Icons.Outlined.ShoppingCart, listOf("shopping", "cart", "buy", "store", "shop")),
    IconEntry("game", Icons.Outlined.Gamepad, listOf("game", "play", "controller", "gaming")),
    IconEntry("person", Icons.Outlined.AccountCircle, listOf("person", "user", "account", "profile", "contact")),
    IconEntry("check", Icons.Outlined.CheckCircle, listOf("check", "done", "complete", "confirm", "ok")),
)

fun iconForName(name: String): ImageVector =
    iconOptions.find { it.name == name }?.icon ?: Icons.Outlined.Star

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IconColorPickerSheet(
    initialIcon: String,
    initialColor: String,
    onDone: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIcon by remember { mutableStateOf(initialIcon.ifBlank { "star" }) }
    var searchQuery by remember { mutableStateOf("") }
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
        scrimColor = Color.Black.copy(alpha = 0.8f),
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
                text = "Choose icon",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = GeistSans,
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            // Live preview — monochrome icon on surface, onSurface tint.
            NothingIconCircle(size = 80f) {
                Icon(
                    imageVector = iconForName(selectedIcon),
                    contentDescription = selectedIcon,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            // Icon search
            NothingInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search",
                placeholder = "Search icons...",
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
                    onClick = { onDone(selectedIcon, initialColor) },
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
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
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

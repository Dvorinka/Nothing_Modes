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
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDragHandle
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSecondaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono

private data class IconEntry(
    val name: String,
    val icon: ImageVector,
    val keywords: List<String>,
)

private val iconOptions =
    listOf(
        IconEntry("star", Icons.Outlined.Star, listOf("star", "favorite", "rate")),
        IconEntry("home", Icons.Outlined.Home, listOf("home", "house", "start")),
        IconEntry("wifi", Icons.Outlined.Wifi, listOf("wifi", "wireless", "network", "internet")),
        IconEntry("bluetooth", Icons.Outlined.Bluetooth, listOf("bluetooth", "wireless", "bt")),
        IconEntry("sun", Icons.Outlined.WbSunny, listOf("sun", "day", "light", "bright", "morning")),
        IconEntry("dark_mode", Icons.Outlined.DarkMode, listOf("dark", "night", "moon", "sleep")),
        IconEntry("brightness", Icons.Outlined.Brightness6, listOf("brightness", "screen", "dim")),
        IconEntry("bolt", Icons.Outlined.Bolt, listOf("bolt", "zap", "flash", "lightning")),
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
        IconEntry("bed", Icons.Outlined.Bed, listOf("bed", "sleep", "night", "rest")),
        IconEntry("nights", Icons.Outlined.NightsStay, listOf("night", "moon", "sleep", "dark")),
        IconEntry("dnd", Icons.Outlined.DoNotDisturb, listOf("dnd", "do not disturb", "silent", "quiet")),
        IconEntry("coffee", Icons.Outlined.Coffee, listOf("coffee", "morning", "wake", "drink", "cafe")),
        IconEntry("car", Icons.Outlined.DirectionsCar, listOf("car", "drive", "commute", "travel")),
        IconEntry("hospital", Icons.Outlined.LocalHospital, listOf("hospital", "health", "medical", "doctor")),
        IconEntry("movie", Icons.Outlined.Movie, listOf("movie", "film", "cinema", "video")),
        IconEntry("book", Icons.AutoMirrored.Outlined.MenuBook, listOf("book", "read", "study", "library")),
        // Nature & weather
        IconEntry("ac_unit", Icons.Outlined.AcUnit, listOf("ac", "cold", "snow", "winter", "freeze")),
        IconEntry("beach", Icons.Outlined.BeachAccess, listOf("beach", "summer", "sun", "vacation")),
        IconEntry("cloud", Icons.Outlined.Cloud, listOf("cloud", "weather", "sync", "sky")),
        IconEntry("cyclone", Icons.Outlined.Cyclone, listOf("cyclone", "storm", "wind", "weather")),
        IconEntry("eco", Icons.Outlined.Eco, listOf("eco", "leaf", "green", "nature")),
        IconEntry("fire", Icons.Outlined.LocalFireDepartment, listOf("fire", "flame", "hot", "burn")),
        IconEntry("nature", Icons.Outlined.Nature, listOf("nature", "tree", "plant", "forest")),
        IconEntry("park", Icons.Outlined.Park, listOf("park", "nature", "tree", "garden")),
        IconEntry("storm", Icons.Outlined.Storm, listOf("storm", "thunder", "weather", "rain")),
        IconEntry("umbrella", Icons.Outlined.Umbrella, listOf("umbrella", "rain", "weather", "dry")),
        IconEntry("water", Icons.Outlined.Water, listOf("water", "drop", "aqua", "drink")),
        IconEntry("waves", Icons.Outlined.Waves, listOf("waves", "ocean", "water", "sea")),
        IconEntry("wb_cloudy", Icons.Outlined.WbCloudy, listOf("cloudy", "weather", "overcast")),
        // Transport & places
        IconEntry("airplane", Icons.Outlined.Flight, listOf("airplane", "flight", "travel", "plane")),
        IconEntry("airport", Icons.Outlined.LocalAirport, listOf("airport", "travel", "plane", "fly")),
        IconEntry("bike", Icons.Outlined.PedalBike, listOf("bike", "bicycle", "cycle", "sport")),
        IconEntry("boat", Icons.Outlined.DirectionsBoat, listOf("boat", "ship", "ferry", "sea")),
        IconEntry("bus", Icons.Outlined.DirectionsBus, listOf("bus", "transport", "public", "commute")),
        IconEntry("car", Icons.Outlined.DirectionsCar, listOf("car", "drive", "commute", "travel")),
        IconEntry("electric_car", Icons.Outlined.ElectricCar, listOf("electric", "car", "ev", "green")),
        IconEntry("gas", Icons.Outlined.LocalGasStation, listOf("gas", "fuel", "station", "petrol")),
        IconEntry("grocery", Icons.Outlined.LocalGroceryStore, listOf("grocery", "store", "shop", "food")),
        IconEntry("hotel", Icons.Outlined.LocalHotel, listOf("hotel", "sleep", "travel", "room")),
        IconEntry("mall", Icons.Outlined.LocalMall, listOf("mall", "shopping", "store", "buy")),
        IconEntry("parking", Icons.Outlined.LocalParking, listOf("parking", "car", "spot", "park")),
        IconEntry("pizza", Icons.Outlined.LocalPizza, listOf("pizza", "food", "dinner", "fast food")),
        IconEntry("railway", Icons.Outlined.DirectionsRailway, listOf("train", "railway", "transport")),
        IconEntry("restaurant", Icons.Outlined.Restaurant, listOf("restaurant", "dinner", "food", "eat")),
        IconEntry("rocket", Icons.Outlined.Rocket, listOf("rocket", "space", "launch", "startup")),
        IconEntry("run", Icons.AutoMirrored.Outlined.DirectionsRun, listOf("run", "jog", "exercise", "sport")),
        IconEntry("subway", Icons.Outlined.Subway, listOf("subway", "metro", "train", "transport")),
        IconEntry("taxi", Icons.Outlined.LocalTaxi, listOf("taxi", "cab", "transport", "car")),
        IconEntry("train", Icons.Outlined.Train, listOf("train", "rail", "transport", "travel")),
        IconEntry("tram", Icons.Outlined.Tram, listOf("tram", "transport", "city", "rail")),
        IconEntry("walk", Icons.AutoMirrored.Outlined.DirectionsWalk, listOf("walk", "steps", "hike", "pedestrian")),
        // Activities & objects
        IconEntry("bath", Icons.Outlined.Bathtub, listOf("bath", "shower", "bathroom", "clean")),
        IconEntry("brush", Icons.Outlined.Brush, listOf("brush", "paint", "art", "design")),
        IconEntry("camera", Icons.Outlined.CameraAlt, listOf("camera", "photo", "picture", "shoot")),
        IconEntry("celebration", Icons.Outlined.Celebration, listOf("celebration", "party", "event", "confetti")),
        IconEntry("cookie", Icons.Outlined.Cookie, listOf("cookie", "sweet", "biscuit", "snack")),
        IconEntry("dining", Icons.Outlined.DinnerDining, listOf("dining", "dinner", "restaurant", "fork")),
        IconEntry("edit", Icons.Outlined.Edit, listOf("edit", "write", "pencil", "modify")),
        IconEntry("event", Icons.Outlined.Event, listOf("event", "calendar", "date", "schedule")),
        IconEntry("flower", Icons.Outlined.LocalFlorist, listOf("flower", "florist", "plant", "rose")),
        IconEntry("gift", Icons.Outlined.CardGiftcard, listOf("gift", "present", "card", "birthday")),
        IconEntry("icecream", Icons.Outlined.Icecream, listOf("icecream", "dessert", "sweet", "cold")),
        IconEntry("laptop", Icons.Outlined.Laptop, listOf("laptop", "computer", "work", "device")),
        IconEntry("light", Icons.Outlined.Lightbulb, listOf("light", "bulb", "lamp", "idea")),
        IconEntry("medical", Icons.Outlined.MedicalServices, listOf("medical", "health", "doctor", "care")),
        IconEntry("money", Icons.Outlined.AttachMoney, listOf("money", "cash", "dollar", "finance")),
        IconEntry("movie", Icons.Outlined.Movie, listOf("movie", "film", "cinema", "video")),
        IconEntry("pets", Icons.Outlined.Pets, listOf("pets", "dog", "cat", "animal")),
        IconEntry("photo", Icons.Outlined.PhotoCamera, listOf("photo", "camera", "picture", "image")),
        IconEntry("pool", Icons.Outlined.Pool, listOf("pool", "swim", "water", "sport")),
        IconEntry("science", Icons.Outlined.Science, listOf("science", "lab", "chemistry", "test")),
        IconEntry("shopping_bag", Icons.Outlined.ShoppingBag, listOf("shopping", "bag", "buy", "store")),
        IconEntry("shopping_basket", Icons.Outlined.ShoppingBasket, listOf("basket", "shopping", "buy", "cart")),
        IconEntry("shower", Icons.Outlined.Shower, listOf("shower", "bath", "clean", "water")),
        IconEntry("skateboard", Icons.Outlined.Skateboarding, listOf("skateboard", "skate", "sport")),
        IconEntry("smartphone", Icons.Outlined.Smartphone, listOf("smartphone", "phone", "mobile", "device")),
        IconEntry("sports_bar", Icons.Outlined.SportsBar, listOf("sports bar", "bar", "drink", "pub")),
        IconEntry("sports_baseball", Icons.Outlined.SportsBaseball, listOf("baseball", "sport", "ball")),
        IconEntry("sports_basketball", Icons.Outlined.SportsBasketball, listOf("basketball", "sport", "ball")),
        IconEntry("sports_football", Icons.Outlined.SportsFootball, listOf("football", "sport", "ball", "soccer")),
        IconEntry("sports_golf", Icons.Outlined.SportsGolf, listOf("golf", "sport", "club")),
        IconEntry("sports_soccer", Icons.Outlined.SportsSoccer, listOf("soccer", "football", "sport", "ball")),
        IconEntry("sports_tennis", Icons.Outlined.SportsTennis, listOf("tennis", "sport", "racket")),
        IconEntry("stadium", Icons.Outlined.Stadium, listOf("stadium", "arena", "sport", "event")),
        IconEntry("store", Icons.Outlined.Store, listOf("store", "shop", "market", "buy")),
        IconEntry("storefront", Icons.Outlined.Storefront, listOf("storefront", "shop", "market", "business")),
        IconEntry("surfing", Icons.Outlined.Surfing, listOf("surfing", "surf", "beach", "sport")),
        IconEntry("theater", Icons.Outlined.TheaterComedy, listOf("theater", "comedy", "show", "entertainment")),
        IconEntry("toys", Icons.Outlined.Toys, listOf("toys", "play", "kids", "game")),
        IconEntry("train", Icons.Outlined.Train, listOf("train", "rail", "transport", "travel")),
        IconEntry("wallet", Icons.Outlined.Wallet, listOf("wallet", "money", "cash", "pay")),
        IconEntry("watch", Icons.Outlined.Watch, listOf("watch", "time", "wearable", "clock")),
        IconEntry("wine", Icons.Outlined.WineBar, listOf("wine", "bar", "drink", "alcohol")),
    )

fun iconForName(name: String): ImageVector = iconOptions.find { it.name == name }?.icon ?: Icons.Outlined.Star

private val emojiToIconName =
    mapOf(
        "🌙" to "dark_mode", "💡" to "lightbulb", "⚡" to "bolt", "🎧" to "music",
        "🏠" to "home", "🏡" to "home", "✈️" to "airplane", "🛫" to "airplane",
        "📍" to "location", "🗺️" to "location", "⏰" to "alarm", "🕐" to "clock",
        "⏱️" to "timer", "⏲️" to "timer", "🔕" to "notification", "🔔" to "notification",
        "💼" to "work", "🎮" to "game", "🎯" to "game", "🏋️" to "fitness",
        "💪" to "fitness", "🍔" to "food", "🍽️" to "food", "🛒" to "shopping",
        "☀️" to "sun", "🌅" to "sun", "🔆" to "brightness", "📶" to "wifi",
        "🔊" to "volume", "🎵" to "music", "📱" to "phone", "☎️" to "phone",
        "💬" to "message", "📧" to "email", "⚙️" to "settings", "🔧" to "settings",
        "⭐" to "star", "🌐" to "language", "👤" to "person", "✅" to "check",
        "🔒" to "lock", "📳" to "vibration", "🎤" to "mic", "☁️" to "cloud",
        "💨" to "air", "🔦" to "flashlight", "📢" to "campaign", "⏻" to "power",
        "🔋" to "battery", "🌡️" to "air",
    )

fun iconForEmoji(emoji: String): ImageVector = iconForName(emojiToIconName[emoji.trim()] ?: "")

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

    val filteredIcons =
        remember(searchQuery) {
            if (searchQuery.isBlank()) {
                iconOptions
            } else {
                iconOptions.filter { entry ->
                    entry.name.contains(searchQuery, ignoreCase = true) ||
                        entry.keywords.any { it.contains(searchQuery, ignoreCase = true) }
                }
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
        dragHandle = { NothingDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
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
                fontFamily = NothingFonts.mono(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            FlowRow(
                modifier =
                    Modifier
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
                NothingSecondaryButton(
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
        modifier =
            Modifier
                .size(48.dp)
                .background(
                    if (selected) {
                        NothingColors.accent.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    NothingShapes.iconChip,
                ).border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) NothingColors.accent else MaterialTheme.colorScheme.outline,
                    shape = NothingShapes.iconChip,
                ).clickable(onClick = onClick),
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

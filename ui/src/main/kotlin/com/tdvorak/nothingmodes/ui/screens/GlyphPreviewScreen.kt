package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.nothing.GlyphPresets
import com.tdvorak.nothingmodes.nothing.GlyphToysBridge
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingRedDot
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GlyphPreviewViewModel @Inject constructor(
    private val stripeProvider: NothingGlyphProvider,
    private val matrixProvider: NothingGlyphMatrixProvider,
) : ViewModel() {

    val glyphAvailable: Boolean
        get() = stripeProvider.isAvailable() || matrixProvider.isAvailable()

    /** Blank every glyph output — the manual "reset" for stuck text/frames. */
    fun turnOffNow() {
        runCatching { stripeProvider.turnOff() }
        runCatching { matrixProvider.turnOff() }
    }
}

@Composable
fun GlyphPreviewScreen(
    onBack: () -> Unit,
    viewModel: GlyphPreviewViewModel = hiltViewModel(),
) {
    val presets = remember {
        listOf(
            "Sleep Mode" to GlyphPresets.sleepMode,
            "Morning" to GlyphPresets.morning,
            "Work Focus" to GlyphPresets.workFocus,
            "DND Active" to GlyphPresets.dndActive,
            "DND Off" to GlyphPresets.dndOff,
            "Automation Fired" to GlyphPresets.automationFired,
            "Error" to GlyphPresets.error,
            "Success" to GlyphPresets.success,
            "Charging Start" to GlyphPresets.chargingStart,
            "Charging Complete" to GlyphPresets.chargingComplete,
            "Incoming Call" to GlyphPresets.incomingCall,
            "SMS Received" to GlyphPresets.smsReceived,
            "Timer Fired" to GlyphPresets.timerFired,
            "Off" to GlyphPresets.off,
        )
    }
    var selected by remember { mutableStateOf<GlyphPresets.GlyphVisual>(GlyphPresets.sleepMode) }
    var selectedName by remember { mutableStateOf("Sleep Mode") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val toysBridge = remember { GlyphToysBridge(context) }
    var systemInstalled by remember { mutableStateOf(toysBridge.isGlyphSystemInstalled()) }
    var registeredToys by remember { mutableStateOf(toysBridge.listRegisteredToys()) }
    var systemToys by remember { mutableStateOf(toysBridge.listSystemToys()) }
    var activeAodToy by remember { mutableStateOf(toysBridge.activeAodToy()) }

    // Refresh when the user returns from the system Glyph Toys screens.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                systemInstalled = toysBridge.isGlyphSystemInstalled()
                registeredToys = toysBridge.listRegisteredToys()
                systemToys = toysBridge.listSystemToys()
                activeAodToy = toysBridge.activeAodToy()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(title = "Glyph Preview", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = NothingSpacing.md,
                end = NothingSpacing.md,
                top = NothingSpacing.lg,
                bottom = NothingSpacing.xxxl,
            ),
        ) {
            // ── System Glyph Toys integration ──────────────────────────────
            item {
                NothingSectionHeader(text = "Glyph Toys (system)")
                NothingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingLabel(text = "System app")
                        Text(
                            text = if (systemInstalled) "Found" else "Not found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (systemInstalled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SpaceMono,
                        )
                    }
                    if (!systemInstalled) {
                        Text(
                            text = "The Glyph Toys manager is part of Nothing OS. On this device you can still register this app's toy — it appears in the list below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = NothingSpacing.sm),
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = NothingSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        ) {
                            GlyphLinkButton("Open toys", Modifier.weight(1f)) { toysBridge.openToysManager() }
                            GlyphLinkButton("Always-on", Modifier.weight(1f)) { toysBridge.openAodToyPicker() }
                            GlyphLinkButton("Timeout", Modifier.weight(1f)) { toysBridge.openTimeoutSettings() }
                        }
                        activeAodToy?.let {
                            Text(
                                text = "Always-on toy: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SpaceMono,
                                modifier = Modifier.padding(top = NothingSpacing.sm),
                            )
                        }
                    }
                }
            }

            // Live control — the reset the user was missing.
            item {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            NothingLabel(text = "Glyph output")
                            Text(
                                text = if (viewModel.glyphAvailable) "Hardware detected"
                                else "No Glyph hardware on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = NothingSpacing.xs),
                            )
                        }
                        Text(
                            text = "TURN OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = NothingColors.accent,
                            fontFamily = SpaceMono,
                            modifier = Modifier
                                .clip(NothingShapes.input)
                                .clickable(enabled = viewModel.glyphAvailable) {
                                    viewModel.turnOffNow()
                                }
                                .padding(
                                    horizontal = NothingSpacing.md,
                                    vertical = NothingSpacing.sm,
                                ),
                        )
                    }
                }
            }

            // Registered toys on the system (our app included).
            if (registeredToys.isNotEmpty()) {
                item {
                    NothingSectionHeader(text = "Registered toys")
                    NothingCard {
                        registeredToys.forEachIndexed { index, toy ->
                            if (index > 0) NothingDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = NothingSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = toy.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = SpaceMono,
                                    modifier = Modifier.weight(1f),
                                )
                                if (toy.isOurs) {
                                    Text(
                                        text = "THIS APP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NothingColors.accent,
                                        fontFamily = SpaceMono,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (systemToys.isNotEmpty()) {
                item {
                    NothingSectionHeader(text = "System toy table")
                    NothingCard {
                        systemToys.forEachIndexed { index, row ->
                            if (index > 0) NothingDivider()
                            Text(
                                text = row,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SpaceMono,
                                modifier = Modifier.padding(vertical = NothingSpacing.xs),
                            )
                        }
                    }
                }
            }

            // Hero — selected preset name in Doto
            item {
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = Doto,
                )
                NothingLabel(text = "Glyph Preset")
            }

            // Stripe preview — the one break in the grid (circular/visual element)
            item {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingCard(borderless = true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(5f)
                            .clip(NothingShapes.input)
                            .background(Color.Black),
                    ) {
                        StripeCanvas(selected)
                    }
                    Text(
                        text = descriptionFor(selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = NothingSpacing.sm),
                    )
                }
            }

            // Preset list
            item {
                Spacer(modifier = Modifier.height(NothingSpacing.xl))
                NothingSectionHeader(text = "Presets")
                NothingCard {
                    presets.forEachIndexed { index, (name, visual) ->
                        val isSelected = visual == selected
                        if (index > 0) NothingDivider()
                        NothingListRow(
                            title = name,
                            selected = isSelected,
                            onClick = {
                                selected = visual
                                selectedName = name
                            },
                            leading = {
                                NothingIconCircle(size = 40f) {
                                    NothingRedDot(size = 6f)
                                }
                            },
                            trailing = {
                                if (isSelected) {
                                    NothingRedDot(size = 6f)
                                } else {
                                    NothingRedDot(size = 6f, modifier = Modifier.size(0.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StripeCanvas(visual: GlyphPresets.GlyphVisual) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (visual) {
            is GlyphPresets.GlyphVisual.Stripe -> {
                val channelCount = 15
                val gap = 4f
                val ledWidth = (w - gap * (channelCount + 1)) / channelCount
                val litChannels = when {
                    visual.zone == "A" -> listOf(0)
                    visual.zone == "B" -> listOf(1)
                    visual.zone == "C" -> listOf(2, 3, 4, 5)
                    visual.zone == "D" -> (7..14).toList()
                    visual.zone == "E" -> listOf(6)
                    visual.progress != null -> {
                        val p = visual.progress!!
                        val count = (channelCount * p / 100).coerceIn(0, channelCount)
                        (0 until count).toList()
                    }
                    else -> (0 until channelCount).toList()
                }
                for (i in 0 until channelCount) {
                    val x = gap + i * (ledWidth + gap)
                    val color = if (i in litChannels) Color.White else Color(0xFF222222)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, gap),
                        size = androidx.compose.ui.geometry.Size(ledWidth, h - gap * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                    )
                }
            }
            is GlyphPresets.GlyphVisual.Matrix -> {
                val size = 25
                val cellW = w / size
                val cellH = h / size
                val matrixColor = visual.color?.let(::intColorToCompose) ?: intColorToCompose(visual.fillColor)
                for (row in 0 until size) {
                    for (col in 0 until size) {
                        val lit = when {
                            visual.color != null -> true
                            visual.percentFill != null -> {
                                val pf = visual.percentFill!!
                                val fillRows = (size * pf / 100)
                                (size - 1 - row) < fillRows
                            }
                            else -> false
                        }
                        val color = if (lit) matrixColor else Color(0xFF111111)
                        drawRect(
                            color = color,
                            topLeft = Offset(col * cellW, row * cellH),
                            size = androidx.compose.ui.geometry.Size(cellW - 1, cellH - 1),
                        )
                    }
                }
            }
            GlyphPresets.GlyphVisual.Off -> {
                drawRect(color = Color.Black)
            }
        }
    }
}

private fun descriptionFor(visual: GlyphPresets.GlyphVisual): String = when (visual) {
    is GlyphPresets.GlyphVisual.Stripe -> buildString {
        visual.zone?.let { append("Zone: $it. ") }
        if (visual.periodMs > 0) append("Period: ${visual.periodMs}ms. ")
        if (visual.cycles > 0) append("Cycles: ${visual.cycles}. ")
        visual.progress?.let { append("Progress: $it%. ") }
        if (periodMs(visual) == 0 && visual.cycles == 0 && visual.progress == null && visual.zone == null) {
            append("All channels on.")
        }
    }
    is GlyphPresets.GlyphVisual.Matrix -> buildString {
        visual.color?.let { append("Color fill. ") }
        visual.text?.let { append("Text: $it. ") }
        visual.scrollingText?.let { append("Scrolling: $it. ") }
        visual.percentFill?.let { append("Fill: $it%. ") }
        visual.number?.let { append("Number: $it. ") }
    }
    GlyphPresets.GlyphVisual.Off -> "All glyphs off."
}

private fun periodMs(v: GlyphPresets.GlyphVisual.Stripe) = v.periodMs

@Composable
private fun GlyphLinkButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Boolean,
) {
    Box(
        modifier = modifier
            .clip(NothingShapes.input)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = NothingSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = SpaceMono,
        )
    }
}

private fun intColorToCompose(color: Int): Color = Color(
    red = ((color shr 16) and 0xFF) / 255f,
    green = ((color shr 8) and 0xFF) / 255f,
    blue = (color and 0xFF) / 255f,
    alpha = ((color shr 24) and 0xFF) / 255f,
)

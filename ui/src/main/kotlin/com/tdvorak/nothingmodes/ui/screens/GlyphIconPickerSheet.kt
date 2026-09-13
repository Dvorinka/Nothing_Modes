package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.tdvorak.nothingmodes.data.community.CommunityApi
import com.tdvorak.nothingmodes.nothing.CustomGlyphStore
import com.tdvorak.nothingmodes.nothing.GlyphIconLibrary
import com.tdvorak.nothingmodes.nothing.GlyphMuseumPresets
import com.tdvorak.nothingmodes.ui.theme.LocalUiStyle
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingRedDot
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.ThemeManager
import kotlinx.coroutines.launch

private sealed class IconOption {
    abstract val key: String
    abstract val title: String
    abstract val subtitle: String

    data class Preset(
        val info: GlyphMuseumPresets.Info,
    ) : IconOption() {
        override val key: String get() = info.key
        override val title: String get() = info.title
        override val subtitle: String
            get() =
                buildString {
                    append("Glyph Museum")
                    info.author?.let { append(" · $it") }
                    append(" · ${info.gridSize}×${info.gridSize}")
                    if (info.frameCount > 1) append(" · ${info.frameCount} frames")
                }
    }

    data class Emoji(
        override val key: String,
    ) : IconOption() {
        override val title: String
            get() = key.replaceFirstChar { it.uppercase() }
        override val subtitle: String
            get() = GlyphIconLibrary.emojiFor(key)?.let { "Emoji · $it" } ?: "Standard icon"
    }

    data class Custom(
        override val key: String,
    ) : IconOption() {
        override val title: String get() = key
        override val subtitle: String get() = "Custom"
    }

    /** A glyph published on the community library — imported on first use. */
    data class Community(
        val item: CommunityApi.LibraryItem,
    ) : IconOption() {
        override val key: String get() = "community:${item.id}"
        override val title: String get() = item.title
        override val subtitle: String
            get() =
                buildString {
                    append("Community · @").append(item.handle)
                    if (item.summary.isNotBlank()) append(" · ").append(item.summary)
                }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphIconPickerDialog(
    initial: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    var selected by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    var importingId by remember { mutableStateOf<String?>(null) }

    var options by remember { mutableStateOf(emptyList<IconOption>()) }
    LaunchedEffect(Unit) {
        val presets = GlyphMuseumPresets(context).infos().map { IconOption.Preset(it) }
        val custom = CustomGlyphStore(context).names().map { IconOption.Custom(it) }
        val community =
            runCatching { CommunityApi.list(type = "glyph") }
                .getOrDefault(emptyList())
                .map { IconOption.Community(it) }
        val emoji = GlyphIconLibrary.names.map { IconOption.Emoji(it) }
        options = presets + custom + community + emoji
    }

    val filtered =
        remember(query, options) {
            if (query.isBlank()) {
                options
            } else {
                options.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true) ||
                        it.key.contains(query, ignoreCase = true)
                }
            }
        }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = NothingShapes.input,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 0.dp,
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 560.dp)
                    .padding(NothingSpacing.md),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(NothingSpacing.md),
            ) {
                Text(
                    text = if (classic) "Choose icon" else "CHOOSE ICON",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.doto(),
                )

                Spacer(modifier = Modifier.height(NothingSpacing.md))

                NothingInput(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search",
                    placeholder = "Search icons, presets, or creators...",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(NothingSpacing.sm))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.key }) { option ->
                        GlyphIconRow(
                            option = option,
                            selected = selected == option.key,
                            classic = classic,
                            busy = importingId == option.key,
                            onClick = {
                                when (option) {
                                    is IconOption.Community -> {
                                        if (importingId != null) return@GlyphIconRow
                                        importingId = option.key
                                        scope.launch {
                                            val stored =
                                                runCatching {
                                                    // Seed-fallback items carry
                                                    // the payload inline — no fetch.
                                                    val payload =
                                                        option.item.payload
                                                            ?: CommunityApi.fetchItem(option.item.id)
                                                    CustomGlyphStore(context)
                                                        .import(payload.toString(), option.item.title)
                                                }.getOrNull()
                                            importingId = null
                                            if (stored != null) {
                                                selected = option.key
                                                onSelect(stored)
                                            }
                                        }
                                    }
                                    else -> {
                                        selected = option.key
                                        onSelect(option.key)
                                    }
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
private fun GlyphIconRow(
    option: IconOption,
    selected: Boolean,
    classic: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (busy) 0.5f else 1f)
                .clickable(onClick = onClick)
                .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (option is IconOption.Community) {
            val design = remember(option.item.preview) { glyphPreviewFromJson(option.item.preview) }
            design?.let {
                GlyphDesignThumbnail(
                    design = it,
                    modifier = Modifier.padding(end = NothingSpacing.sm).height(36.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (classic) option.title else option.title.uppercase(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = NothingFonts.mono(),
            )
            Text(
                text =
                    if (busy) {
                        "IMPORTING…"
                    } else if (classic) {
                        option.subtitle
                    } else {
                        option.subtitle.uppercase()
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }
        if (selected) {
            NothingRedDot(size = 6f)
        }
    }
}

/** A readable title for [key] across bundled presets, emoji, and custom glyphs. */
@Composable
fun glyphIconTitle(
    context: Context = LocalContext.current,
    key: String,
): String =
    remember(key) {
        GlyphMuseumPresets(context).info(key)?.title
            ?: GlyphIconLibrary.emojiFor(key)
            ?: key
    }

/** Field that opens the full glyph icon/preset picker. */
@Composable
fun GlyphIconField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    val context = LocalContext.current
    val title = glyphIconTitle(context, value)
    val subtitle =
        remember(value) {
            GlyphMuseumPresets(context).info(value)?.let { "Glyph Museum · ${it.author ?: "unknown"}" }
                ?: if (value in GlyphIconLibrary.names) "Standard icon" else "Custom glyph"
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (classic) "Icon" else "ICON",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.padding(bottom = NothingSpacing.xs),
        )
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = NothingShapes.input,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (classic) title else title.uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = NothingFonts.mono(),
                    )
                    Text(
                        text = if (classic) subtitle else subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                }
                Text(
                    text = "[OPEN]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }
    }
}

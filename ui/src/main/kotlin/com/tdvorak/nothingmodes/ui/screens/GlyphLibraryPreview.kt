package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.data.community.CommunityApi
import com.tdvorak.nothingmodes.nothing.GlyphFrameCodec
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Build a single-frame [GlyphFrameCodec.Design] from the server preview block.
 * The server returns first-frame pixels as 0-255; we scale to 0-4095.
 */
fun glyphPreviewFromJson(preview: JsonObject?): GlyphFrameCodec.Design? {
    val size = preview?.get("size")?.jsonPrimitive?.intOrNull ?: 25
    val durations = preview?.get("duration")?.jsonPrimitive?.intOrNull
    val firstFrame = preview?.get("firstFrame")?.jsonArray ?: return null
    val widths = GlyphFrameCodec.rowWidths(size)
    val grid = IntArray(size * size)
    var idx = 0
    for (row in 0 until size) {
        val start = (size - widths[row]) / 2
        for (c in 0 until widths[row]) {
            val v = firstFrame.getOrNull(idx)?.jsonPrimitive?.intOrNull?.coerceIn(0, 255) ?: 0
            grid[row * size + start + c] = v * 4095 / 255
            idx++
        }
    }
    if (idx == 0) return null
    return GlyphFrameCodec.Design(
        version = if (size == 13) 4 else 1,
        gridSize = size,
        frames = listOf(GlyphFrameCodec.Frame(grid, durations?.takeIf { it > 0 })),
        author = null,
        postId = null,
        url = null,
    )
}

/**
 * Render a static thumbnail of the first frame.
 */
@Composable
fun GlyphDesignThumbnail(
    design: GlyphFrameCodec.Design,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(NothingShapes.input)
                .background(Color.Black),
    ) {
        GlyphCanvas(
            gridSize = design.gridSize,
            pixels = design.frames.firstOrNull()?.pixels ?: IntArray(design.gridSize * design.gridSize),
        )
    }
}

/**
 * Render an animated Glyph Matrix from a full [design], looping frames.
 */
@Composable
fun AnimatedGlyphCanvas(
    design: GlyphFrameCodec.Design,
    modifier: Modifier = Modifier,
) {
    var frame by remember(design) { mutableStateOf(0) }
    LaunchedEffect(design) {
        while (true) {
            val d = design.frames.getOrNull(frame)?.durationMs?.takeIf { it > 0 } ?: 100
            delay(d.toLong())
            frame = (frame + 1) % design.frames.size.coerceAtLeast(1)
        }
    }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(NothingShapes.input)
                .background(Color.Black)
                .padding(NothingSpacing.sm),
    ) {
        GlyphCanvas(
            gridSize = design.gridSize,
            pixels = design.frames.getOrNull(frame)?.pixels ?: IntArray(design.gridSize * design.gridSize),
        )
    }
}

/**
 * A single community glyph row with a rendered thumbnail.
 */
@Composable
fun GlyphCommunityRow(
    item: CommunityApi.LibraryItem,
    onClick: () -> Unit,
) {
    val design = remember(item.preview) { glyphPreviewFromJson(item.preview) }
    NothingListRow(
        title = item.title,
        subtitle = "by @${item.handle}" + if (item.summary.isNotBlank()) " · ${item.summary}" else "",
        onClick = onClick,
        leading = {
            if (design != null) {
                GlyphDesignThumbnail(
                    design = design,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(NothingShapes.input)
                            .background(Color.Black),
                )
            }
        },
    )
}

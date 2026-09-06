package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.tdvorak.nothingmodes.nothing.CustomGlyphStore
import com.tdvorak.nothingmodes.nothing.GlyphFrameCodec
import com.tdvorak.nothingmodes.nothing.GlyphRasterizer
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GlyphEditorViewModel
    @Inject
    constructor(
        private val matrixProvider: NothingGlyphMatrixProvider,
        @ApplicationContext context: Context,
    ) : ViewModel() {
        private val store = CustomGlyphStore(context)

        fun save(name: String, json: String): Boolean = store.save(name, json)

        fun preview(pixels: IntArray) {
            matrixProvider.setFrame(pixels)
        }

        fun previewDesign(design: GlyphFrameCodec.Design) {
            matrixProvider.displayDesign(design)
        }

        fun designJson(name: String): String? = store.designJson(name)

        fun names(): List<String> = store.names()

        fun delete(name: String) {
            store.delete(name)
        }

        fun importJson(json: String): String? = store.import(json, null)

        fun author(name: String): String? = store.design(name)?.author
    }

@Composable
fun GlyphEditorScreen(
    onBack: () -> Unit,
    viewModel: GlyphEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var frames by remember { mutableStateOf(listOf(IntArray(625))) }
    var durations by remember { mutableStateOf(listOf(100)) }
    var frameIndex by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var savedNames by remember { mutableStateOf(viewModel.names()) }

    fun refreshNames() {
        savedNames = viewModel.names()
    }

    fun currentFrame(): IntArray = frames[frameIndex]

    fun replaceCurrent(newPixels: IntArray) {
        frames = frames.toMutableList().also { it[frameIndex] = newPixels }
    }

    fun toggleCell(row: Int, col: Int, paint: Boolean?) {
        val w = GlyphFrameCodec.ROWS_25[row]
        val start = (25 - w) / 2
        if (col !in start until start + w) return
        val idx = row * 25 + col
        val target = when (paint) {
            true -> 4095
            false -> 0
            null -> if (currentFrame()[idx] > 0) 0 else 4095
        }
        if (currentFrame()[idx] != target) {
            replaceCurrent(currentFrame().copyOf().also { it[idx] = target })
        }
    }

    fun cellFromOffset(offset: Offset, canvasSize: Float): Pair<Int, Int>? {
        if (canvasSize <= 0f) return null
        val cellW = canvasSize / 25f
        val col = (offset.x / cellW).toInt().coerceIn(0, 24)
        val row = (offset.y / cellW).toInt().coerceIn(0, 24)
        return row to col
    }

    fun addBlankFrame() {
        frames = frames + IntArray(625)
        durations = durations + 100
        frameIndex = frames.lastIndex
    }

    fun duplicateFrame() {
        frames = frames.toMutableList().also { it.add(frameIndex + 1, currentFrame().copyOf()) }
        durations = durations.toMutableList().also { it.add(frameIndex + 1, durations[frameIndex]) }
        frameIndex += 1
    }

    fun deleteFrame() {
        if (frames.size <= 1) return
        frames = frames.toMutableList().also { it.removeAt(frameIndex) }
        durations = durations.toMutableList().also { it.removeAt(frameIndex) }
        frameIndex = frameIndex.coerceAtMost(frames.lastIndex)
    }

    fun setDuration(value: Int) {
        durations = durations.toMutableList().also { it[frameIndex] = value.coerceAtLeast(0) }
    }

    fun loadDesign(design: GlyphFrameCodec.Design) {
        frames = design.frames.map { it.pixels.copyOf() }
        durations = design.frames.map { it.durationMs ?: 100 }
        frameIndex = 0
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(context, "Not an image", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (bitmap == null) {
            Toast.makeText(context, "Not an image", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val matrix = GlyphRasterizer.bitmapToMatrix(bitmap, 25)
        replaceCurrent(matrix)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(title = "Glyph Studio", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding =
                PaddingValues(
                    start = NothingSpacing.md,
                    end = NothingSpacing.md,
                    top = NothingSpacing.lg,
                    bottom = NothingSpacing.xxxl,
                ),
        ) {
            item {
                NothingSectionHeader(text = "Canvas")
                NothingCard {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(NothingShapes.input)
                                .background(Color.Black)
                                .pointerInput(frames, frameIndex) {
                                    detectTapGestures { offset ->
                                        cellFromOffset(offset, size.width.toFloat())?.let { (r, c) ->
                                            toggleCell(r, c, null)
                                        }
                                    }
                                }.pointerInput(frames, frameIndex) {
                                    var paintMode: Boolean? = null
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            cellFromOffset(offset, size.width.toFloat())?.let { (r, c) ->
                                                val w = GlyphFrameCodec.ROWS_25[r]
                                                val start = (25 - w) / 2
                                                if (c in start until start + w) {
                                                    val idx = r * 25 + c
                                                    paintMode = currentFrame()[idx] <= 0
                                                    toggleCell(r, c, paintMode)
                                                }
                                            }
                                        },
                                        onDragEnd = { paintMode = null },
                                        onDragCancel = { paintMode = null },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            cellFromOffset(change.position, size.width.toFloat())?.let { (r, c) ->
                                                toggleCell(r, c, paintMode)
                                            }
                                        },
                                    )
                                },
                    ) {
                        GlyphCanvas(frames[frameIndex])
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingSectionHeader(text = "Frames")
                NothingCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(NothingSpacing.md)) {
                        val stripState = rememberLazyListState()
                        LaunchedEffect(frameIndex, frames.size) {
                            stripState.animateScrollToItem(frameIndex.coerceIn(0, frames.lastIndex))
                        }
                        LazyRow(
                            state = stripState,
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(frames.size) { i ->
                                val selected = i == frameIndex
                                Box(
                                    modifier =
                                        Modifier
                                            .size(width = 36.dp, height = 32.dp)
                                            .clip(NothingShapes.input)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                            ).clickable { frameIndex = i },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                            if (selected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.primary,
                                        fontFamily = NothingFonts.mono(),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FrameAction("+", Modifier.weight(1f)) { addBlankFrame() }
                            FrameAction("DUP", Modifier.weight(1f)) { duplicateFrame() }
                            FrameAction(
                                "DEL",
                                Modifier.weight(1f),
                                enabled = frames.size > 1,
                            ) { deleteFrame() }
                        }
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        ) {
                            NothingLabel(
                                text = "ms",
                                modifier = Modifier,
                            )
                            NothingInput(
                                value = durations[frameIndex].toString(),
                                onValueChange = { text ->
                                    val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    setDuration(parsed)
                                },
                                label = "Duration",
                                placeholder = "100",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Design name",
                    placeholder = "my_glyph",
                )
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    EditorAction("Clear", Modifier.weight(1f)) {
                        replaceCurrent(IntArray(625))
                    }
                    EditorAction("Save", Modifier.weight(1f)) {
                        val trimmed = name.trim().ifEmpty { "custom_${savedNames.size + 1}" }
                        val json = GlyphFrameCodec.encode(frames, 25, durations)
                        val ok = viewModel.save(trimmed, json)
                        if (ok) {
                            Toast.makeText(context, "Saved as $trimmed", Toast.LENGTH_SHORT).show()
                            refreshNames()
                        } else {
                            Toast.makeText(context, "Invalid name", Toast.LENGTH_SHORT).show()
                        }
                    }
                    EditorAction("Show", Modifier.weight(1f)) {
                        runCatching {
                            if (frames.size == 1) {
                                viewModel.preview(currentFrame())
                            } else {
                                val json = GlyphFrameCodec.encode(frames, 25, durations)
                                val design = GlyphFrameCodec.decode(json)
                                viewModel.previewDesign(design)
                            }
                        }.onFailure {
                            Toast.makeText(context, "Preview failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    EditorAction("Museum", Modifier.weight(1f)) {
                        val json = GlyphFrameCodec.encode(frames, 25, durations)
                        val file = File(context.cacheDir, "glyph_design.json")
                        runCatching {
                            file.writeText(json)
                            val uri =
                                FileProvider.getUriForFile(
                                    context,
                                    context.packageName + ".fileprovider",
                                    file,
                                )
                            val intent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            context.startActivity(
                                Intent.createChooser(intent, "Send to Glyph Museum"),
                            )
                        }.onFailure {
                            Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    EditorAction("Image", Modifier.weight(1f)) {
                        imagePicker.launch("image/*")
                    }
                    EditorAction("Import", Modifier.weight(1f)) {
                        val text = clipboard.getText()?.text
                        if (text.isNullOrBlank()) {
                            Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                            return@EditorAction
                        }
                        val stored = viewModel.importJson(text)
                        if (stored != null) {
                            Toast.makeText(context, "Imported as $stored", Toast.LENGTH_SHORT).show()
                            refreshNames()
                        } else {
                            Toast.makeText(context, "Invalid design JSON", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            item {
                NothingSectionHeader(text = "Saved designs")
                if (savedNames.isEmpty()) {
                    NothingCard {
                        Text(
                            text = "No saved designs yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                } else {
                    NothingCard {
                        savedNames.forEachIndexed { index, entry ->
                            if (index > 0) NothingDivider()
                            val author = remember(entry) { viewModel.author(entry) }
                            NothingListRow(
                                title = entry,
                                subtitle = author?.let { "by @$it" } ?: "",
                                onClick = {
                                    viewModel.designJson(entry)?.let { json ->
                                        runCatching { GlyphFrameCodec.decode(json) }
                                            .getOrNull()?.let { design ->
                                                loadDesign(design)
                                                viewModel.previewDesign(design)
                                            }
                                    }
                                },
                                trailing = {
                                    Text(
                                        text = "X",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NothingColors.accent,
                                        fontFamily = NothingFonts.mono(),
                                        modifier =
                                            Modifier
                                                .clip(NothingShapes.input)
                                                .clickable {
                                                    viewModel.delete(entry)
                                                    refreshNames()
                                                }.padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlyphCanvas(pixels: IntArray) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasSize = size.width
        val cellW = canvasSize / 25f
        val radius = cellW * 0.38f
        for (row in 0 until 25) {
            val w = GlyphFrameCodec.ROWS_25[row]
            val start = (25 - w) / 2
            for (col in 0 until 25) {
                val inside = col in start until start + w
                val idx = row * 25 + col
                val cx = col * cellW + cellW / 2f
                val cy = row * cellW + cellW / 2f
                val color =
                    when {
                        !inside -> onSurface.copy(alpha = 0.08f)
                        pixels[idx] > 0 -> primary
                        else -> onSurface.copy(alpha = 0.25f)
                    }
                drawCircle(color = color, radius = radius, center = Offset(cx, cy))
            }
        }
    }
}

@Composable
private fun EditorAction(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(NothingShapes.input)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .padding(vertical = NothingSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = NothingFonts.mono(),
        )
    }
}

@Composable
private fun FrameAction(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg =
        if (enabled) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val fg =
        if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    Box(
        modifier =
            modifier
                .clip(NothingShapes.input)
                .background(bg)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = NothingSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontFamily = NothingFonts.mono(),
        )
    }
}

package com.tdvorak.nothingmodes.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.nothing.CustomGlyphStore
import com.tdvorak.nothingmodes.nothing.GlyphFrameCodec
import com.tdvorak.nothingmodes.nothing.GlyphRasterizer
import com.tdvorak.nothingmodes.nothing.GlyphResult
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.ui.theme.LocalUiStyle
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingRadio
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTag
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private enum class PaintMode { PAINT, SELECT }

private enum class EditorSection {
    ANIMATION,
    BRUSH,
    ACTIONS,
    IMPORT,
    SAVED,
    LIBRARY,
}

private enum class ImportType { IMAGE, GIF, VIDEO }

private data class TargetDevice(
    val label: String,
    val size: Int,
)

@HiltViewModel
class GlyphEditorViewModel
    @Inject
    constructor(
        private val matrixProvider: NothingGlyphMatrixProvider,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val store = CustomGlyphStore(context)

        /** The device matrix size (0 if this phone has no Glyph Matrix). */
        val matrixSize: Int = matrixProvider.matrixSize()

        /** Whether the running device has a Glyph Matrix that can be previewed. */
        val isMatrix: Boolean = matrixProvider.isAvailable()

        fun save(
            name: String,
            json: String,
        ): Boolean = store.save(name, json)

        /** Preview a full design, resampling to the device matrix if needed. */
        fun preview(design: GlyphFrameCodec.Design): GlyphResult = matrixProvider.displayDesign(design)

        /** Preview a single frame from the editor, resampling to the device. */
        fun previewFrame(
            frame: IntArray,
            gridSize: Int,
        ): GlyphResult {
            val design =
                GlyphFrameCodec.Design(
                    version = if (gridSize == 13) 4 else 1,
                    gridSize = gridSize,
                    frames = listOf(GlyphFrameCodec.Frame(frame, null)),
                    author = null,
                    postId = null,
                    url = null,
                )
            return matrixProvider.displayDesign(design)
        }

        fun designJson(name: String): String? = store.designJson(name)

        fun names(): List<String> = store.names().sorted()

        fun delete(name: String) {
            store.delete(name)
        }

        fun importJson(json: String): String? = store.import(json, null)

        fun author(name: String): String? = store.design(name)?.author

        // ── Community library ──────────────────────────────────────────────

        private val _library =
            kotlinx.coroutines.flow.MutableStateFlow<List<com.tdvorak.nothingmodes.data.community.CommunityApi.LibraryItem>>(
                emptyList(),
            )
        val library = _library

        private val _sharing = kotlinx.coroutines.flow.MutableStateFlow(false)
        val sharing = _sharing

        private val _shareResult =
            kotlinx.coroutines.flow.MutableStateFlow<com.tdvorak.nothingmodes.data.community.CommunityApi.SubmitResult?>(
                null,
            )
        val shareResult = _shareResult

        val creatorProfile =
            com.tdvorak.nothingmodes.ui.prefs
                .CreatorPreferences(context)

        init {
            viewModelScope.launch {
                _library.value =
                    runCatching {
                        com.tdvorak.nothingmodes.data.community.CommunityApi
                            .list(type = "glyph")
                    }.getOrDefault(emptyList())
            }
        }

        /** Publish a saved design to the community library (admin review). */
        fun publishGlyph(
            designName: String,
            title: String,
            description: String,
            handle: String,
            email: String,
            github: String,
        ) {
            val designJson =
                store.designJson(designName) ?: run {
                    _shareResult.value =
                        com.tdvorak.nothingmodes.data.community.CommunityApi.SubmitResult
                            .Failed("design not found")
                    return
                }
            viewModelScope.launch {
                _sharing.value = true
                _shareResult.value =
                    runCatching {
                        com.tdvorak.nothingmodes.data.community.CommunityApi.submit(
                            type = "glyph",
                            title = title,
                            description = description,
                            handle = handle,
                            email = email,
                            github = github,
                            payload =
                                com.tdvorak.nothingmodes.engine.model.EngineJson.json
                                    .parseToJsonElement(designJson),
                        )
                    }.getOrElse {
                        com.tdvorak.nothingmodes.data.community.CommunityApi.SubmitResult.Failed(
                            it.message ?: "submit failed",
                        )
                    }
                _sharing.value = false
            }
        }

        fun clearShareResult() {
            _shareResult.value = null
        }

        /** Import an approved community design; returns the stored name or null. */
        suspend fun importLibraryItem(item: com.tdvorak.nothingmodes.data.community.CommunityApi.LibraryItem): String? =
            runCatching {
                val payload =
                    com.tdvorak.nothingmodes.data.community.CommunityApi
                        .fetchItem(item.id)
                store.import(payload.toString(), item.title)
            }.getOrNull()
    }

private val targets =
    listOf(
        TargetDevice("25 \u00d7 25 \u00b7 Phone 3", 25),
        TargetDevice("13 \u00d7 13 \u00b7 Phone 4a Pro", 13),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlyphEditorScreen(
    onBack: () -> Unit,
    viewModel: GlyphEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC

    var gridSize by remember { mutableStateOf(viewModel.matrixSize.takeIf { it > 0 } ?: 25) }
    var frames by remember { mutableStateOf(listOf(IntArray(gridSize * gridSize))) }
    var durations by remember { mutableStateOf(listOf(100)) }
    var frameIndex by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var savedNames by remember { mutableStateOf(viewModel.names()) }
    var opacity by remember { mutableStateOf(255) }
    var paintMode by remember { mutableStateOf(PaintMode.PAINT) }
    var expanded by remember { mutableStateOf(setOf(EditorSection.BRUSH, EditorSection.ACTIONS)) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var dialogText by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<ImportType?>(null) }
    var shareDesign by remember { mutableStateOf<String?>(null) }
    var selectedLibraryItem by remember { mutableStateOf<com.tdvorak.nothingmodes.data.community.CommunityApi.LibraryItem?>(null) }
    var selectedLibraryDesign by remember { mutableStateOf<GlyphFrameCodec.Design?>(null) }
    var importing by remember { mutableStateOf(false) }
    val library by viewModel.library.collectAsState()
    var glyphSearch by remember { mutableStateOf("") }
    var glyphSort by remember { mutableStateOf("newest") }
    var glyphCaps by remember { mutableStateOf<Set<String>>(emptySet()) }
    val allGlyphCaps = remember(library) { library.flatMap { it.capabilities }.distinct().sorted() }
    val visibleLibrary =
        library
            .filter {
                glyphSearch.isBlank() ||
                    it.title.contains(glyphSearch, true) ||
                    it.handle.contains(glyphSearch, true)
            }.filter { glyphCaps.isEmpty() || it.capabilities.any { c -> glyphCaps.contains(c) } }
            .let { list ->
                when (glyphSort) {
                    "download" -> list.sortedByDescending { it.downloads }
                    "alpha" -> list.sortedBy { it.title.lowercase() }
                    else -> list
                }
            }
    val sharing by viewModel.sharing.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    val scope = rememberCoroutineScope()

    val brush = remember(opacity) { opacity * 4095 / 255 }

    fun refreshNames() {
        savedNames = viewModel.names()
    }

    fun currentFrame(): IntArray = frames[frameIndex]

    fun replaceCurrent(newPixels: IntArray) {
        frames = frames.toMutableList().also { it[frameIndex] = newPixels }
    }

    fun isInside(
        row: Int,
        col: Int,
    ): Boolean {
        val widths = GlyphFrameCodec.rowWidths(gridSize)
        val start = (gridSize - widths[row]) / 2
        return col in start until start + widths[row]
    }

    fun setPixel(
        row: Int,
        col: Int,
        value: Int,
    ) {
        if (!isInside(row, col)) return
        val idx = row * gridSize + col
        if (currentFrame()[idx] != value) {
            replaceCurrent(currentFrame().copyOf().also { it[idx] = value })
        }
    }

    fun togglePixel(
        row: Int,
        col: Int,
    ) {
        if (!isInside(row, col)) return
        val idx = row * gridSize + col
        val target = if (currentFrame()[idx] > 0) 0 else brush
        if (currentFrame()[idx] != target) {
            replaceCurrent(currentFrame().copyOf().also { it[idx] = target })
        }
    }

    fun cellFromOffset(
        offset: Offset,
        canvasSize: Float,
    ): Pair<Int, Int>? {
        if (canvasSize <= 0f) return null
        val cellW = canvasSize / gridSize.toFloat()
        val col = (offset.x / cellW).toInt().coerceIn(0, gridSize - 1)
        val row = (offset.y / cellW).toInt().coerceIn(0, gridSize - 1)
        return row to col
    }

    fun addBlankFrame() {
        frames = frames + IntArray(gridSize * gridSize)
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

    fun setGridSize(newSize: Int) {
        if (newSize == gridSize) return
        val newFrames = frames.map { GlyphFrameCodec.rescaleFrame(it, gridSize, newSize) }
        gridSize = newSize
        frames = newFrames
        frameIndex = frameIndex.coerceAtMost(frames.lastIndex)
    }

    fun loadDesign(design: GlyphFrameCodec.Design) {
        frames = design.frames.map { it.pixels.copyOf() }
        durations = design.frames.map { it.durationMs ?: 100 }
        gridSize = design.gridSize
        frameIndex = 0
    }

    fun encodeCurrent(): String =
        GlyphFrameCodec.encode(
            frames = frames,
            size = gridSize,
            durationsMs = durations,
        )

    fun showCurrent() {
        val result =
            if (frames.size == 1) {
                viewModel.previewFrame(currentFrame(), gridSize)
            } else {
                val design =
                    GlyphFrameCodec.Design(
                        version = if (gridSize == 13) 4 else 1,
                        gridSize = gridSize,
                        frames = frames.map { GlyphFrameCodec.Frame(it, durations.getOrNull(frames.indexOf(it))) },
                        author = null,
                        postId = null,
                        url = null,
                    )
                viewModel.preview(design)
            }
        if (result is GlyphResult.Failure) {
            Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
        }
    }

    fun saveCurrent() {
        val trimmed = name.trim().ifEmpty { "custom_${savedNames.size + 1}" }
        val json = encodeCurrent()
        if (viewModel.save(trimmed, json)) {
            Toast.makeText(context, "Saved as $trimmed", Toast.LENGTH_SHORT).show()
            refreshNames()
        } else {
            Toast.makeText(context, "Invalid name", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCurrent() {
        val json = encodeCurrent()
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
            context.startActivity(Intent.createChooser(intent, "Send to Glyph Museum"))
        }.onFailure {
            Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
        }
    }

    fun importRawJson() {
        val text = clipboard.getText()?.text
        if (text.isNullOrBlank()) {
            Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
            return
        }
        val stored = viewModel.importJson(text)
        if (stored != null) {
            Toast.makeText(context, "Imported as $stored", Toast.LENGTH_SHORT).show()
            refreshNames()
        } else {
            Toast.makeText(context, "Invalid design JSON", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyBitmap(bitmap: Bitmap) {
        val matrix = GlyphRasterizer.bitmapToMatrix(bitmap, gridSize)
        replaceCurrent(matrix)
    }

    fun importUri(uri: Uri) {
        val bitmap =
            when (pendingImport) {
                ImportType.IMAGE,
                ImportType.GIF,
                -> {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                        ?: runCatching {
                            val source = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        }.getOrNull()
                }
                ImportType.VIDEO -> {
                    runCatching {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        retriever.release()
                        frame
                    }.getOrNull()
                }
                null -> null
            }
        if (bitmap != null) {
            applyBitmap(bitmap)
        } else {
            Toast.makeText(context, "Could not load media", Toast.LENGTH_SHORT).show()
        }
        pendingImport = null
    }

    val mediaPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) {
                pendingImport = null
                return@rememberLauncherForActivityResult
            }
            importUri(uri)
        }

    fun pasteImage() {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = manager?.primaryClip
        if (clip == null || clip.itemCount == 0) {
            Toast.makeText(context, "No image in clipboard", Toast.LENGTH_SHORT).show()
            return
        }
        val item = clip.getItemAt(0)
        val uri = item.uri
        if (uri != null) {
            val bitmap =
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            if (bitmap != null) applyBitmap(bitmap) else Toast.makeText(context, "Could not load image", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No image in clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun importText(
        typeface: Typeface,
        scale: Float,
    ) {
        if (dialogText.isBlank()) {
            showTextDialog = false
            showEmojiDialog = false
            return
        }
        val matrix = GlyphRasterizer.rasterize(dialogText, gridSize, scale, typeface)
        replaceCurrent(matrix)
        dialogText = ""
        showTextDialog = false
        showEmojiDialog = false
    }

    fun toggleSection(section: EditorSection) {
        expanded =
            if (section in expanded) {
                expanded - section
            } else {
                expanded + section
            }
    }

    @Composable
    fun SectionHeader(
        label: String,
        section: EditorSection,
    ) {
        val arrow =
            if (section in expanded) {
                if (classic) "v" else "V"
            } else {
                ">"
            }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { toggleSection(section) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NothingSectionHeader(text = label, modifier = Modifier)
            Text(
                text = arrow,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }
    }

    if (showTextDialog) {
        TextImportDialog(
            label = "Text",
            value = dialogText,
            onValueChange = { dialogText = it },
            onConfirm = { importText(Typeface.DEFAULT_BOLD, 0.55f) },
            onDismiss = {
                dialogText = ""
                showTextDialog = false
            },
        )
    }

    if (showEmojiDialog) {
        TextImportDialog(
            label = "Emoji",
            value = dialogText,
            onValueChange = { dialogText = it },
            onConfirm = { importText(Typeface.DEFAULT, 0.72f) },
            onDismiss = {
                dialogText = ""
                showEmojiDialog = false
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { NothingTopBar(title = "Glyph Matrix Editor", onBack = onBack) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        start = NothingSpacing.md,
                        end = NothingSpacing.md,
                        top = NothingSpacing.lg,
                    ),
        ) {
            if (!viewModel.isMatrix) {
                NothingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(NothingSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingColors.accent.let { color ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .background(color),
                            )
                        }
                        Text(
                            text = if (classic) "This device has no Glyph Matrix. Designs are saved for other phones." else "THIS DEVICE HAS NO GLYPH MATRIX. DESIGNS ARE SAVED FOR OTHER PHONES.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.md))
            }

            NothingEnumSelector(
                label = "Target",
                value = targets.first { it.size == gridSize }.label,
                options = targets.map { it.label },
                onSelect = { selected ->
                    targets.firstOrNull { it.label == selected }?.let { setGridSize(it.size) }
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.md))

            CanvasCard(
                gridSize = gridSize,
                frame = currentFrame(),
                paintMode = paintMode,
                brush = brush,
                onTap = { row, col ->
                    when (paintMode) {
                        PaintMode.PAINT -> togglePixel(row, col)
                        PaintMode.SELECT -> setPixel(row, col, brush)
                    }
                },
                onDragStart = { row, col ->
                    val current = currentFrame()[row * gridSize + col]
                    val painting = current <= 0
                    if (painting) setPixel(row, col, brush) else setPixel(row, col, 0)
                    painting
                },
                onDrag = { row, col, painting ->
                    if (painting) setPixel(row, col, brush) else setPixel(row, col, 0)
                },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (classic) {
                            "${GlyphFrameCodec.activeCount(
                                currentFrame(),
                                gridSize,
                            )} / ${GlyphFrameCodec.ledCount(gridSize)} pixels active"
                        } else {
                            "${GlyphFrameCodec.activeCount(currentFrame(), gridSize)} / ${GlyphFrameCodec.ledCount(gridSize)} PIXELS ACTIVE"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
                Text(
                    text = if (classic) targets.first { it.size == gridSize }.label else targets.first { it.size == gridSize }.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
            Spacer(modifier = Modifier.height(NothingSpacing.md))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding =
                    PaddingValues(
                        bottom = NothingSpacing.xxxl,
                    ),
            ) {
                item {
                    NothingCard {
                        SectionHeader(label = "Animation", section = EditorSection.ANIMATION)
                        AnimatedVisibility(visible = EditorSection.ANIMATION in expanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                        FrameChip(
                                            number = i + 1,
                                            selected = selected,
                                            onClick = { frameIndex = i },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                ) {
                                    GlyphPill("+", Modifier.weight(1f)) { addBlankFrame() }
                                    GlyphPill("DUP", Modifier.weight(1f)) { duplicateFrame() }
                                    GlyphPill(
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
                                    NothingLabel(text = "Duration")
                                    NothingInput(
                                        value = durations[frameIndex].toString(),
                                        onValueChange = { text ->
                                            val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                                            setDuration(parsed)
                                        },
                                        label = "ms",
                                        placeholder = "100",
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    )
                                }
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                NothingPillButton(
                                    text = "Play",
                                    onClick = { showCurrent() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    NothingCard {
                        SectionHeader(label = "Brush", section = EditorSection.BRUSH)
                        AnimatedVisibility(visible = EditorSection.BRUSH in expanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                                ) {
                                    RadioRow(
                                        label = if (classic) "Drag to paint" else "DRAG TO PAINT",
                                        selected = paintMode == PaintMode.PAINT,
                                        onClick = { paintMode = PaintMode.PAINT },
                                        modifier = Modifier.weight(1f),
                                    )
                                    RadioRow(
                                        label = if (classic) "Select" else "SELECT",
                                        selected = paintMode == PaintMode.SELECT,
                                        onClick = { paintMode = PaintMode.SELECT },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(modifier = Modifier.height(NothingSpacing.md))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                ) {
                                    Text(
                                        text = if (classic) "Opacity" else "OPACITY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = NothingFonts.mono(),
                                        modifier = Modifier.width(56.dp),
                                    )
                                    Slider(
                                        value = opacity / 255f,
                                        onValueChange = { opacity = (it * 255).toInt().coerceIn(0, 255) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f),
                                        colors =
                                            SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            ),
                                    )
                                    Text(
                                        text = "$opacity",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = NothingFonts.mono(),
                                        modifier = Modifier.width(40.dp),
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    NothingCard {
                        SectionHeader(label = "Actions", section = EditorSection.ACTIONS)
                        AnimatedVisibility(visible = EditorSection.ACTIONS in expanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ActionRow(
                                    listOf(
                                        "Fill" to { replaceCurrent(GlyphFrameCodec.fill(currentFrame(), gridSize, brush)) },
                                        "Clear" to { replaceCurrent(IntArray(gridSize * gridSize)) },
                                        "Invert" to { replaceCurrent(GlyphFrameCodec.invert(currentFrame(), gridSize)) },
                                    ),
                                )
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                ActionRow(
                                    listOf(
                                        "Mirror" to { replaceCurrent(GlyphFrameCodec.flipHorizontal(currentFrame(), gridSize)) },
                                        "Flip" to { replaceCurrent(GlyphFrameCodec.flipVertical(currentFrame(), gridSize)) },
                                        "Rotate" to { replaceCurrent(GlyphFrameCodec.rotate90(currentFrame(), gridSize)) },
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    NothingCard {
                        SectionHeader(label = "Import", section = EditorSection.IMPORT)
                        AnimatedVisibility(visible = EditorSection.IMPORT in expanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ActionRow(
                                    listOf(
                                        "Upload" to {
                                            pendingImport = ImportType.IMAGE
                                            mediaPicker.launch("image/*")
                                        },
                                        "Paste" to { pasteImage() },
                                        "Text" to { showTextDialog = true },
                                    ),
                                )
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                ActionRow(
                                    listOf(
                                        "Emoji" to { showEmojiDialog = true },
                                        "GIF" to {
                                            pendingImport = ImportType.GIF
                                            mediaPicker.launch("image/gif")
                                        },
                                        "Video" to {
                                            pendingImport = ImportType.VIDEO
                                            mediaPicker.launch("video/*")
                                        },
                                    ),
                                )
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                GlyphPill(
                                    label = if (classic) "Raw data" else "RAW DATA",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { importRawJson() },
                                )
                                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                                GlyphPill(
                                    label = if (classic) "Open Glyph Museum" else "OPEN GLYPH MUSEUM",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { openGlyphMuseum(context) },
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
                        label = if (classic) "Design name" else "DESIGN NAME",
                        placeholder = "my_glyph",
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        NothingPillButton(
                            text = "Save",
                            onClick = { saveCurrent() },
                            modifier = Modifier.weight(1f),
                        )
                        NothingPillButton(
                            text = "Show",
                            onClick = { showCurrent() },
                            modifier = Modifier.weight(1f),
                        )
                        NothingPillButton(
                            text = "Export",
                            onClick = { shareCurrent() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                }

                item {
                    NothingCard {
                        SectionHeader(label = "Saved designs", section = EditorSection.SAVED)
                        AnimatedVisibility(visible = EditorSection.SAVED in expanded) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (savedNames.isEmpty()) {
                                    Text(
                                        text = if (classic) "No saved designs yet." else "NO SAVED DESIGNS YET.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = NothingFonts.mono(),
                                    )
                                } else {
                                    savedNames.forEachIndexed { index, entry ->
                                        if (index > 0) NothingDivider()
                                        val author = remember(entry) { viewModel.author(entry) }
                                        NothingListRow(
                                            title = entry,
                                            subtitle = author?.let { "by @$it" } ?: "",
                                            onClick = {
                                                viewModel.designJson(entry)?.let { json ->
                                                    runCatching { GlyphFrameCodec.decode(json) }
                                                        .getOrNull()
                                                        ?.let { design ->
                                                            loadDesign(design)
                                                            showCurrent()
                                                        }
                                                }
                                            },
                                            trailing = {
                                                Row {
                                                    Text(
                                                        text = "SHARE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontFamily = NothingFonts.mono(),
                                                        modifier =
                                                            Modifier
                                                                .clip(NothingShapes.input)
                                                                .clickable {
                                                                    viewModel.clearShareResult()
                                                                    shareDesign = entry
                                                                }.padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
                                                    )
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
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    NothingCard {
                        SectionHeader(label = "Library", section = EditorSection.LIBRARY)
                        AnimatedVisibility(visible = EditorSection.LIBRARY in expanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                            ) {
                                GlyphPill(
                                    label = if (classic) "Open Glyph Museum" else "OPEN GLYPH MUSEUM",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { openGlyphMuseum(context) },
                                )
                                if (library.isNotEmpty() || glyphSearch.isNotBlank()) {
                                    com.tdvorak.nothingmodes.ui.theme.NothingInput(
                                        value = glyphSearch,
                                        onValueChange = { glyphSearch = it },
                                        label = "Search",
                                        placeholder = "Find a design",
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        listOf(
                                            "newest" to "Newest",
                                            "download" to "Most downloaded",
                                            "alpha" to "A-Z",
                                        ).forEach { (key, label) ->
                                            NothingTag(
                                                text = label,
                                                active = glyphSort == key,
                                                onClick = { glyphSort = key },
                                            )
                                        }
                                    }
                                    if (allGlyphCaps.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            allGlyphCaps.forEach { cap ->
                                                NothingTag(
                                                    text = cap.replace("_", " "),
                                                    active = glyphCaps.contains(cap),
                                                    onClick = {
                                                        glyphCaps =
                                                            if (glyphCaps.contains(cap)) glyphCaps - cap else glyphCaps + cap
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    if (visibleLibrary.isEmpty()) {
                                        Text(
                                            text = "No matching designs.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = NothingFonts.mono(),
                                        )
                                    } else {
                                        visibleLibrary.forEachIndexed { index, item ->
                                            if (index > 0) NothingDivider()
                                            GlyphCommunityRow(
                                                item = item,
                                                onClick = { selectedLibraryItem = item },
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No designs downloaded. Open Glyph Museum to find more.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = NothingFonts.mono(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        shareDesign?.let { designName ->
            ModalBottomSheet(
                onDismissRequest = { shareDesign = null },
            ) {
                ShareSheet(
                    initialTitle = designName,
                    initialDescription = "",
                    profile = viewModel.creatorProfile.get(),
                    sharing = sharing,
                    result = shareResult,
                    onPublish = { t, d, h, e, g ->
                        viewModel.publishGlyph(designName, t, d, h, e, g)
                    },
                    onDismiss = { shareDesign = null },
                )
            }
        }

        selectedLibraryItem?.let { item ->
            val design = selectedLibraryDesign
            LaunchedEffect(item) {
                selectedLibraryDesign = null
                runCatching {
                    val payload =
                        com.tdvorak.nothingmodes.data.community.CommunityApi
                            .fetchItem(item.id)
                    GlyphFrameCodec.decode(payload.toString())
                }.getOrNull()?.let { selectedLibraryDesign = it }
            }
            ModalBottomSheet(
                onDismissRequest = {
                    selectedLibraryItem = null
                    selectedLibraryDesign = null
                },
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(NothingSpacing.lg)
                            .navigationBarsPadding(),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "by @${item.handle}${if (item.summary.isNotBlank()) " · ${item.summary}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    if (design != null) {
                        AnimatedGlyphCanvas(
                            design = design,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(NothingShapes.input)
                                    .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "LOADING PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton(
                        text = "Import",
                        enabled = !importing,
                        onClick = {
                            importing = true
                            scope.launch {
                                val stored = viewModel.importLibraryItem(item)
                                importing = false
                                Toast
                                    .makeText(
                                        context,
                                        if (stored != null) "Imported as $stored" else "Import failed",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                refreshNames()
                                selectedLibraryItem = null
                                selectedLibraryDesign = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasCard(
    gridSize: Int,
    frame: IntArray,
    paintMode: PaintMode,
    brush: Int,
    onTap: (Int, Int) -> Unit,
    onDragStart: (Int, Int) -> Boolean,
    onDrag: (Int, Int, Boolean) -> Unit,
) {
    NothingCard(borderless = true) {
        // 70% of the width — leaves more room for the scrollable tools below.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.55f)
                        .aspectRatio(1f)
                        .clip(NothingShapes.input)
                        .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.input)
                        .background(Color.Black)
                        .pointerInput(gridSize, paintMode, brush) {
                            detectTapGestures { offset ->
                                cellFromOffset(offset, size.width.toFloat(), gridSize)?.let { (r, c) ->
                                    onTap(r, c)
                                }
                            }
                        }.pointerInput(gridSize, paintMode, brush) {
                            var painting: Boolean? = null
                            detectDragGestures(
                                onDragStart = { offset ->
                                    cellFromOffset(offset, size.width.toFloat(), gridSize)?.let { (r, c) ->
                                        painting = onDragStart(r, c)
                                    }
                                },
                                onDragEnd = { painting = null },
                                onDragCancel = { painting = null },
                                onDrag = { change, _ ->
                                    change.consume()
                                    cellFromOffset(change.position, size.width.toFloat(), gridSize)?.let { (r, c) ->
                                        painting?.let { onDrag(r, c, it) }
                                    }
                                },
                            )
                        },
            ) {
                GlyphCanvas(gridSize, frame)
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(NothingSpacing.sm),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "(0, 0)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontFamily = NothingFonts.mono(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "(${gridSize - 1}, ${gridSize - 1})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }
            }
        }
    }
}

private fun cellFromOffset(
    offset: Offset,
    canvasSize: Float,
    gridSize: Int,
): Pair<Int, Int>? {
    if (canvasSize <= 0f) return null
    val cellW = canvasSize / gridSize.toFloat()
    val col = (offset.x / cellW).toInt().coerceIn(0, gridSize - 1)
    val row = (offset.y / cellW).toInt().coerceIn(0, gridSize - 1)
    return row to col
}

@Composable
internal fun GlyphCanvas(
    gridSize: Int,
    pixels: IntArray,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasSize = size.width
        val cellW = canvasSize / gridSize.toFloat()
        val widths = GlyphFrameCodec.rowWidths(gridSize)
        for (row in 0 until gridSize) {
            val start = (gridSize - widths[row]) / 2
            for (col in 0 until gridSize) {
                val inside = col in start until start + widths[row]
                val idx = row * gridSize + col
                val cx = col * cellW + cellW / 2f
                val cy = row * cellW + cellW / 2f
                val color =
                    when {
                        !inside -> onSurface.copy(alpha = 0.08f)
                        pixels[idx] > 0 -> primary.copy(alpha = (pixels[idx] / 4095f).coerceIn(0f, 1f))
                        else -> onSurface.copy(alpha = 0.22f)
                    }
                // Squares — the matrix LEDs are square cells, not dots.
                val inset = cellW * 0.12f
                drawRect(
                    color = color,
                    topLeft = Offset(cx - cellW / 2f + inset, cy - cellW / 2f + inset),
                    size =
                        androidx.compose.ui.geometry
                            .Size(cellW - inset * 2, cellW - inset * 2),
                )
            }
        }
    }
}

@Composable
private fun FrameChip(
    number: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Box(
        modifier =
            Modifier
                .size(width = 40.dp, height = 36.dp)
                .clip(NothingShapes.input)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            fontFamily = NothingFonts.mono(),
        )
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        NothingRadio(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = NothingFonts.mono(),
        )
    }
}

@Composable
private fun ActionRow(actions: List<Pair<String, () -> Unit>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        actions.forEach { (label, onClick) ->
            GlyphPill(
                label = label,
                modifier = Modifier.weight(1f),
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun GlyphPill(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    val bg =
        if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val fg =
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Box(
        modifier =
            modifier
                .clip(NothingShapes.pill)
                .background(bg)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = NothingSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (classic) label else label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontFamily = NothingFonts.mono(),
        )
    }
}

@Composable
private fun TextImportDialog(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = label) },
        text = {
            NothingInput(
                value = value,
                onValueChange = onValueChange,
                label = label,
                placeholder = label,
            )
        },
        confirmButton = {
            Text(
                text = "Import",
                modifier = Modifier.clickable { onConfirm() }.padding(NothingSpacing.md),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = NothingFonts.mono(),
            )
        },
        dismissButton = {
            Text(
                text = "Cancel",
                modifier = Modifier.clickable { onDismiss() }.padding(NothingSpacing.md),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        },
    )
}

private const val GLYPH_MUSEUM_PKG = "com.pauwma.glyphmuseum"

private fun openGlyphMuseum(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(GLYPH_MUSEUM_PKG)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        val store =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$GLYPH_MUSEUM_PKG")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(store)
    }
}

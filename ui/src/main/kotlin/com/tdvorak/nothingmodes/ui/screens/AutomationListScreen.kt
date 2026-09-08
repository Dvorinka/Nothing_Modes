package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.automation.widget.WidgetRefreshHelper
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CapabilityLabels
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.data.dao.ScheduledTimeAlarmDao
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider
import com.tdvorak.nothingmodes.engine.runtime.ImportResult
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingAddCircle
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDotGrid
import com.tdvorak.nothingmodes.ui.theme.ModeDotRow
import com.tdvorak.nothingmodes.ui.theme.ModeDotState
import com.tdvorak.nothingmodes.ui.theme.NothingDotRow
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingGhostButton
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingScreenHero
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTag
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AutomationListViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: android.content.Context,
        private val store: AutomationStore,
        private val modeActivationProvider: ModeActivationProvider,
        private val scheduledTimeAlarmDao: ScheduledTimeAlarmDao,
    ) : ViewModel() {
        private val _items = MutableStateFlow<List<Automation>>(emptyList())
        val items: StateFlow<List<Automation>> = _items.asStateFlow()

        private val _loading = MutableStateFlow(true)
        val loading: StateFlow<Boolean> = _loading.asStateFlow()

        private val _selected = MutableStateFlow<Set<AutomationId>>(emptySet())
        val selected: StateFlow<Set<AutomationId>> = _selected.asStateFlow()

        private val _activeIds = MutableStateFlow<Set<String>>(emptySet())
        val activeIds: StateFlow<Set<String>> = _activeIds.asStateFlow()

        private val _nextFires = MutableStateFlow<Map<String, Long>>(emptyMap())
        val nextFires: StateFlow<Map<String, Long>> = _nextFires.asStateFlow()

        private val _importResult = MutableStateFlow<ImportResult?>(null)
        val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

        private val _importWarnings = MutableStateFlow<List<String>>(emptyList())
        val importWarnings: StateFlow<List<String>> = _importWarnings.asStateFlow()

        private val importExportService =
            ImportExportService(
                store,
                appVersion =
                    runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }.getOrNull().orEmpty(),
            )

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                try {
                    val all = store.all().sortedBy { it.priority }
                    _items.value = all
                    _activeIds.value = modeActivationProvider.activeModeIds().toSet()
                    _nextFires.value =
                        all
                            .mapNotNull { automation ->
                                val next = scheduledTimeAlarmDao.getNextStart(automation.id.value)
                                next?.let { automation.id.value to it.wakeAtMillis }
                            }.toMap()
                } finally {
                    _loading.value = false
                }
            }
        }

        fun runNow(automation: Automation) {
            val intent =
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_MANUAL
                    putExtra(AutomationService.EXTRA_MANUAL_ID, automation.id.value)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        fun runNow(id: AutomationId) {
            _items.value.find { it.id == id }?.let { runNow(it) }
        }

        fun toggleEnabled(automation: Automation) {
            viewModelScope.launch {
                val updated = automation.copy(enabled = !automation.enabled)
                store.save(updated)
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun delete(automation: Automation) {
            viewModelScope.launch {
                store.delete(automation.id)
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun duplicate(automation: Automation) {
            viewModelScope.launch {
                val copy =
                    automation.copy(
                        id = AutomationId("${automation.id.value}-copy-${System.currentTimeMillis()}"),
                        name = "${automation.name} (copy)",
                        enabled = false,
                    )
                store.save(copy)
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun moveUp(automation: Automation) {
            viewModelScope.launch {
                val list = _items.value.toMutableList()
                val index = list.indexOfFirst { it.id == automation.id }
                if (index <= 0) return@launch
                val prev = list[index - 1]
                val current = list[index]
                store.save(prev.copy(priority = current.priority))
                store.save(current.copy(priority = prev.priority))
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun moveDown(automation: Automation) {
            viewModelScope.launch {
                val list = _items.value.toMutableList()
                val index = list.indexOfFirst { it.id == automation.id }
                if (index < 0 || index >= list.size - 1) return@launch
                val next = list[index + 1]
                val current = list[index]
                store.save(next.copy(priority = current.priority))
                store.save(current.copy(priority = next.priority))
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        // ── Selection mode ─────────────────────────────────────────────────────────

        fun toggleSelected(id: AutomationId) {
            _selected.value = if (_selected.value.contains(id)) _selected.value - id else _selected.value + id
        }

        fun selectAll() {
            _selected.value = _items.value.map { it.id }.toSet()
        }

        fun clearSelection() {
            _selected.value = emptySet()
        }

        fun deleteSelected() {
            viewModelScope.launch {
                _selected.value.forEach { store.delete(it) }
                _selected.value = emptySet()
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun enableSelected(enabled: Boolean) {
            viewModelScope.launch {
                _selected.value.forEach { id ->
                    val item = _items.value.find { it.id == id } ?: return@forEach
                    store.save(item.copy(enabled = enabled))
                }
                _selected.value = emptySet()
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun runSelected() {
            _selected.value.forEach { runNow(it) }
            _selected.value = emptySet()
        }

        fun import(json: String) {
            viewModelScope.launch {
                // Surface compatibility warnings before the write, not after.
                val preview = importExportService.preview(json)
                _importWarnings.value =
                    preview.requiredCapabilities
                        .mapNotNull { CapabilityLabels.describe(it).ifBlank { null } }
                        .distinct()
                _importResult.value = importExportService.import(json)
                load()
                WidgetRefreshHelper.refresh(context)
            }
        }

        fun readImportFromFile(uri: android.net.Uri) {
            viewModelScope.launch {
                val json =
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readBytes().toString(Charsets.UTF_8)
                        }
                    } ?: return@launch
                import(json)
            }
        }

        fun clearImportResult() {
            _importResult.value = null
            _importWarnings.value = emptyList()
        }
    }

@Composable
fun AutomationListScreen(
    onAutomationClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogClick: () -> Unit,
    onCreateClick: () -> Unit = {},
    onTemplatesClick: () -> Unit = {},
    viewModel: AutomationListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val activeIds by viewModel.activeIds.collectAsState()
    val nextFires by viewModel.nextFires.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val importWarnings by viewModel.importWarnings.collectAsState()
    val inSelection = selected.isNotEmpty()

    // Type filter chips (All / Modes / Routines) — display-only, client-side.
    var typeFilter by rememberSaveable { mutableStateOf("ALL") }
    val visibleItems =
        remember(items, typeFilter) {
            if (typeFilter == "ALL") items else items.filter { it.type.name == typeFilter }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let { viewModel.readImportFromFile(it) }
        }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Reload on resume so changes from builder/detail are reflected immediately.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.load()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = if (inSelection) "${selected.size} SELECTED" else "Modes",
                showLeadingDot = !inSelection,
                actions =
                    if (inSelection) {
                        listOf(TopBarAction("CLOSE") { viewModel.clearSelection() })
                    } else {
                        listOf(
                            TopBarAction("TEMPLATES", icon = Icons.Outlined.GridView, onClick = onTemplatesClick),
                            TopBarAction(
                                "IMPORT",
                                icon = Icons.Outlined.FileUpload,
                                onClick = { importLauncher.launch(arrayOf("application/json")) },
                            ),
                            TopBarAction("LOG", icon = Icons.AutoMirrored.Outlined.List, onClick = onLogClick),
                            TopBarAction("SETTINGS", icon = Icons.Outlined.Settings, onClick = onSettingsClick),
                        )
                    },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            NothingDotGrid(
                modifier = Modifier.fillMaxSize(),
            )

            if (loading) {
                // Blank while the first load is in flight — prevents the
                // "Automations" hero flashing before the empty state appears.
                Box(modifier = Modifier.fillMaxSize())
            } else if (items.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = NothingSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    NothingEmptyState(
                        title = "No routines yet",
                        description = "Tap + to create your first mode or routine",
                        action = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                NothingGhostButton(
                                    text = "Import from backup",
                                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                                )
                                NothingGhostButton(
                                    text = "Browse templates",
                                    onClick = onTemplatesClick,
                                )
                            }
                        },
                    )
                    importResult?.let { result ->
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Text(
                            text = "Imported: ${result.imported}  Skipped: ${result.skipped}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = NothingFonts.mono(),
                        )
                        result.errors.forEach { error ->
                            Text(
                                text = "[ ERROR: $error ]",
                                style = MaterialTheme.typography.labelSmall,
                                color = NothingColors.accent,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                        importWarnings.forEach { warning ->
                            Text(
                                text = "[ REQUIRES: $warning ]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                        NothingGhostButton(
                            text = "Dismiss",
                            onClick = { viewModel.clearImportResult() },
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = NothingSpacing.md,
                            end = NothingSpacing.md,
                            top = NothingSpacing.lg,
                            bottom = 96.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.padding(bottom = NothingSpacing.sm)) {
                            NothingScreenHero(
                                title = "Automations",
                                caption = "${visibleItems.size} items",
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                            ) {
                                NothingTag(
                                    text = "All",
                                    active = typeFilter == "ALL",
                                    onClick = { typeFilter = "ALL" },
                                )
                                NothingTag(
                                    text = "Modes",
                                    active = typeFilter == "MODE",
                                    onClick = { typeFilter = "MODE" },
                                )
                                NothingTag(
                                    text = "Routines",
                                    active = typeFilter == "ROUTINE",
                                    onClick = { typeFilter = "ROUTINE" },
                                )
                            }
                            Spacer(modifier = Modifier.height(NothingSpacing.lg))
                            val dotStates =
                                remember(visibleItems, activeIds, nextFires) {
                                    val now = System.currentTimeMillis()
                                    visibleItems.map { automation ->
                                        when {
                                            activeIds.contains(automation.id.value) -> ModeDotState.ACTIVE
                                            isFiringSoon(automation, nextFires[automation.id.value], now) -> ModeDotState.FIRING_SOON
                                            automation.enabled -> ModeDotState.ENABLED
                                            else -> ModeDotState.DISABLED
                                        }
                                    }
                                }
                            RoutineHeroCard(
                                totalRoutines = visibleItems.size,
                                totalActions = visibleItems.sumOf { it.actions.size },
                                dotStates = dotStates,
                            )
                        }
                    }

                    items(visibleItems, key = { it.id.value }) { automation ->
                        val isActive = activeIds.contains(automation.id.value)
                        val nextFireAt = nextFires[automation.id.value]
                        RoutineTile(
                            automation = automation,
                            isSelected = selected.contains(automation.id),
                            isActive = isActive,
                            isFiringSoon = !isActive && isFiringSoon(automation, nextFireAt, System.currentTimeMillis()),
                            nextFireAt = nextFireAt,
                            inSelectionMode = inSelection,
                            onClick = { onAutomationClick(automation.id.value) },
                            onToggleSelection = { viewModel.toggleSelected(automation.id) },
                            onToggleEnabled = { viewModel.toggleEnabled(automation) },
                            onRun = { viewModel.runNow(automation) },
                        )
                    }
                }
            }

            if (inSelection) {
                MultiSelectBottomBar(
                    onSelectAll = viewModel::selectAll,
                    onDelete = viewModel::deleteSelected,
                    onRun = viewModel::runSelected,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                )
            } else {
                NothingAddCircle(
                    onClick = onCreateClick,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(NothingSpacing.lg),
                )
            }
        }
    }
}

@Composable
private fun RoutineHeroCard(
    totalRoutines: Int,
    totalActions: Int,
    dotStates: List<ModeDotState>,
    modifier: Modifier = Modifier,
) {
    NothingCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                NothingLabel(text = "Overview")
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Text(
                    text = "$totalRoutines",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.doto(),
                )
                Text(
                    text = "ROUTINES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalActions",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.doto(),
                )
                Text(
                    text = "ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        ModeDotRow(states = dotStates)
    }
}

@Composable
private fun RoutineTile(
    automation: Automation,
    isSelected: Boolean,
    isActive: Boolean,
    isFiringSoon: Boolean,
    nextFireAt: Long?,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onToggleEnabled: () -> Unit,
    onRun: () -> Unit = {},
) {
    val iconTextColor = MaterialTheme.colorScheme.onSurface
    val borderColor =
        when {
            isActive -> MaterialTheme.colorScheme.primary
            isFiringSoon -> MaterialTheme.colorScheme.primary
            isSelected -> NothingColors.accent
            else -> MaterialTheme.colorScheme.outlineVariant
        }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.card,
        border = BorderStroke(1.dp, borderColor),
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (inSelectionMode) onToggleSelection() else onClick() },
                    onLongClick = onToggleSelection,
                ),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NothingIconCircle(size = 44f) {
                    if (automation.icon.isNotBlank()) {
                        Icon(
                            imageVector = iconForName(automation.icon),
                            contentDescription = null,
                            tint = iconTextColor,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text(
                            text =
                                automation.name
                                    .firstOrNull()
                                    ?.uppercaseChar()
                                    ?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = iconTextColor,
                            fontFamily = GeistSans,
                        )
                    }
                }

                if (isActive) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.mono(),
                        letterSpacing = 1.0.sp,
                        modifier = Modifier.padding(end = NothingSpacing.sm),
                    )
                } else if (isFiringSoon && nextFireAt != null) {
                    val minutes = ((nextFireAt - System.currentTimeMillis()) / 60_000).toInt().coerceAtLeast(1)
                    Text(
                        text = "IN $minutes MIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.mono(),
                        letterSpacing = 1.0.sp,
                        modifier = Modifier.padding(end = NothingSpacing.sm),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (inSelectionMode) {
                    SelectionIndicator(isSelected = isSelected)
                } else if (automation.trigger is Trigger.Manual) {
                    Text(
                        text = "RUN",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingColors.accent,
                        fontFamily = NothingFonts.mono(),
                        letterSpacing = 1.0.sp,
                        modifier =
                            Modifier
                                .clickable(onClick = onRun)
                                .padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.xs),
                    )
                } else {
                    NothingToggle(
                        checked = automation.enabled,
                        onCheckedChange = { onToggleEnabled() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.md))

            Text(
                text = automation.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            NothingLabel(
                text = if (automation.type == AutomationType.MODE) "Mode" else "Routine",
                modifier = Modifier.padding(top = NothingSpacing.xs),
            )

            if (automation.status == AutomationStatus.PENDING_APPROVAL) {
                NothingLabel(
                    text = "Pending Approval",
                    color = NothingColors.accent,
                    modifier = Modifier.padding(top = NothingSpacing.xs),
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.md))

            Text(
                text = triggerDescription(automation.trigger),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${automation.actions.size} actions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val FIRING_SOON_THRESHOLD_MS = 30 * 60 * 1000L

private fun isFiringSoon(
    automation: Automation,
    nextFireAt: Long?,
    now: Long,
): Boolean {
    if (nextFireAt == null || nextFireAt <= now) return false
    if (automation.trigger !is Trigger.Time && automation.trigger !is Trigger.TimeWindow) return false
    return nextFireAt - now <= FIRING_SOON_THRESHOLD_MS
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    if (isSelected) {
        Text(
            text = "●",
            style = MaterialTheme.typography.titleMedium,
            color = NothingColors.accent,
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

// ── Multi-Select Bottom Action Bar ───────────────────────────────────────────

@Composable
private fun MultiSelectBottomBar(
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.cardLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = NothingSpacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ALL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = NothingFonts.mono(),
                letterSpacing = 1.0.sp,
                modifier =
                    Modifier
                        .clickable(onClick = onSelectAll)
                        .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
            )
            Text(
                text = "DELETE",
                style = MaterialTheme.typography.labelLarge,
                color = NothingColors.accent,
                fontFamily = NothingFonts.mono(),
                letterSpacing = 1.0.sp,
                modifier =
                    Modifier
                        .clickable(onClick = onDelete)
                        .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
            )
            Text(
                text = "RUN",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = NothingFonts.mono(),
                letterSpacing = 1.0.sp,
                modifier =
                    Modifier
                        .clickable(onClick = onRun)
                        .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
            )
        }
    }
}

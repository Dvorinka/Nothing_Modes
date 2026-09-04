package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.automation.widget.WidgetRefreshHelper
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.screens.triggerDescription
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingAddCircle
import com.tdvorak.nothingmodes.ui.theme.NothingCircleButton
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDotGrid
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AutomationListViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val store: AutomationStore,
) : ViewModel() {

    private val _items = MutableStateFlow<List<Automation>>(emptyList())
    val items: StateFlow<List<Automation>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selected = MutableStateFlow<Set<AutomationId>>(emptySet())
    val selected: StateFlow<Set<AutomationId>> = _selected.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = store.all().sortedBy { it.priority }
            _loading.value = false
        }
    }

    fun runNow(automation: Automation) {
        val intent = Intent(context, AutomationService::class.java).apply {
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
            val copy = automation.copy(
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
        _selected.value.firstOrNull()?.let { runNow(it) }
        _selected.value = emptySet()
    }
}

@Composable
fun AutomationListScreen(
    onAutomationClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogClick: () -> Unit,
    onCreateClick: () -> Unit = {},
    viewModel: AutomationListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val inSelection = selected.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = if (inSelection) "${selected.size} SELECTED" else "Nothing Modes",
                showLeadingDot = !inSelection,
                actions = if (inSelection) {
                    emptyList()
                } else {
                    listOf(
                        TopBarAction("LOG", onLogClick),
                        TopBarAction("SETTINGS", onSettingsClick),
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NothingDotGrid(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.04f,
            )

            if (items.isEmpty() && !loading) {
                NothingEmptyState(
                    title = "No routines yet",
                    description = "Tap + to create your first mode or routine",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = NothingSpacing.md,
                        end = NothingSpacing.md,
                        top = NothingSpacing.lg,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                ) {
                    items(items, key = { it.id.value }) { automation ->
                        RoutineCard(
                            automation = automation,
                            isSelected = selected.contains(automation.id),
                            inSelectionMode = inSelection,
                            onClick = { onAutomationClick(automation.id.value) },
                            onToggleSelection = { viewModel.toggleSelected(automation.id) },
                            onToggleEnabled = { viewModel.toggleEnabled(automation) },
                        )
                    }
                }
            }

            if (inSelection) {
                MultiSelectBottomBar(
                    onSelectAll = viewModel::selectAll,
                    onEnable = { viewModel.enableSelected(true) },
                    onDisable = { viewModel.enableSelected(false) },
                    onDelete = viewModel::deleteSelected,
                    onRun = viewModel::runSelected,
                    onClear = viewModel::clearSelection,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            } else {
                NothingAddCircle(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(NothingSpacing.lg),
                )
            }
        }
    }
}

@Composable
private fun RoutineCard(
    automation: Automation,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onToggleEnabled: () -> Unit,
) {
    val iconColor = if (automation.iconBackground.isNotBlank()) colorForHex(automation.iconBackground) else routineColor(automation.name)
    val iconTextColor = if (iconColor.luminance() > 0.5f) Color.Black else Color.White
    val borderColor = if (isSelected) NothingColors.accent else MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.card,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (automation.icon.isNotBlank()) {
                        Icon(
                            imageVector = iconForName(automation.icon),
                            contentDescription = null,
                            tint = iconTextColor,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Text(
                            text = automation.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = iconTextColor,
                            fontFamily = Doto,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (inSelectionMode) {
                    SelectionIndicator(isSelected = isSelected)
                } else {
                    NothingToggle(
                        checked = automation.enabled,
                        onCheckedChange = { onToggleEnabled() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.sm))

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

            Spacer(modifier = Modifier.height(NothingSpacing.sm))

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
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

private val routineColors = listOf(
    Color(0xFF4A9E5C),
    Color(0xFFD4A843),
    Color(0xFF5B9BF6),
    Color(0xFFD71921),
    Color(0xFF9B59B6),
    Color(0xFF1ABC9C),
)

private fun routineColor(name: String): Color {
    val index = name.hashCode().rem(routineColors.size).let {
        if (it < 0) it + routineColors.size else it
    }
    return routineColors[index]
}

// ── Multi-Select Bottom Action Bar ───────────────────────────────────────────

@Composable
private fun MultiSelectBottomBar(
    onSelectAll: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit,
    onRun: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.cardLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NothingSpacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            NothingCircleButton(icon = "A", label = "All", onClick = onSelectAll)
            NothingCircleButton(icon = "1", label = "On", onClick = onEnable)
            NothingCircleButton(icon = "0", label = "Off", onClick = onDisable)
            NothingCircleButton(icon = "D", label = "Del", onClick = onDelete, color = NothingColors.accent)
            NothingCircleButton(icon = "R", label = "Run", onClick = onRun, color = NothingColors.success)
            NothingCircleButton(icon = "X", label = "X", onClick = onClear)
        }
    }
}
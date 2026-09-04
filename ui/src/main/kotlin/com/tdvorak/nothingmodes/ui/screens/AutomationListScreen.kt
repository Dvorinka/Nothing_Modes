package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingDotGrid
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingRedDot
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

    init { load() }

    fun runNow(automation: Automation) {
        val intent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_MANUAL
            putExtra(AutomationService.EXTRA_MANUAL_ID, automation.id.value)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = store.all().sortedBy { it.priority }
            _loading.value = false
        }
    }

    fun toggleEnabled(automation: Automation) {
        viewModelScope.launch {
            val updated = automation.copy(enabled = !automation.enabled)
            store.save(updated)
            load()
        }
    }

    fun delete(automation: Automation) {
        viewModelScope.launch {
            store.delete(automation.id)
            load()
        }
    }

    fun duplicate(automation: Automation) {
        viewModelScope.launch {
            val copy = automation.copy(
                id = com.tdvorak.nothingmodes.engine.model.AutomationId(
                    "${automation.id.value}-copy-${System.currentTimeMillis()}"
                ),
                name = "${automation.name} (copy)",
                enabled = false,
            )
            store.save(copy)
            load()
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
        }
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
    val loading by viewModel.loading.collectAsState()

    val enabled = items.filter { it.enabled }
    val disabled = items.filter { !it.enabled }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Nothing Modes",
                actions = listOf(
                    TopBarAction("LOG", onLogClick),
                    TopBarAction("SETTINGS", onSettingsClick),
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Subtle dot-grid background
            NothingDotGrid(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.04f,
            )

            if (items.isEmpty() && !loading) {
                NothingEmptyState(
                    title = "No modes yet",
                    description = "Create your first automation to get started",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = NothingSpacing.md,
                        end = NothingSpacing.md,
                        top = NothingSpacing.lg,
                        bottom = 120.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    // Active section
                    if (enabled.isNotEmpty()) {
                        item {
                            NothingLabel(
                                text = "Active",
                                modifier = Modifier.padding(
                                    bottom = NothingSpacing.sm,
                                    top = NothingSpacing.sm,
                                ),
                            )
                        }
                        items(enabled) { automation ->
                            AutomationRow(
                                automation = automation,
                                isActive = true,
                                onClick = { onAutomationClick(automation.id.value) },
                                onToggle = { viewModel.toggleEnabled(automation) },
                                onDelete = { viewModel.delete(automation) },
                                onMoveUp = { viewModel.moveUp(automation) },
                                onMoveDown = { viewModel.moveDown(automation) },
                                onRunNow = { viewModel.runNow(automation) },
                            )
                        }
                    }

                    // Inactive section — vast gap = new context
                    if (disabled.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(NothingSpacing.xxl))
                            NothingLabel(
                                text = "Inactive",
                                modifier = Modifier.padding(bottom = NothingSpacing.sm),
                            )
                        }
                        items(disabled) { automation ->
                            AutomationRow(
                                automation = automation,
                                isActive = false,
                                onClick = { onAutomationClick(automation.id.value) },
                                onToggle = { viewModel.toggleEnabled(automation) },
                                onDelete = { viewModel.delete(automation) },
                                onMoveUp = { viewModel.moveUp(automation) },
                                onMoveDown = { viewModel.moveDown(automation) },
                                onRunNow = { viewModel.runNow(automation) },
                            )
                        }
                    }
                }
            }

            // Pill button pinned bottom — replaces FAB
            NothingPillButton(
                text = "+ New Mode",
                onClick = onCreateClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = NothingSpacing.lg),
            )
        }
    }
}

@Composable
private fun AutomationRow(
    automation: Automation,
    isActive: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onRunNow: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Red accent dot for active automations
        if (isActive) {
            NothingRedDot(
                size = 6f,
                modifier = Modifier.padding(end = NothingSpacing.sm),
            )
        } else {
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Name — display size for active, title for inactive (hierarchy)
            Text(
                text = automation.name,
                style = if (isActive) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.titleMedium,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = if (isActive) Doto else null,
            )
            // Type label — Space Mono ALL CAPS
            NothingLabel(
                text = when (automation.type) {
                    AutomationType.MODE -> "Mode"
                    AutomationType.ROUTINE -> "Routine"
                },
                modifier = Modifier.padding(top = 2.dp),
            )
            if (automation.status == AutomationStatus.PENDING_APPROVAL) {
                NothingLabel(
                    text = "Pending Approval",
                    color = NothingColors.accent,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Run now for manual triggers
        if (automation.trigger is Trigger.Manual) {
            Text(
                text = "RUN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onRunNow)
                    .padding(horizontal = NothingSpacing.sm),
            )
        }

        // Toggle
        NothingToggle(
            checked = automation.enabled,
            onCheckedChange = { onToggle() },
        )
    }
}

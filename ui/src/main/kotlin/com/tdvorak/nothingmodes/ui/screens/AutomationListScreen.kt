package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AutomationListViewModel @Inject constructor(
    private val store: AutomationStore,
) : ViewModel() {

    private val _items = MutableStateFlow<List<Automation>>(emptyList())
    val items: StateFlow<List<Automation>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _items.value = store.all()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationListScreen(
    onAutomationClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLogClick: () -> Unit,
    viewModel: AutomationListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nothing Modes") },
                actions = {
                    IconButton(onClick = onLogClick) {
                        Icon(Icons.Default.History, contentDescription = "Execution Log")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: create new automation */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
    ) { padding ->
        if (items.isEmpty() && !loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "No modes or routines yet",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Tap + to create your first automation",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items) { automation ->
                    AutomationCard(
                        automation = automation,
                        onClick = { onAutomationClick(automation.id.value) },
                        onToggle = { viewModel.toggleEnabled(automation) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationCard(
    automation: Automation,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(automation.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    when (automation.type) {
                        AutomationType.MODE -> "Mode"
                        AutomationType.ROUTINE -> "Routine"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (automation.status == AutomationStatus.PENDING_APPROVAL) {
                    Text(
                        "Pending approval",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Switch(
                checked = automation.enabled,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuditEntry(
    val automationId: String,
    val kind: String,
    val timestamp: Long,
    val detail: String,
)

data class ExecutionStats(
    val totalEvents: Int,
    val firedCount: Int,
    val modeActivatedCount: Int,
    val modeDeactivatedCount: Int,
    val suppressedCount: Int,
    val conditionsNotMetCount: Int,
    val errorCount: Int,
    val successRate: Float,
)

@HiltViewModel
class ExecutionLogViewModel @Inject constructor(
    db: NothingModesDatabase,
) : ViewModel() {

    private val _entries = MutableStateFlow<List<AuditEntry>>(emptyList())
    val entries: StateFlow<List<AuditEntry>> = _entries.asStateFlow()

    private val _stats = MutableStateFlow(ExecutionStats(0, 0, 0, 0, 0, 0, 0, 0f))
    val stats: StateFlow<ExecutionStats> = _stats.asStateFlow()

    private val auditDao = db.auditDao()

    init {
        viewModelScope.launch {
            auditDao.observeRecent(50).collect { entities ->
                _entries.value = entities.map { entity ->
                    AuditEntry(
                        automationId = entity.automationId,
                        kind = entity.kind,
                        timestamp = entity.atMillis,
                        detail = entity.detail,
                    )
                }
                _stats.value = computeStats(_entries.value)
            }
        }
    }

    private fun computeStats(entries: List<AuditEntry>): ExecutionStats {
        val total = entries.size
        val fired = entries.count { it.kind == "FIRED" }
        val activated = entries.count { it.kind == "MODE_ACTIVATED" }
        val deactivated = entries.count { it.kind == "MODE_DEACTIVATED" }
        val suppressed = entries.count { it.kind == "SUPPRESSED_COOLDOWN" }
        val notMet = entries.count { it.kind == "CONDITIONS_NOT_MET" }
        val errors = entries.count { it.kind == "ERROR" }
        val successful = fired + activated + deactivated
        val rate = if (total > 0) successful.toFloat() / total else 0f
        return ExecutionStats(total, fired, activated, deactivated, suppressed, notMet, errors, rate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionLogScreen(
    onBack: () -> Unit,
    viewModel: ExecutionLogViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Execution Timeline") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No executions yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { StatsCard(stats) }
                items(entries) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    entry.kind.replace("_", " "),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    dateFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                entry.automationId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (entry.detail.isNotEmpty()) {
                                Text(
                                    entry.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: ExecutionStats) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Statistics", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            StatRow("Total events", stats.totalEvents.toString())
            StatRow("Fired", stats.firedCount.toString())
            StatRow("Mode activated", stats.modeActivatedCount.toString())
            StatRow("Mode deactivated", stats.modeDeactivatedCount.toString())
            StatRow("Suppressed (cooldown)", stats.suppressedCount.toString())
            StatRow("Conditions not met", stats.conditionsNotMetCount.toString())
            StatRow("Errors", stats.errorCount.toString())
            HorizontalDivider()
            StatRow("Success rate", "%.1f%%".format(stats.successRate * 100))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

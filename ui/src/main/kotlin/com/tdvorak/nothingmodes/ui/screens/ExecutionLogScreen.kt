package com.tdvorak.nothingmodes.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingInfoRow
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSegmentedBar
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
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

@Composable
fun ExecutionLogScreen(
    onBack: () -> Unit,
    viewModel: ExecutionLogViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(title = "Execution Log", onBack = onBack)
        },
    ) { padding ->
        if (entries.isEmpty()) {
            NothingEmptyState(
                title = "No executions yet",
                description = "Automations will appear here when they fire",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = NothingSpacing.md,
                    end = NothingSpacing.md,
                    top = NothingSpacing.lg,
                    bottom = NothingSpacing.xxxl,
                ),
            ) {
                // Hero — title + success rate
                item {
                    Text(
                        text = "LOG",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = Doto,
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    Text(
                        text = "%.0f%%".format(stats.successRate * 100),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = Doto,
                    )
                    NothingLabel(text = "Success Rate")
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))

                    // Segmented bar — visual proportion
                    NothingSegmentedBar(
                        total = 20,
                        filled = (stats.successRate * 20).toInt().coerceIn(0, 20),
                        fillColor = if (stats.errorCount > 0) NothingColors.warning
                        else NothingColors.success,
                        height = 12f,
                    )
                }

                // Stats section
                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.xxl))
                    NothingSectionHeader(text = "Statistics")
                    NothingDivider()
                    NothingInfoRow(label = "Total Events", value = stats.totalEvents.toString())
                    NothingDivider()
                    NothingInfoRow(
                        label = "Fired",
                        value = stats.firedCount.toString(),
                        valueColor = NothingColors.success,
                    )
                    NothingDivider()
                    NothingInfoRow(label = "Mode Activated", value = stats.modeActivatedCount.toString())
                    NothingDivider()
                    NothingInfoRow(label = "Mode Deactivated", value = stats.modeDeactivatedCount.toString())
                    NothingDivider()
                    NothingInfoRow(
                        label = "Suppressed",
                        value = stats.suppressedCount.toString(),
                        valueColor = NothingColors.warning,
                    )
                    NothingDivider()
                    NothingInfoRow(
                        label = "Conditions Not Met",
                        value = stats.conditionsNotMetCount.toString(),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NothingDivider()
                    NothingInfoRow(
                        label = "Errors",
                        value = stats.errorCount.toString(),
                        valueColor = if (stats.errorCount > 0) NothingColors.accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Timeline section
                item {
                    Spacer(modifier = Modifier.height(NothingSpacing.xl))
                    NothingSectionHeader(text = "Timeline")
                }

                itemsIndexed(entries) { index, entry ->
                    val kindColor = when (entry.kind) {
                        "FIRED" -> NothingColors.success
                        "MODE_ACTIVATED" -> NothingColors.success
                        "MODE_DEACTIVATED" -> MaterialTheme.colorScheme.onSurfaceVariant
                        "SUPPRESSED_COOLDOWN" -> NothingColors.warning
                        "CONDITIONS_NOT_MET" -> MaterialTheme.colorScheme.onSurfaceVariant
                        "ERROR" -> NothingColors.accent
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    NothingDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = NothingSpacing.md),
                    ) {
                        Text(
                            text = String.format("%02d", index + 1),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SpaceMono,
                            modifier = Modifier.padding(end = NothingSpacing.sm),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.kind.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium,
                                color = kindColor,
                                fontFamily = SpaceMono,
                            )
                            Text(
                                text = entry.automationId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            if (entry.detail.isNotEmpty()) {
                                Text(
                                    text = entry.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = dateFormat.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SpaceMono,
                        )
                    }
                }
            }
        }
    }
}

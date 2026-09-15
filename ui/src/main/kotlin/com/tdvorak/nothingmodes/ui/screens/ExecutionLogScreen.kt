package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInfoRow
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSegmentedBar
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val LOG_PAGE_SIZE = 15

/** User-facing label for an audit kind — raw enum names read as jargon. */
private fun auditKindLabel(kind: String): String =
    when (kind) {
        "FIRED" -> "Fired"
        "MODE_ACTIVATED" -> "Mode started"
        "MODE_DEACTIVATED" -> "Mode ended"
        "SUPPRESSED_COOLDOWN" -> "Skipped — cooldown"
        "SUPPRESSED_CONFLICT" -> "Skipped — another mode holds a setting"
        "SUPPRESSED_DUPLICATE" -> "Skipped — duplicate event"
        "CONDITIONS_NOT_MET" -> "Conditions not met"
        "BLOCKED_POLICY" -> "Blocked — policy"
        "RULE_NEEDS_REVIEW" -> "Needs review"
        "ERROR" -> "Error"
        else -> kind.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

private fun formatLatency(ms: Long): String =
    if (ms >= 1000) "%.1f s".format(ms / 1000f) else "$ms ms"

data class AuditEntry(
    val automationId: String,
    val kind: String,
    val timestamp: Long,
    val detail: String,
    val latencyMillis: Long = 0,
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
class ExecutionLogViewModel
    @Inject
    constructor(
        db: NothingModesDatabase,
    ) : ViewModel() {
        private val _entries = MutableStateFlow<List<AuditEntry>>(emptyList())
        val entries: StateFlow<List<AuditEntry>> = _entries.asStateFlow()

        private val _stats = MutableStateFlow(ExecutionStats(0, 0, 0, 0, 0, 0, 0, 0f))
        val stats: StateFlow<ExecutionStats> = _stats.asStateFlow()

        private val _names = MutableStateFlow<Map<String, String>>(emptyMap())
        val names: StateFlow<Map<String, String>> = _names.asStateFlow()

        private val auditDao = db.auditDao()

        init {
            viewModelScope.launch {
                db.automationDao().observeAll().collect { list ->
                    _names.value = list.associate { it.id to it.name }
                }
            }
            viewModelScope.launch {
                auditDao.observeRecent(500).collect { entities ->
                    _entries.value =
                        entities.map { entity ->
                            AuditEntry(
                                automationId = entity.automationId,
                                kind = entity.kind,
                                timestamp = entity.atMillis,
                                detail = entity.detail,
                                latencyMillis = entity.latencyMillis,
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
    val names by viewModel.names.collectAsState()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }
    var page by remember { mutableIntStateOf(0) }
    val pageCount = maxOf(1, (entries.size + LOG_PAGE_SIZE - 1) / LOG_PAGE_SIZE)
    val safePage = page.coerceIn(0, pageCount - 1)
    val pageEntries = entries.drop(safePage * LOG_PAGE_SIZE).take(LOG_PAGE_SIZE)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(title = "Execution Log", onBack = onBack)
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (entries.isEmpty()) {
                NothingEmptyState(
                    title = "No executions yet",
                    description = "Modes will appear here when they fire",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = NothingSpacing.md,
                            end = NothingSpacing.md,
                            top = NothingSpacing.lg,
                            bottom = NothingSpacing.xxxl,
                        ),
                ) {
                    // Hero — success rate as the single Doto moment; accent when errors exist
                    item {
                        Text(
                            text = "%.0f%%".format(stats.successRate * 100),
                            style = MaterialTheme.typography.displayLarge,
                            color =
                                if (stats.errorCount > 0) {
                                    NothingColors.accent
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            fontFamily = NothingFonts.doto(),
                        )
                        NothingLabel(text = "Success Rate")
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))

                        // Segmented bar — visual proportion; primary fill, accent only for errors.
                        NothingSegmentedBar(
                            total = 20,
                            filled = (stats.successRate * 20).toInt().coerceIn(0, 20),
                            fillColor =
                                if (stats.errorCount > 0) {
                                    NothingColors.accent
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            height = 12f,
                        )
                    }

                    // Stats section
                    item {
                        Spacer(modifier = Modifier.height(NothingSpacing.xxl))
                        NothingSectionHeader(text = "Statistics")
                        NothingCard {
                            NothingInfoRow(label = "Total Events", value = stats.totalEvents.toString())
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingInfoRow(
                                label = "Fired",
                                value = stats.firedCount.toString(),
                                valueColor = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingInfoRow(label = "Mode Activated", value = stats.modeActivatedCount.toString())
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingInfoRow(label = "Mode Deactivated", value = stats.modeDeactivatedCount.toString())
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingInfoRow(
                                label = "Skipped — cooldown",
                                value = stats.suppressedCount.toString(),
                                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingInfoRow(
                                label = "Conditions Not Met",
                                value = stats.conditionsNotMetCount.toString(),
                                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingInfoRow(
                                label = "Errors",
                                value = stats.errorCount.toString(),
                                valueColor =
                                    if (stats.errorCount > 0) {
                                        NothingColors.accent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }

                    // Timeline section — paginated, errors carry the red accent
                    item {
                        Spacer(modifier = Modifier.height(NothingSpacing.xl))
                        NothingSectionHeader(text = "Timeline")
                        NothingCard {
                            pageEntries.forEachIndexed { index, entry ->
                                val isError = entry.kind == "ERROR"
                                val kindColor =
                                    when (entry.kind) {
                                        "FIRED" -> MaterialTheme.colorScheme.primary
                                        "MODE_ACTIVATED" -> MaterialTheme.colorScheme.primary
                                        "MODE_DEACTIVATED" -> MaterialTheme.colorScheme.onSurfaceVariant
                                        "SUPPRESSED_COOLDOWN" -> MaterialTheme.colorScheme.onSurfaceVariant
                                        "CONDITIONS_NOT_MET" -> MaterialTheme.colorScheme.onSurfaceVariant
                                        "ERROR" -> NothingColors.accent
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                if (index > 0) NothingDivider()
                                val latencySuffix =
                                    if (entry.latencyMillis > 0) {
                                        " · fired in ${formatLatency(entry.latencyMillis)}"
                                    } else {
                                        ""
                                    }
                                NothingListRow(
                                    title = auditKindLabel(entry.kind),
                                    titleColor = if (isError) NothingColors.accent else null,
                                    subtitle =
                                        (names[entry.automationId] ?: entry.automationId) +
                                            (if (entry.detail.isNotEmpty()) " · ${entry.detail}" else "") +
                                            latencySuffix,
                                    leading = {
                                        NothingIconCircle(size = 40f) {
                                            Text(
                                                text = String.format("%02d", safePage * LOG_PAGE_SIZE + index + 1),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = kindColor,
                                                fontFamily = NothingFonts.mono(),
                                            )
                                        }
                                    },
                                    trailing = {
                                        Text(
                                            text = dateFormat.format(Date(entry.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isError) NothingColors.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = NothingFonts.mono(),
                                        )
                                    },
                                )
                            }
                            if (pageCount > 1) {
                                NothingDivider()
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = NothingSpacing.sm),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "‹ NEWER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                            if (safePage > 0) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        fontFamily = NothingFonts.mono(),
                                        modifier =
                                            Modifier
                                                .clickable(enabled = safePage > 0) { page = safePage - 1 }
                                                .padding(NothingSpacing.sm),
                                    )
                                    Text(
                                        text = "${safePage + 1} / $pageCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = NothingFonts.mono(),
                                        modifier = Modifier.padding(NothingSpacing.sm),
                                    )
                                    Text(
                                        text = "OLDER ›",
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                            if (safePage < pageCount - 1) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        fontFamily = NothingFonts.mono(),
                                        modifier =
                                            Modifier
                                                .clickable(enabled = safePage < pageCount - 1) { page = safePage + 1 }
                                                .padding(NothingSpacing.sm),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

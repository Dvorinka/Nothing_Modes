package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.prefs.CreatorPreferences
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingStatusDot
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomationDetailViewModel
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
        private val store: AutomationStore,
    ) : ViewModel() {
        private val _automation = MutableStateFlow<Automation?>(null)
        val automation: StateFlow<Automation?> = _automation.asStateFlow()

        fun runNow() {
            val current = _automation.value ?: return
            val intent =
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_MANUAL
                    putExtra(AutomationService.EXTRA_MANUAL_ID, current.id.value)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        fun load(id: String) {
            viewModelScope.launch {
                _automation.value =
                    store.get(
                        com.tdvorak.nothingmodes.engine.model
                            .AutomationId(id),
                    )
            }
        }

        fun share() {
            val current = _automation.value ?: return
            viewModelScope.launch {
                runCatching {
                    val appVersion =
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull().orEmpty()
                    val creator = CreatorPreferences(context).get()
                    val export =
                        com.tdvorak.nothingmodes.engine.runtime
                            .ImportExportService(store, appVersion)
                            .export(listOf(current.id), creator)
                    val share =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Nothing Modes routine: ${current.name}")
                            putExtra(Intent.EXTRA_TEXT, export.json)
                        }
                    val chooser = Intent.createChooser(share, "Share routine")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }.onFailure {
                    Toast.makeText(context, "Share failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun delete(onDeleted: () -> Unit) {
            val current = _automation.value ?: return
            viewModelScope.launch {
                store.delete(current.id)
                onDeleted()
            }
        }

        fun duplicate(onDuplicated: () -> Unit) {
            val current = _automation.value ?: return
            viewModelScope.launch {
                val copy =
                    current.copy(
                        id =
                            com.tdvorak.nothingmodes.engine.model.AutomationId(
                                "${current.id.value}-copy-${System.currentTimeMillis()}",
                            ),
                        name = "${current.name} (copy)",
                        enabled = false,
                    )
                store.save(copy)
                onDuplicated()
            }
        }
    }

@Composable
fun AutomationDetailScreen(
    automationId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: AutomationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(automationId) { viewModel.load(automationId) }
    val automation by viewModel.automation.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Detail",
                onBack = onBack,
                actions =
                    listOf(
                        TopBarAction("Run", icon = Icons.Filled.PlayArrow, accent = true, onClick = { viewModel.runNow() }),
                        TopBarAction("Share", icon = Icons.Filled.Share, onClick = { viewModel.share() }),
                        TopBarAction("Edit", icon = Icons.Filled.Edit, onClick = onEdit),
                        TopBarAction("Copy", icon = Icons.Filled.ContentCopy, onClick = { viewModel.duplicate(onBack) }),
                        TopBarAction("Delete", icon = Icons.Filled.Delete, onClick = { viewModel.delete(onBack) }),
                    ),
            )
        },
    ) { padding ->
        val data = automation
        if (data == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "[ LOADING... ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = NothingSpacing.md),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val statusText =
                    when (data.status) {
                        AutomationStatus.ARMED -> "ARMED"
                        AutomationStatus.DISABLED -> "DISABLED"
                        AutomationStatus.PENDING_APPROVAL -> "PENDING"
                        AutomationStatus.NEEDS_REVIEW -> "REVIEW"
                    }
                val statusColor =
                    when (data.status) {
                        AutomationStatus.ARMED -> MaterialTheme.colorScheme.primary
                        AutomationStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        AutomationStatus.PENDING_APPROVAL -> NothingColors.accent
                        AutomationStatus.NEEDS_REVIEW -> NothingColors.accent
                    }

                NothingCardLarge(
                    modifier = Modifier.padding(top = NothingSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingIconCircle(size = 56f) {
                            Text(
                                text =
                                    data.name
                                        .firstOrNull()
                                        ?.uppercaseChar()
                                        ?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SpaceMono,
                            )
                        }
                        Spacer(modifier = Modifier.width(NothingSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = data.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = Doto,
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.xs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NothingStatusDot(color = statusColor, size = 6f)
                                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                                Text(
                                    text = "${data.type.name.uppercase()} · $statusText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = SpaceMono,
                                )
                            }
                        }
                    }

                    if (!data.enabled) {
                        Spacer(modifier = Modifier.height(NothingSpacing.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NothingStatusDot(color = NothingColors.accent, size = 6f)
                            Spacer(modifier = Modifier.width(NothingSpacing.sm))
                            Text(
                                text = "DISABLED — open the editor to enable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NothingColors.accent,
                                fontFamily = SpaceMono,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xl))

                NothingCardLarge {
                    NothingSectionHeader(text = "Trigger")
                    NothingListRow(
                        title = triggerDescription(data.trigger),
                        subtitle = "Tap to reconfigure",
                        onClick = onEdit,
                        leading = {
                            NothingIconCircle(size = 44f, accent = true) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(NothingSpacing.md))

                    NothingSectionHeader(text = "Actions")
                    if (data.actions.isEmpty()) {
                        NothingDivider()
                        Text(
                            text = "No actions configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        data.actions.forEachIndexed { index, action ->
                            NothingDivider()
                            NothingListRow(
                                title = actionDescription(action),
                                onClick = onEdit,
                                leading = {
                                    NothingIconCircle(size = 40f) {
                                        Text(
                                            text = String.format("%02d", index + 1),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = SpaceMono,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            }
        }
    }
}

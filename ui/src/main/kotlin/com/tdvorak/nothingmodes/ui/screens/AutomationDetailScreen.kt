package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AutomationDetailViewModel @Inject constructor(
    private val store: AutomationStore,
) : ViewModel() {

    private val _automation = MutableStateFlow<Automation?>(null)
    val automation: StateFlow<Automation?> = _automation.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _automation.value = store.get(com.tdvorak.nothingmodes.engine.model.AutomationId(id))
        }
    }

    fun toggleEnabled() {
        val current = _automation.value ?: return
        viewModelScope.launch {
            val updated = current.copy(enabled = !current.enabled)
            store.save(updated)
            _automation.value = updated
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
            val copy = current.copy(
                id = com.tdvorak.nothingmodes.engine.model.AutomationId(
                    "${current.id.value}-copy-${System.currentTimeMillis()}"
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
                actions = listOf(
                    TopBarAction("EDIT", onClick = onEdit),
                    TopBarAction("COPY", onClick = { viewModel.duplicate(onBack) }),
                    TopBarAction("DEL", onClick = { viewModel.delete(onBack) }),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NothingSpacing.md),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Hero — automation name in Doto
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = Doto,
                    modifier = Modifier.padding(top = NothingSpacing.lg),
                )

                // Status word — large ALL CAPS, status-colored
                val statusText = when (data.status) {
                    AutomationStatus.ARMED -> "ARMED"
                    AutomationStatus.DISABLED -> "DISABLED"
                    AutomationStatus.PENDING_APPROVAL -> "PENDING"
                    AutomationStatus.NEEDS_REVIEW -> "REVIEW"
                }
                val statusColor = when (data.status) {
                    AutomationStatus.ARMED -> MaterialTheme.colorScheme.primary
                    AutomationStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                    AutomationStatus.PENDING_APPROVAL -> NothingColors.accent
                    AutomationStatus.NEEDS_REVIEW -> NothingColors.accent
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    fontFamily = SpaceMono,
                    modifier = Modifier.padding(top = NothingSpacing.xs),
                )

                // Toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = NothingSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NothingLabel(
                        text = "Enabled",
                        modifier = Modifier.weight(1f),
                    )
                    NothingToggle(
                        checked = data.enabled,
                        onCheckedChange = { viewModel.toggleEnabled() },
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxl))

                NothingCardLarge {
                    NothingSectionHeader(text = "Trigger")
                    NothingListRow(
                        title = triggerDescription(data.trigger),
                        subtitle = "Tap to reconfigure",
                        leading = {
                            NothingIconCircle(size = 44f) {
                                Text(
                                    text = data.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = SpaceMono,
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

package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDetailScreen(
    automationId: String,
    onBack: () -> Unit,
    viewModel: AutomationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(automationId) { viewModel.load(automationId) }
    val automation by viewModel.automation.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(automation?.name ?: "Automation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.delete(onBack) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
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
                Text("Loading...", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatusCard(data, viewModel::toggleEnabled)
                TriggerCard(data.trigger)
                ActionsCard(data.actions)
            }
        }
    }
}

@Composable
private fun StatusCard(automation: Automation, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enabled", style = MaterialTheme.typography.titleMedium)
                Text(
                    when (automation.status) {
                        AutomationStatus.ARMED -> "Armed and ready"
                        AutomationStatus.DISABLED -> "Disabled"
                        AutomationStatus.PENDING_APPROVAL -> "Pending approval"
                        AutomationStatus.NEEDS_REVIEW -> "Needs review"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = automation.enabled, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun TriggerCard(trigger: Trigger) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Trigger", style = MaterialTheme.typography.titleMedium)
            Text(
                triggerDescription(trigger),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionsCard(actions: List<Action>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Actions", style = MaterialTheme.typography.titleMedium)
            if (actions.isEmpty()) {
                Text(
                    "No actions configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                actions.forEachIndexed { index, action ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        actionDescription(action),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun triggerDescription(trigger: Trigger): String = when (trigger) {
    is Trigger.Time -> {
        val cron = trigger.cron
        val at = trigger.at
        when {
            cron != null -> "Schedule: $cron (${trigger.tz})"
            at != null -> "At: $at (${trigger.tz})"
            else -> "Time-based (${trigger.tz})"
        }
    }
    is Trigger.TimeWindow -> "Window: ${trigger.startLocal} - ${trigger.endLocal} (${trigger.tz})"
    is Trigger.Immediate -> "Immediate"
    is Trigger.Notification -> "Notification from ${trigger.pkg}"
    is Trigger.PhoneState -> "Phone: ${trigger.event}"
    is Trigger.Connectivity -> "${trigger.medium} ${trigger.state}"
    is Trigger.Boot -> "On boot"
    is Trigger.BatteryLevel -> "Battery at ${trigger.level}%"
    is Trigger.ScreenStateTrigger -> "Screen ${trigger.state}"
    is Trigger.AppOpened -> "App opened: ${trigger.pkg}"
    is Trigger.Geofence -> "Geofence (${trigger.lat}, ${trigger.lng}) r=${trigger.radiusM}m"
}

private fun actionDescription(action: Action): String = when (action) {
    is Action.SetWifi -> "Wi-Fi: ${if (action.on) "On" else "Off"}"
    is Action.SetBluetooth -> "Bluetooth: ${if (action.on) "On" else "Off"}"
    is Action.SetMobileData -> "Mobile Data: ${if (action.on) "On" else "Off"}"
    is Action.SetDnd -> "DND: ${action.mode.name.lowercase()}"
    is Action.SetRinger -> "Ringer: ${action.mode}"
    is Action.LaunchApp -> "Launch: ${action.pkg}"
    is Action.OpenUrl -> "Open URL: ${action.url}"
    is Action.ShowNotification -> "Notification: ${action.title}"
    is Action.SetVolume -> "Volume ${action.stream}: ${action.level}"
    is Action.SetFlashlight -> "Flashlight: ${if (action.on) "On" else "Off"}"
    is Action.SetDarkMode -> "Dark Mode: ${action.mode.name.lowercase()}"
    is Action.OpenSettingsScreen -> "Open Settings: ${action.screen.name.lowercase()}"
    is Action.Vibrate -> "Vibrate: ${action.durationMs}ms"
    is Action.SetBrightness -> "Brightness: ${action.level}"
    is Action.SetAutoBrightness -> "Auto Brightness: ${if (action.on) "On" else "Off"}"
    is Action.SetExtraDim -> "Extra Dim: ${if (action.on) "On" else "Off"}"
    is Action.SetScreenTimeout -> "Screen Timeout: ${action.timeoutMs}ms"
    is Action.SetGlyph -> "Glyph: ${if (action.on) "On" else "Off"}"
    is Action.SetGlyphMatrix -> "Glyph Matrix: ${if (action.restore) "Restore" else "Set"}"
    is Action.GlyphAnimate -> "Glyph Animate: ${action.zone ?: "all"} ${action.periodMs}ms x${action.cycles}"
    is Action.GlyphProgress -> "Glyph Progress: ${action.progress}%"
    is Action.GlyphText -> "Glyph Text: ${action.text.take(30)}"
    is Action.GlyphScrollingText -> "Glyph Scroll: ${action.text.take(30)}"
    is Action.GlyphPreset -> "Glyph Preset: ${action.preset}"
    is Action.GlyphTurnOff -> "Glyph Off"
    is Action.CopyText -> "Copy: ${action.text.take(30)}"
    is Action.Wait -> "Wait: ${action.durationMs}ms"
    is Action.WriteSetting -> "Write: ${action.namespace.name.lowercase()}/${action.key}=${action.value}"
}

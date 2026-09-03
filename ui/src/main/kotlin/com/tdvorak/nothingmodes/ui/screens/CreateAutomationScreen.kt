package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistantChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AutomationTemplate(
    val name: String,
    val description: String,
    val type: AutomationType,
    val cron: String,
    val actions: List<Action>,
)

object AutomationTemplates {
    val templates = listOf(
        AutomationTemplate(
            name = "Sleep",
            description = "DND, dark mode, dim screen, glyph off",
            type = AutomationType.MODE,
            cron = "30 22 * * *",
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetDarkMode(NightMode.ON),
                Action.SetExtraDim(on = true, restore = true),
                Action.SetBrightness(level = 26, restore = true),
                Action.SetGlyph(on = false),
            ),
        ),
        AutomationTemplate(
            name = "Morning",
            description = "DND off, dark mode off, brightness up",
            type = AutomationType.ROUTINE,
            cron = "0 7 * * *",
            actions = listOf(
                Action.SetDnd(DndMode.OFF),
                Action.SetDarkMode(NightMode.OFF),
                Action.SetExtraDim(on = false, restore = true),
                Action.SetBrightness(level = 128, restore = true),
                Action.SetGlyph(on = true, restore = true),
            ),
        ),
        AutomationTemplate(
            name = "Work Focus",
            description = "DND priority, ringer vibrate, brightness auto",
            type = AutomationType.MODE,
            cron = "0 9 * * 1-5",
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetRinger("VIBRATE"),
                Action.SetAutoBrightness(on = true),
            ),
        ),
        AutomationTemplate(
            name = "Evening Wind Down",
            description = "Dark mode, extra dim, screen timeout 15s",
            type = AutomationType.ROUTINE,
            cron = "0 20 * * *",
            actions = listOf(
                Action.SetDarkMode(NightMode.ON),
                Action.SetExtraDim(on = true, restore = true),
                Action.SetScreenTimeout(timeoutMs = 15_000),
            ),
        ),
        AutomationTemplate(
            name = "Movie Mode",
            description = "Brightness 255, volume max, rotation locked",
            type = AutomationType.MODE,
            cron = "0 21 * * 5,6",
            actions = listOf(
                Action.SetBrightness(level = 255, restore = true),
                Action.SetVolume(com.tdvorak.nothingmodes.engine.model.VolumeStream.MEDIA, 15),
                Action.SetDnd(DndMode.PRIORITY),
            ),
        ),
        AutomationTemplate(
            name = "Custom",
            description = "Start from scratch",
            type = AutomationType.ROUTINE,
            cron = "0 12 * * *",
            actions = listOf(Action.ShowNotification("Custom", "Edit this automation")),
        ),
    )
}

@HiltViewModel
class CreateAutomationViewModel @Inject constructor(
    private val store: AutomationStore,
) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(name: String, template: AutomationTemplate) {
        viewModelScope.launch {
            val id = AutomationId("auto-${System.currentTimeMillis()}")
            val automation = Automation(
                id = id,
                name = name.ifBlank { template.name },
                type = template.type,
                createdBy = CreatedBy.USER,
                status = AutomationStatus.ARMED,
                trigger = Trigger.Time(cron = template.cron, tz = "Europe/Prague"),
                actions = template.actions,
                priority = if (template.type == AutomationType.MODE) 10 else 5,
            )
            store.save(automation)
            _saved.value = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateAutomationViewModel = hiltViewModel(),
) {
    val saved by viewModel.saved.collectAsState()
    var name by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<AutomationTemplate?>(null) }

    if (saved) {
        onSaved()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Automation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Sleep, Work Focus") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text("Choose a template:", style = MaterialTheme.typography.titleMedium)

            AutomationTemplates.templates.forEach { template ->
                TemplateCard(
                    template = template,
                    selected = selectedTemplate == template,
                    onSelect = { selectedTemplate = template },
                )
            }

            Button(
                onClick = {
                    selectedTemplate?.let { viewModel.save(name, it) }
                },
                enabled = selectedTemplate != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create Automation")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCard(
    template: AutomationTemplate,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(template.name, style = MaterialTheme.typography.titleMedium)
            Text(
                template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Schedule: ${template.cron}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${template.actions.size} actions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

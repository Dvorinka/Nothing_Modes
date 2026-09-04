package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingGhostButton
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingRedDot
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
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

    private val _editingAutomation = MutableStateFlow<Automation?>(null)
    val editingAutomation: StateFlow<Automation?> = _editingAutomation.asStateFlow()

    fun loadForEdit(automationId: String) {
        viewModelScope.launch {
            val automation = store.get(AutomationId(automationId))
            _editingAutomation.value = automation
        }
    }

    fun save(name: String, template: AutomationTemplate) {
        viewModelScope.launch {
            val existing = _editingAutomation.value
            val id = existing?.id ?: AutomationId("auto-${System.currentTimeMillis()}")
            val automation = Automation(
                id = id,
                name = name.ifBlank { template.name },
                type = template.type,
                createdBy = existing?.createdBy ?: CreatedBy.USER,
                status = existing?.status ?: AutomationStatus.ARMED,
                trigger = Trigger.Time(cron = template.cron, tz = "Europe/Prague"),
                actions = template.actions,
                priority = if (template.type == AutomationType.MODE) 10 else 5,
            )
            store.save(automation)
            _saved.value = true
        }
    }

    fun saveEdit(name: String) {
        viewModelScope.launch {
            val existing = _editingAutomation.value ?: return@launch
            val updated = existing.copy(name = name.ifBlank { existing.name })
            store.save(updated)
            _saved.value = true
        }
    }
}

@Composable
fun CreateAutomationScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    automationId: String? = null,
    onCustomBuilder: () -> Unit = {},
    viewModel: CreateAutomationViewModel = hiltViewModel(),
) {
    val saved by viewModel.saved.collectAsState()
    val editing by viewModel.editingAutomation.collectAsState()
    var name by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<AutomationTemplate?>(null) }

    LaunchedEffect(automationId) {
        if (automationId != null) {
            viewModel.loadForEdit(automationId)
        }
    }

    LaunchedEffect(editing) {
        if (editing != null && name.isBlank()) {
            name = editing!!.name
        }
    }

    val isEditing = automationId != null

    if (saved) {
        onSaved()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = if (isEditing) "Edit" else "New Mode",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NothingSpacing.md),
        ) {
            // Hero — Doto title
            Text(
                text = if (isEditing) "Edit Mode" else "New Mode",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Doto,
                modifier = Modifier.padding(top = NothingSpacing.lg),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            // Name input
            NothingInput(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                placeholder = "e.g. Sleep, Work Focus",
            )

            if (!isEditing) {
                // Templates section
                NothingSectionHeader(text = "Templates")

                AutomationTemplates.templates.forEach { template ->
                    TemplateRow(
                        template = template,
                        selected = selectedTemplate == template,
                        onSelect = { selectedTemplate = template },
                    )
                }
                NothingDivider()

                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                NothingGhostButton(
                    text = "Custom Builder",
                    onClick = onCustomBuilder,
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xl))

            // Save button
            NothingPillButton(
                text = if (isEditing) "Save Changes" else "Create",
                onClick = {
                    if (isEditing) {
                        viewModel.saveEdit(name)
                    } else {
                        selectedTemplate?.let { viewModel.save(name, it) }
                    }
                },
                enabled = if (isEditing) name.isNotBlank() else selectedTemplate != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
        }
    }
}

@Composable
private fun TemplateRow(
    template: AutomationTemplate,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    NothingDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = NothingSpacing.md),
    ) {
        if (selected) {
            NothingRedDot(size = 6f, modifier = Modifier.padding(end = NothingSpacing.sm))
        } else {
            Spacer(modifier = Modifier.padding(end = 10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.padding(top = NothingSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                NothingLabel(text = template.cron)
                NothingLabel(text = "${template.actions.size} Actions")
            }
        }
    }
}

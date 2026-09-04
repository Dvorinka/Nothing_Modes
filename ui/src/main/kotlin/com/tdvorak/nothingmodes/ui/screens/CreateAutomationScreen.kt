package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingGhostButton
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSegmentedControl
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    /**
     * Create a new automation shell with the given name and type. The trigger
     * and actions are left empty for the Custom Builder to populate; a neutral
     * placeholder trigger is used to keep the model valid.
     */
    fun create(name: String, type: AutomationType) {
        viewModelScope.launch {
            val id = AutomationId("auto-${System.currentTimeMillis()}")
            val automation = Automation(
                id = id,
                name = name.ifBlank { defaultName(type) },
                type = type,
                createdBy = CreatedBy.USER,
                status = AutomationStatus.ARMED,
                trigger = Trigger.Manual,
                actions = listOf(Action.ShowNotification("Custom", "Edit this automation")),
                priority = if (type == AutomationType.MODE) 10 else 5,
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

    private fun defaultName(type: AutomationType): String =
        if (type == AutomationType.MODE) "New Mode" else "New Routine"
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
    var typeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(automationId) {
        if (automationId != null) {
            viewModel.loadForEdit(automationId)
        }
    }

    LaunchedEffect(editing) {
        if (editing != null && name.isBlank()) {
            name = editing!!.name
            typeIndex = if (editing!!.type == AutomationType.MODE) 0 else 1
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
                title = if (isEditing) "Edit" else "New",
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
            // Hero — Doto title, the one expressive moment on the screen
            Text(
                text = if (isEditing) "Edit" else "New",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Doto,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = NothingSpacing.lg),
            )
            NothingLabel(
                text = if (isEditing) "Rename automation" else "Create a mode or routine",
                modifier = Modifier.padding(top = NothingSpacing.xs),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.xl))

            // Name input
            NothingInput(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                placeholder = if (typeIndex == 0) "e.g. Sleep, Work Focus" else "e.g. Morning, Movie",
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            if (!isEditing) {
                // Type segmented control — Mode vs Routine
                NothingSectionHeader(text = "Type")
                NothingSegmentedControl(
                    segments = listOf("Mode", "Routine"),
                    selectedIndex = typeIndex,
                    onSelected = { typeIndex = it },
                )

                Spacer(modifier = Modifier.height(NothingSpacing.xl))

                // Custom Builder entry — kept intact per spec
                NothingSectionHeader(text = "Builder")
                NothingGhostButton(
                    text = "+ Custom Builder",
                    onClick = onCustomBuilder,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xl))

            // Primary action
            NothingPillButton(
                text = if (isEditing) "Save" else "Create",
                onClick = {
                    if (isEditing) {
                        viewModel.saveEdit(name)
                    } else {
                        viewModel.create(
                            name = name,
                            type = if (typeIndex == 0) AutomationType.MODE else AutomationType.ROUTINE,
                        )
                    }
                },
                enabled = name.isNotBlank() || isEditing,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
        }
    }
}

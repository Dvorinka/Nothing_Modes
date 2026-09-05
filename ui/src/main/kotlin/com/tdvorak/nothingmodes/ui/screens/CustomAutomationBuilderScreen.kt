package com.tdvorak.nothingmodes.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.automation.widget.WidgetRefreshHelper
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableListItemScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// ─── Builder State ───────────────────────────────────────────────────────────

data class BuilderState(
    val name: String = "",
    val type: AutomationType = AutomationType.ROUTINE,
    val trigger: Trigger = Trigger.Time(cron = "0 12 * * *", tz = defaultTimeZone()),
    val actions: List<Action> = emptyList(),
    val conditions: List<Condition> = emptyList(),
    val priority: Int = 5,
    val icon: String = "",
    val iconBackground: String = "",
)

@HiltViewModel
class CustomBuilderViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val store: AutomationStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BuilderState())
    val state: StateFlow<BuilderState> = _state.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _editingId = MutableStateFlow<String?>(null)
    val editingId: StateFlow<String?> = _editingId.asStateFlow()

    fun loadForEdit(automationId: String) {
        viewModelScope.launch {
            val automation = store.get(AutomationId(automationId)) ?: return@launch
            _editingId.value = automationId
            _state.value = BuilderState(
                name = automation.name,
                type = automation.type,
                trigger = automation.trigger,
                actions = automation.actions,
                conditions = listOfNotNull(automation.conditions),
                priority = automation.priority,
                icon = automation.icon,
                iconBackground = automation.iconBackground,
            )
        }
    }

    fun updateName(name: String) { _state.value = _state.value.copy(name = name) }
    fun updateType(type: AutomationType) { _state.value = _state.value.copy(type = type) }
    fun updateTrigger(trigger: Trigger) { _state.value = _state.value.copy(trigger = trigger) }
    fun updatePriority(priority: Int) { _state.value = _state.value.copy(priority = priority) }
    fun updateIcon(icon: String) { _state.value = _state.value.copy(icon = icon) }
    fun updateIconBackground(color: String) { _state.value = _state.value.copy(iconBackground = color) }

    fun addAction(action: Action) {
        _state.value = _state.value.copy(actions = _state.value.actions + action)
    }

    fun updateAction(index: Int, action: Action) {
        _state.value = _state.value.copy(actions = _state.value.actions.toMutableList().also { it[index] = action })
    }

    fun removeAction(index: Int) {
        _state.value = _state.value.copy(actions = _state.value.actions.toMutableList().also { it.removeAt(index) })
    }

    fun moveAction(fromIndex: Int, toIndex: Int) {
        val actions = _state.value.actions.toMutableList()
        if (fromIndex < 0 || fromIndex >= actions.size || toIndex < 0 || toIndex >= actions.size) return
        val moved = actions.removeAt(fromIndex)
        actions.add(toIndex, moved)
        _state.value = _state.value.copy(actions = actions)
    }

    fun addCondition(condition: Condition) {
        _state.value = _state.value.copy(conditions = _state.value.conditions + condition)
    }

    fun removeCondition(index: Int) {
        _state.value = _state.value.copy(conditions = _state.value.conditions.toMutableList().also { it.removeAt(index) })
    }

    fun updateCondition(index: Int, condition: Condition) {
        _state.value = _state.value.copy(
            conditions = _state.value.conditions.toMutableList().also { it[index] = condition },
        )
    }

    fun moveCondition(fromIndex: Int, toIndex: Int) {
        val conditions = _state.value.conditions.toMutableList()
        if (fromIndex < 0 || fromIndex >= conditions.size || toIndex < 0 || toIndex >= conditions.size) return
        val moved = conditions.removeAt(fromIndex)
        conditions.add(toIndex, moved)
        _state.value = _state.value.copy(conditions = conditions)
    }

    // ponytail: save() accepts icon/color directly to eliminate any state-propagation race
    //          between the picker sheet and the async store write.
    fun save(icon: String? = null, color: String? = null) {
        viewModelScope.launch {
            val current = _state.value
            val s = if (icon != null || color != null) current.copy(
                icon = icon ?: current.icon,
                iconBackground = color ?: current.iconBackground,
            ) else current
            _state.value = s
            val existingId = _editingId.value
            val id = existingId?.let { AutomationId(it) } ?: AutomationId("auto-${System.currentTimeMillis()}")
            val conditions = when {
                s.conditions.isEmpty() -> null
                s.conditions.size == 1 -> s.conditions[0]
                else -> Condition.And(s.conditions)
            }
            val existing = if (existingId != null) store.get(id) else null
            val automation = Automation(
                id = id,
                name = s.name.ifBlank { "Untitled" },
                type = s.type,
                createdBy = existing?.createdBy ?: CreatedBy.USER,
                status = existing?.status ?: AutomationStatus.ARMED,
                trigger = s.trigger,
                actions = s.actions,
                conditions = conditions,
                priority = s.priority,
                quickAction = true,
                enabled = existing?.enabled ?: true,
                icon = s.icon,
                iconBackground = s.iconBackground,
            )
            store.save(automation)
            WidgetRefreshHelper.refresh(context)
            _saved.value = true
        }
    }

    /** Save a copy as a new automation (Save as). Leaves the original untouched. */
    fun saveAs(icon: String? = null, color: String? = null) {
        viewModelScope.launch {
            val current = _state.value
            val s = if (icon != null || color != null) current.copy(
                icon = icon ?: current.icon,
                iconBackground = color ?: current.iconBackground,
            ) else current
            _state.value = s
            val id = AutomationId("auto-${System.currentTimeMillis()}")
            val conditions = when {
                s.conditions.isEmpty() -> null
                s.conditions.size == 1 -> s.conditions[0]
                else -> Condition.And(s.conditions)
            }
            val automation = Automation(
                id = id,
                name = "${s.name.ifBlank { "Untitled" }} (copy)",
                type = s.type,
                createdBy = CreatedBy.USER,
                status = AutomationStatus.ARMED,
                trigger = s.trigger,
                actions = s.actions,
                conditions = conditions,
                priority = s.priority,
                quickAction = true,
                enabled = true,
                icon = s.icon,
                iconBackground = s.iconBackground,
            )
            store.save(automation)
            WidgetRefreshHelper.refresh(context)
            _saved.value = true
        }
    }
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAutomationBuilderScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    automationId: String? = null,
    navController: NavController? = null,
    onConfigureTrigger: ((String) -> Unit)? = null,
    onAddCondition: (() -> Unit)? = null,
    onEditCondition: ((String) -> Unit)? = null,
    onAddAction: (() -> Unit)? = null,
    onEditAction: ((String) -> Unit)? = null,
    viewModel: CustomBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val saved by viewModel.saved.collectAsState()

    androidx.compose.runtime.LaunchedEffect(automationId) {
        if (automationId != null) viewModel.loadForEdit(automationId)
    }

    // Handle trigger config result.
    // ponytail: using savedStateHandle to pass the trigger JSON back avoids a shared ViewModel for now.
    //          If more cross-screen state appears, introduce a builder-scoped ViewModel.
    val resultFlow = remember(navController) {
        navController?.currentBackStackEntry?.savedStateHandle?.getStateFlow("trigger_result", "")
            ?: MutableStateFlow("")
    }
    val result by resultFlow.collectAsState()
    androidx.compose.runtime.LaunchedEffect(result) {
        if (result.isNotEmpty()) {
            runCatching { Json.decodeFromString<Trigger>(result) }.getOrNull()?.let {
                viewModel.updateTrigger(it)
            }
            navController?.currentBackStackEntry?.savedStateHandle?.set("trigger_result", "")
        }
    }

    // Handle condition result from catalog (all conditions use the bottom sheet).
    var editingConditionIndex by rememberSaveable { mutableStateOf(-1) }
    var conditionSheetCondition by remember { mutableStateOf<Condition?>(null) }
    val conditionResultFlow = remember(navController) {
        navController?.currentBackStackEntry?.savedStateHandle?.getStateFlow("condition_result", "")
            ?: MutableStateFlow("")
    }
    val conditionResult by conditionResultFlow.collectAsState()
    androidx.compose.runtime.LaunchedEffect(conditionResult) {
        if (conditionResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<Condition>(conditionResult) }.getOrNull()?.let { condition ->
                editingConditionIndex = -1
                conditionSheetCondition = condition
            }
            navController?.currentBackStackEntry?.savedStateHandle?.set("condition_result", "")
        }
    }

    // Handle action result from catalog (all actions use the bottom sheet).
    var editingActionIndex by rememberSaveable { mutableStateOf(-1) }
    var actionSheetAction by remember { mutableStateOf<Action?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val actionResultFlow = remember(navController) {
        navController?.currentBackStackEntry?.savedStateHandle?.getStateFlow("action_result", "")
            ?: MutableStateFlow("")
    }
    val actionResult by actionResultFlow.collectAsState()
    androidx.compose.runtime.LaunchedEffect(actionResult) {
        if (actionResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<Action>(actionResult) }.getOrNull()?.let { action ->
                editingActionIndex = -1
                actionSheetAction = action
            }
            navController?.currentBackStackEntry?.savedStateHandle?.set("action_result", "")
        }
    }

    if (saved) { onSaved(); return }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = if (automationId != null) "EDIT ROUTINE" else "NEW ROUTINE",
                onBack = { showDiscardDialog = true },
            )
        },
        // Sticky bottom bar — save button always visible, not clipped by system insets.
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = NothingSpacing.md)
                        .padding(top = NothingSpacing.sm, bottom = NothingSpacing.md),
                ) {
                    NothingPillButton(
                        text = if (automationId != null) "Save Changes" else "Create Automation",
                        onClick = { viewModel.save() },
                        enabled = state.actions.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = NothingSpacing.md,
                end = NothingSpacing.md,
                top = NothingSpacing.lg,
                bottom = NothingSpacing.xxl,
            ),
        ) {
            // Hero — screen title in Doto
            item {
                Text(
                    text = "IF / THEN",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = Doto,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
            }

            // Name (Mode/Routine type selector removed — keep If/Then semantics only)
            item {
                NothingInput(
                    value = state.name,
                    onValueChange = viewModel::updateName,
                    label = "Name",
                    placeholder = "Untitled",
                )
            }

            // Preview tile — live visual of the saved routine card, tappable to pick icon/color
            item {
                AutomationPreviewTile(
                    state = state,
                    modifier = Modifier
                        .padding(vertical = NothingSpacing.md)
                        .clickable { showIconPicker = true },
                )
            }

            // IF: Trigger + Conditions
            item {
                NothingCardLarge(modifier = Modifier.padding(vertical = NothingSpacing.md)) {
                    Text(
                        text = "IF",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = Doto,
                        modifier = Modifier.padding(bottom = NothingSpacing.md),
                    )
                    TriggerEditor(
                        trigger = state.trigger,
                        onUpdate = viewModel::updateTrigger,
                        onConfigure = { onConfigureTrigger?.invoke(Json.encodeToString(state.trigger)) },
                    )

                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    Text(
                        text = "AND",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                    NothingDivider()
                    if (state.conditions.isEmpty()) {
                        Text(
                            "NO CONDITIONS — FIRES ON TRIGGER MATCH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SpaceMono,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        ReorderableColumn(
                            list = state.conditions,
                            onSettle = { fromIndex, toIndex ->
                                viewModel.moveCondition(fromIndex, toIndex)
                            },
                        ) { index, condition, _ ->
                            ReorderableItem {
                                ConditionRow(
                                    condition = condition,
                                    index = index,
                                    onRemove = { viewModel.removeCondition(index) },
                                    onUpdate = { viewModel.updateCondition(index, it) },
                                    onConfigure = { condition ->
                                        editingConditionIndex = index
                                        conditionSheetCondition = condition
                                    },
                                )
                            }
                        }
                    }
                    NothingDivider()
                    Text(
                        text = "+ ADD CONDITION",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = SpaceMono,
                        modifier = Modifier
                            .padding(vertical = NothingSpacing.md)
                            .clickable { onAddCondition?.invoke() },
                    )
                }
            }

            // THEN: Actions
            item {
                NothingCardLarge(modifier = Modifier.padding(bottom = NothingSpacing.md)) {
                    Text(
                        text = "THEN",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = Doto,
                        modifier = Modifier.padding(bottom = NothingSpacing.md),
                    )
                    NothingDivider()
                    if (state.actions.isEmpty()) {
                        Text(
                            "0 ACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingColors.accent,
                            fontFamily = SpaceMono,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        Text(
                            text = "${state.actions.size} ACTION${if (state.actions.size > 1) "S" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SpaceMono,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                        ReorderableColumn(
                            list = state.actions,
                            onSettle = { fromIndex, toIndex ->
                                viewModel.moveAction(fromIndex, toIndex)
                            },
                        ) { index, action, _ ->
                            ReorderableItem {
                                ActionRow(
                                    action = action,
                                    index = index,
                                    onRemove = { viewModel.removeAction(index) },
                                    onUpdate = { viewModel.updateAction(index, it) },
                                    onConfigure = { action ->
                                        editingActionIndex = index
                                        actionSheetAction = action
                                    },
                                )
                            }
                        }
                    }
                    NothingDivider()
                    Text(
                        text = "+ ADD ACTION",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = SpaceMono,
                        modifier = Modifier
                            .padding(vertical = NothingSpacing.md)
                            .clickable { onAddAction?.invoke() },
                    )
                }
            }

            // Priority
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NothingSectionHeader(text = "Priority")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        NothingLabel(text = "Higher wins conflicts")
                        Text(
                            text = state.priority.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = SpaceMono,
                        )
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    PrioritySegmentedBar(
                        total = 10,
                        filled = state.priority.coerceIn(0, 10),
                        onSegmentClick = { viewModel.updatePriority(it) },
                    )
                }
            }
        }

        conditionSheetCondition?.let { condition ->
            ConditionConfigSheet(
                condition = condition,
                onDone = { updated ->
                    if (editingConditionIndex >= 0) {
                        viewModel.updateCondition(editingConditionIndex, updated)
                    } else {
                        viewModel.addCondition(updated)
                    }
                    conditionSheetCondition = null
                    editingConditionIndex = -1
                },
                onDismiss = {
                    conditionSheetCondition = null
                    editingConditionIndex = -1
                },
            )
        }

        actionSheetAction?.let { action ->
            ActionConfigSheet(
                action = action,
                onDone = { updated ->
                    if (editingActionIndex >= 0) {
                        viewModel.updateAction(editingActionIndex, updated)
                    } else {
                        viewModel.addAction(updated)
                    }
                    actionSheetAction = null
                    editingActionIndex = -1
                },
                onDismiss = {
                    actionSheetAction = null
                    editingActionIndex = -1
                },
            )
        }

        if (showIconPicker) {
            IconColorPickerSheet(
                initialIcon = state.icon,
                initialColor = state.iconBackground,
                onDone = { icon, color ->
                    viewModel.updateIcon(icon)
                    viewModel.updateIconBackground(color)
                    showIconPicker = false
                },
                onDismiss = { showIconPicker = false },
            )
        }

        if (showDiscardDialog) {
            BasicAlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NothingSpacing.md),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = NothingShapes.technical,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(NothingSpacing.lg)) {
                        Text(
                            text = "Discard changes?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SpaceMono,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Text(
                            text = "Unsaved changes will be lost.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        ) {
                            NothingPillButton(
                                text = "Cancel",
                                onClick = { showDiscardDialog = false },
                                modifier = Modifier.weight(1f),
                            )
                            NothingPillButton(
                                text = "Save",
                                onClick = {
                                    showDiscardDialog = false
                                    viewModel.save()
                                },
                                enabled = state.actions.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                            NothingPillButton(
                                text = "Discard",
                                onClick = {
                                    showDiscardDialog = false
                                    onBack()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Trigger Editor ──────────────────────────────────────────────────────────

@Composable
private fun TriggerEditor(
    trigger: Trigger,
    onUpdate: (Trigger) -> Unit,
    onConfigure: () -> Unit,
) {
    val triggerLabel = triggerDescription(trigger)

    NothingDivider()

    // Single tappable row — opens the dedicated trigger config page.
    // Trigger type selection happens inside the config page, not inline.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onConfigure)
            .padding(vertical = NothingSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NothingLabel(text = "Trigger")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = triggerLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = SpaceMono,
            )
            Spacer(modifier = Modifier.width(NothingSpacing.sm))
            Text(
                text = ">",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    NothingDivider()
}

// ─── Action Row & Picker ─────────────────────────────────────────────────────

@Composable
private fun ReorderableListItemScope.ActionRow(
    action: Action,
    index: Int,
    onRemove: () -> Unit,
    onUpdate: (Action) -> Unit = {},
    onConfigure: (Action) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NothingSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .draggableHandle()
                    .size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "≡",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = String.format("%02d", index + 1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.width(32.dp),
            )
            Text(
                text = actionDescription(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ">",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier
                    .clickable { onConfigure(action) }
                    .padding(horizontal = NothingSpacing.xs),
            )
            Text(
                text = "DEL",
                style = MaterialTheme.typography.labelSmall,
                color = NothingColors.accent,
                fontFamily = SpaceMono,
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(horizontal = NothingSpacing.xs),
            )
        }

        NothingDivider()
    }
}

// ─── Condition Row & Picker ──────────────────────────────────────────────────

@Composable
private fun ReorderableListItemScope.ConditionRow(
    condition: Condition,
    index: Int,
    onRemove: () -> Unit,
    onUpdate: (Condition) -> Unit = {},
    onConfigure: (Condition) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NothingSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .draggableHandle()
                    .size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "≡",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = String.format("%02d", index + 1),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier.width(32.dp),
            )
            Text(
                text = conditionDescription(condition),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ">",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
                modifier = Modifier
                    .clickable { onConfigure(condition) }
                    .padding(horizontal = NothingSpacing.xs),
            )
            Text(
                text = "DEL",
                style = MaterialTheme.typography.labelSmall,
                color = NothingColors.accent,
                fontFamily = SpaceMono,
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(horizontal = NothingSpacing.xs),
            )
        }
    }
    NothingDivider()
}

// ─── Nothing Picker Dialog (shared) ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> NothingPickerDialog(
    title: String,
    items: List<Pair<String, T>>,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NothingSpacing.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = NothingShapes.technical,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(NothingSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = SpaceMono,
                    )
                    Text(
                        text = "CLOSE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                }
                NothingDivider()

                // Scrollable list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    items.forEachIndexed { index, (label, item) ->
                        NothingDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(item) }
                                .padding(NothingSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = String.format("%02d", index + 1),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SpaceMono,
                                modifier = Modifier.width(32.dp),
                            )
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SpaceMono,
                            )
                        }
                    }
                    NothingDivider()
                }
            }
        }
    }
}

// ─── Priority Segmented Bar (tappable) ───────────────────────────────────────

@Composable
private fun PrioritySegmentedBar(
    total: Int,
    filled: Int,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Float = 20f,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (i in 0 until total) {
                val active = i < filled
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(NothingShapes.technical)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        )
                        .clickable { onSegmentClick(i + 1) }
                        .semantics { contentDescription = "Priority level ${i + 1} of $total" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (i + 1).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontFamily = SpaceMono,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "LOW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
            Text(
                text = "HIGH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
        }
    }
}

@Composable
private fun AutomationPreviewTile(
    state: BuilderState,
    modifier: Modifier = Modifier,
) {
    val iconColor = if (state.iconBackground.isNotBlank()) colorForHex(state.iconBackground) else NothingColors.accent
    val iconTextColor = if (iconColor.luminance() > 0.5f) Color.Black else Color.White

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.card,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(NothingSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment = Alignment.Center,
            ) {
                if (state.icon.isNotBlank()) {
                    Icon(
                        imageVector = iconForName(state.icon),
                        contentDescription = null,
                        tint = iconTextColor,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Text(
                        text = state.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = iconTextColor,
                        fontFamily = Doto,
                    )
                }
            }
            Spacer(modifier = Modifier.width(NothingSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = triggerDescription(state.trigger),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${state.actions.size} actions · ${state.conditions.size} conditions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }
        }
    }
}

internal fun conditionDescription(condition: Condition): String = when (condition) {
    is Condition.TimeWindow -> "Time window: ${condition.startLocal}-${condition.endLocal}"
    is Condition.DayOfWeekCondition -> "Days: ${condition.days.joinToString { it.wireName }}"
    is Condition.BatteryLevel -> "Battery ${condition.op.name} ${condition.level}%"
    is Condition.Charging -> if (condition.isCharging) "Charging" else "Not charging"
    is Condition.WifiConnected -> "Wi-Fi connected${condition.ssid?.let { " ($it)" } ?: ""}"
    is Condition.BluetoothConnected -> "Bluetooth connected${condition.deviceName?.let { " ($it)" } ?: ""}"
    is Condition.ScreenStateCondition -> "Screen ${condition.state.name}"
    is Condition.CurrentModeActive -> "Mode ${condition.modeId} active"
    is Condition.AppInForeground -> "App ${condition.pkg} in foreground"
    is Condition.DarkModeActive -> "Dark mode ${if (condition.active) "on" else "off"}"
    is Condition.PowerSaving -> "Power saving ${if (condition.on) "on" else "off"}"
    is Condition.MediaPlaying -> "Media ${if (condition.playing) "playing" else "not playing"}"
    is Condition.RingerMode -> "Ringer: ${condition.mode}"
    is Condition.AirplaneModeOn -> "Airplane mode ${if (condition.on) "on" else "off"}"
    is Condition.NfcEnabled -> "NFC ${if (condition.enabled) "enabled" else "disabled"}"
    is Condition.LocationEnabled -> "Location ${if (condition.enabled) "enabled" else "disabled"}"
    is Condition.CallStateCondition -> "Call: ${condition.state.name.lowercase()}"
    is Condition.AlarmRinging -> "Alarm ringing${condition.titleMatch?.let { " ($it)" } ?: ""}"
    is Condition.And -> "AND (${condition.all.size} conditions)"
    is Condition.Or -> "OR (${condition.any.size} conditions)"
    is Condition.Not -> "NOT"
}

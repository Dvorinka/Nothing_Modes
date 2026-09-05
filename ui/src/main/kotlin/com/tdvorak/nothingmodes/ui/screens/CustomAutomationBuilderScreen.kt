package com.tdvorak.nothingmodes.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
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
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSecondaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
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
    val enabled: Boolean = true,
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
                enabled = automation.enabled,
            )
        }
    }

    fun updateName(name: String) { _state.value = _state.value.copy(name = name) }
    fun updateType(type: AutomationType) { _state.value = _state.value.copy(type = type) }
    fun updateTrigger(trigger: Trigger) { _state.value = _state.value.copy(trigger = trigger) }
    fun updatePriority(priority: Int) { _state.value = _state.value.copy(priority = priority) }
    fun updateIcon(icon: String) { _state.value = _state.value.copy(icon = icon) }
    fun updateEnabled(enabled: Boolean) { _state.value = _state.value.copy(enabled = enabled) }
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
                enabled = s.enabled,
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
                enabled = s.enabled,
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
    val backStackEntry by navController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf<androidx.navigation.NavBackStackEntry?>(null) }
    val resultFlow = remember(backStackEntry) {
        backStackEntry?.savedStateHandle?.getStateFlow("trigger_result", "")
            ?: MutableStateFlow("")
    }
    val result by resultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(result, backStackEntry) {
        if (result.isNotEmpty()) {
            runCatching { Json.decodeFromString<Trigger>(result) }.getOrNull()?.let {
                viewModel.updateTrigger(it)
            }
            backStackEntry?.savedStateHandle?.set("trigger_result", "")
        }
    }

    // Handle condition result from catalog (all conditions use the bottom sheet).
    var editingConditionIndex by rememberSaveable { mutableStateOf(-1) }
    var conditionSheetCondition by remember { mutableStateOf<Condition?>(null) }
    // Queue of newly added conditions waiting for their config sheet.
    var pendingConditionIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    val conditionResultFlow = remember(backStackEntry) {
        backStackEntry?.savedStateHandle?.getStateFlow("condition_result", "")
            ?: MutableStateFlow("")
    }
    val conditionResult by conditionResultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(conditionResult, backStackEntry) {
        if (conditionResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<Condition>(conditionResult) }.getOrNull()?.let { condition ->
                editingConditionIndex = -1
                conditionSheetCondition = condition
            }
            backStackEntry?.savedStateHandle?.set("condition_result", "")
        }
    }
    // Multi-select catalog result: a JSON array of conditions, added at once.
    // Each new item then gets its config sheet, one after another.
    val conditionsResultFlow = remember(backStackEntry) {
        backStackEntry?.savedStateHandle?.getStateFlow("condition_results", "")
            ?: MutableStateFlow("")
    }
    val conditionsResult by conditionsResultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(conditionsResult, backStackEntry) {
        if (conditionsResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<List<Condition>>(conditionsResult) }
                .getOrNull()?.let { conditions ->
                    if (conditions.isNotEmpty()) {
                        val firstIndex = state.conditions.size
                        conditions.forEach(viewModel::addCondition)
                        pendingConditionIndices = (firstIndex until firstIndex + conditions.size).toList()
                    }
                }
            backStackEntry?.savedStateHandle?.set("condition_results", "")
        }
    }
    // Pop the next queued condition into the config sheet whenever none is open.
    androidx.compose.runtime.LaunchedEffect(conditionSheetCondition, pendingConditionIndices, state.conditions) {
        if (conditionSheetCondition == null && editingConditionIndex < 0 && pendingConditionIndices.isNotEmpty()) {
            val next = pendingConditionIndices.first()
            pendingConditionIndices = pendingConditionIndices.drop(1)
            state.conditions.getOrNull(next)?.let {
                editingConditionIndex = next
                conditionSheetCondition = it
            }
        }
    }

    // Handle action result from catalog (all actions use the bottom sheet).
    var editingActionIndex by rememberSaveable { mutableStateOf(-1) }
    var actionSheetAction by remember { mutableStateOf<Action?>(null) }
    var pendingActionIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val actionResultFlow = remember(backStackEntry) {
        backStackEntry?.savedStateHandle?.getStateFlow("action_result", "")
            ?: MutableStateFlow("")
    }
    val actionResult by actionResultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(actionResult, backStackEntry) {
        if (actionResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<Action>(actionResult) }.getOrNull()?.let { action ->
                editingActionIndex = -1
                actionSheetAction = action
            }
            backStackEntry?.savedStateHandle?.set("action_result", "")
        }
    }
    val actionsResultFlow = remember(backStackEntry) {
        backStackEntry?.savedStateHandle?.getStateFlow("action_results", "")
            ?: MutableStateFlow("")
    }
    val actionsResult by actionsResultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(actionsResult, backStackEntry) {
        if (actionsResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<List<Action>>(actionsResult) }
                .getOrNull()?.let { actions ->
                    if (actions.isNotEmpty()) {
                        val firstIndex = state.actions.size
                        actions.forEach(viewModel::addAction)
                        pendingActionIndices = (firstIndex until firstIndex + actions.size).toList()
                    }
                }
            backStackEntry?.savedStateHandle?.set("action_results", "")
        }
    }
    androidx.compose.runtime.LaunchedEffect(actionSheetAction, pendingActionIndices, state.actions) {
        if (actionSheetAction == null && editingActionIndex < 0 && pendingActionIndices.isNotEmpty()) {
            val next = pendingActionIndices.first()
            pendingActionIndices = pendingActionIndices.drop(1)
            state.actions.getOrNull(next)?.let {
                editingActionIndex = next
                actionSheetAction = it
            }
        }
    }

    // Intercept system back and the back-swipe gesture so leaving always
    // goes through the same Save / Discard / Cancel prompt.
    BackHandler { showDiscardDialog = true }

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
                    text = "WHEN / THEN",
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

            // WHEN: the trigger decides when the routine fires.
            item {
                NothingCardLarge(modifier = Modifier.padding(vertical = NothingSpacing.md)) {
                    NothingLabel(
                        text = "When this happens",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = NothingSpacing.md),
                    )
                    TriggerEditor(
                        trigger = state.trigger,
                        onUpdate = viewModel::updateTrigger,
                        onConfigure = { onConfigureTrigger?.invoke(Json.encodeToString(state.trigger)) },
                    )

                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    Text(
                        text = "ONLY IF (ALL MUST BE TRUE)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                    NothingDivider()
                    if (state.conditions.isEmpty()) {
                        Text(
                            "ALWAYS — RUNS WHENEVER THE TRIGGER FIRES",
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
                    AddRowButton(
                        label = "Add condition",
                        onClick = { onAddCondition?.invoke() },
                    )
                }
            }

            // THEN: Actions
            item {
                NothingCardLarge(modifier = Modifier.padding(bottom = NothingSpacing.md)) {
                    NothingLabel(
                        text = "THEN",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = NothingSpacing.md),
                    )
                    NothingDivider()
                    if (state.actions.isEmpty()) {
                        Text(
                            "0 ACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    AddRowButton(
                        label = "Add action",
                        onClick = { onAddAction?.invoke() },
                    )
                }
            }

            // Advanced — folded away; most routines never need it.
            item {
                var showAdvanced by rememberSaveable { mutableStateOf(false) }
                NothingCardLarge(modifier = Modifier.padding(bottom = NothingSpacing.md)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced }
                            .padding(vertical = NothingSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingLabel(text = "Advanced")
                        Text(
                            text = if (showAdvanced) "−" else "+",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SpaceMono,
                        )
                    }
                    if (showAdvanced) {
                        NothingDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = NothingSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                NothingLabel(text = "Enabled")
                                Text(
                                    text = "Off keeps the routine saved but never fires it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = NothingSpacing.xs),
                                )
                            }
                            NothingToggle(
                                checked = state.enabled,
                                onCheckedChange = viewModel::updateEnabled,
                            )
                        }
                        NothingDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = NothingSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                NothingLabel(text = "Priority")
                                Text(
                                    text = "When two routines fight over the same setting, the higher one wins.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = NothingSpacing.xs),
                                )
                            }
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
                    shape = NothingShapes.dialog,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(NothingSpacing.lg)) {
                        Text(
                            text = "Leave without saving?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = SpaceMono,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.sm))
                        Text(
                            text = "You have unsaved changes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        ) {
                            NothingPillButton(
                                text = "Save",
                                onClick = {
                                    showDiscardDialog = false
                                    viewModel.save()
                                },
                                enabled = state.actions.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                            ) {
                                NothingSecondaryButton(
                                    text = "Cancel",
                                    onClick = { showDiscardDialog = false },
                                    modifier = Modifier.weight(1f),
                                )
                                NothingSecondaryButton(
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
}

// ─── Add Row Button (red circled + label) ────────────────────────────────────

@Composable
private fun AddRowButton(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, NothingColors.accent, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.labelLarge,
                color = NothingColors.accent,
                fontFamily = SpaceMono,
            )
        }
        Spacer(modifier = Modifier.width(NothingSpacing.md))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = NothingColors.accent,
            fontFamily = SpaceMono,
        )
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
            shape = NothingShapes.dialog,
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
    height: Float = 14f,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until total) {
                val active = i < filled
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        )
                        .clickable { onSegmentClick(i + 1) }
                        .semantics { contentDescription = "Priority level ${i + 1} of $total" },
                )
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
    // Monochrome icon tile: surface background, outline border, onSurface icon.
    // iconBackground is intentionally ignored per Nothing OS monochrome spec.
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
            NothingIconCircle(size = 48f) {
                if (state.icon.isNotBlank()) {
                    Icon(
                        imageVector = iconForName(state.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Text(
                        text = state.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = GeistSans,
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

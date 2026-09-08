package com.tdvorak.nothingmodes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tdvorak.nothingmodes.automation.widget.WidgetRefreshHelper
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.NotifyRule
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.canRestore
import com.tdvorak.nothingmodes.engine.model.isGlyphAction
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import com.tdvorak.nothingmodes.engine.model.withRestore
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.components.NotifyRulesEditor
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.GeistSans
import com.tdvorak.nothingmodes.ui.theme.NothingBottomActionBar
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDestructiveButton
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSecondaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.booleanStateLabel
import com.tdvorak.nothingmodes.ui.util.numericStateLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableListItemScope
import javax.inject.Inject

// ─── Builder State ───────────────────────────────────────────────────────────

data class BuilderState(
    val name: String = "",
    val type: AutomationType = AutomationType.ROUTINE,
    val trigger: Trigger = Trigger.Manual,
    val actions: List<Action> = emptyList(),
    val conditions: List<Condition> = emptyList(),
    val priority: Int = 5,
    val icon: String = "",
    val iconBackground: String = "",
    val iconTint: String = "",
    val enabled: Boolean = true,
    val quickAction: Boolean = true,
    val cooldownMs: Long = 0,
    val notifyRules: List<NotifyRule> = emptyList(),
)

@HiltViewModel
class CustomBuilderViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: android.content.Context,
        private val store: AutomationStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BuilderState())
        val state: StateFlow<BuilderState> = _state.asStateFlow()

        private val _saved = MutableStateFlow(false)
        val saved: StateFlow<Boolean> = _saved.asStateFlow()

        private val _saveError = MutableStateFlow<String?>(null)
        val saveError: StateFlow<String?> = _saveError.asStateFlow()

        private val _editingId = MutableStateFlow<String?>(null)
        val editingId: StateFlow<String?> = _editingId.asStateFlow()

        fun clearSaveError() {
            _saveError.value = null
        }

        fun clearSaved() {
            _saved.value = false
        }

        fun loadForEdit(automationId: String) {
            viewModelScope.launch {
                val automation = store.get(AutomationId(automationId)) ?: return@launch
                _editingId.value = automationId
                _state.value =
                    BuilderState(
                        name = automation.name,
                        type = automation.type,
                        trigger = automation.trigger,
                        actions = automation.actions,
                        conditions = listOfNotNull(automation.conditions),
                        priority = automation.priority,
                        icon = automation.icon,
                        iconBackground = automation.iconBackground,
                        iconTint = automation.iconTint,
                        enabled = automation.enabled,
                        quickAction = automation.quickAction,
                        cooldownMs = automation.cooldownMs,
                        notifyRules = automation.notifyRules,
                    )
            }
        }

        fun updateName(name: String) {
            _state.value = _state.value.copy(name = name)
        }

        fun updateType(type: AutomationType) {
            _state.value = _state.value.copy(type = type)
        }

        fun updateTrigger(trigger: Trigger) {
            _state.value = _state.value.copy(trigger = trigger)
        }

        fun updatePriority(priority: Int) {
            _state.value = _state.value.copy(priority = priority)
        }

        fun updateIcon(icon: String) {
            _state.value = _state.value.copy(icon = icon)
        }

        fun updateEnabled(enabled: Boolean) {
            _state.value = _state.value.copy(enabled = enabled)
        }

        fun updateQuickAction(quick: Boolean) {
            _state.value = _state.value.copy(quickAction = quick)
        }

        fun updateCooldown(minutes: Long) {
            _state.value = _state.value.copy(cooldownMs = minutes.coerceIn(0, 1440) * 60_000)
        }

        fun addNotifyRule(rule: NotifyRule) {
            _state.value = _state.value.copy(notifyRules = _state.value.notifyRules + rule)
        }

        fun removeNotifyRule(rule: NotifyRule) {
            _state.value = _state.value.copy(notifyRules = _state.value.notifyRules - rule)
        }

        fun updateNotifyRules(rules: List<NotifyRule>) {
            _state.value = _state.value.copy(notifyRules = rules)
        }

        fun updateIconBackground(color: String) {
            _state.value = _state.value.copy(iconBackground = color)
        }

        fun updateIconTint(color: String) {
            _state.value = _state.value.copy(iconTint = color)
        }

        fun addAction(action: Action) {
            _state.value = _state.value.copy(actions = _state.value.actions + action)
        }

        fun updateAction(
            index: Int,
            action: Action,
        ) {
            _state.value =
                _state.value.copy(
                    actions =
                        _state.value.actions
                            .toMutableList()
                            .also { it[index] = action },
                )
        }

        fun removeAction(index: Int) {
            _state.value =
                _state.value.copy(
                    actions =
                        _state.value.actions
                            .toMutableList()
                            .also { it.removeAt(index) },
                )
        }

        fun moveAction(
            fromIndex: Int,
            toIndex: Int,
        ) {
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
            _state.value =
                _state.value.copy(
                    conditions =
                        _state.value.conditions
                            .toMutableList()
                            .also { it.removeAt(index) },
                )
        }

        fun updateCondition(
            index: Int,
            condition: Condition,
        ) {
            _state.value =
                _state.value.copy(
                    conditions =
                        _state.value.conditions
                            .toMutableList()
                            .also { it[index] = condition },
                )
        }

        fun moveCondition(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val conditions = _state.value.conditions.toMutableList()
            if (fromIndex < 0 || fromIndex >= conditions.size || toIndex < 0 || toIndex >= conditions.size) return
            val moved = conditions.removeAt(fromIndex)
            conditions.add(toIndex, moved)
            _state.value = _state.value.copy(conditions = conditions)
        }

        // ponytail: save() accepts icon/color directly to eliminate any state-propagation race
        //          between the picker sheet and the async store write.
        fun save(
            icon: String? = null,
            color: String? = null,
        ) {
            viewModelScope.launch {
                val current = _state.value
                val s =
                    if (icon != null || color != null) {
                        current.copy(
                            icon = icon ?: current.icon,
                            iconBackground = color ?: current.iconBackground,
                        )
                    } else {
                        current
                    }
                _state.value = s
                val existingId = _editingId.value
                val id = existingId?.let { AutomationId(it) } ?: AutomationId("auto-${System.currentTimeMillis()}")
                val conditions =
                    when {
                        s.conditions.isEmpty() -> null
                        s.conditions.size == 1 -> s.conditions[0]
                        else -> Condition.And(s.conditions)
                    }
                val existing = if (existingId != null) store.get(id) else null
                val automation =
                    Automation(
                        id = id,
                        name = s.name.ifBlank { "Untitled" },
                        // jarvis: modes and routines merged — windowed trigger implies mode semantics.
                        type = if (s.trigger is Trigger.TimeWindow) AutomationType.MODE else AutomationType.ROUTINE,
                        createdBy = existing?.createdBy ?: CreatedBy.USER,
                        status = existing?.status ?: AutomationStatus.ARMED,
                        trigger = s.trigger,
                        actions = s.actions,
                        conditions = conditions,
                        priority = s.priority,
                        quickAction = s.quickAction,
                        cooldownMs = s.cooldownMs,
                        notifyRules = s.notifyRules,
                        enabled = s.enabled,
                        icon = s.icon,
                        iconBackground = s.iconBackground,
                        iconTint = s.iconTint,
                    )
                runCatching {
                    store.save(automation)
                    WidgetRefreshHelper.refresh(context)
                    _saved.value = true
                }.onFailure { _saveError.value = it.message ?: "Save failed" }
            }
        }

        /** Save a copy as a new automation (Save as). Leaves the original untouched. */
        fun saveAs(
            icon: String? = null,
            color: String? = null,
        ) {
            viewModelScope.launch {
                val current = _state.value
                val s =
                    if (icon != null || color != null) {
                        current.copy(
                            icon = icon ?: current.icon,
                            iconBackground = color ?: current.iconBackground,
                        )
                    } else {
                        current
                    }
                _state.value = s
                val id = AutomationId("auto-${System.currentTimeMillis()}")
                val conditions =
                    when {
                        s.conditions.isEmpty() -> null
                        s.conditions.size == 1 -> s.conditions[0]
                        else -> Condition.And(s.conditions)
                    }
                val automation =
                    Automation(
                        id = id,
                        name = "${s.name.ifBlank { "Untitled" }} (copy)",
                        type = if (s.trigger is Trigger.TimeWindow) AutomationType.MODE else AutomationType.ROUTINE,
                        createdBy = CreatedBy.USER,
                        status = AutomationStatus.ARMED,
                        trigger = s.trigger,
                        actions = s.actions,
                        conditions = conditions,
                        priority = s.priority,
                        quickAction = s.quickAction,
                        cooldownMs = s.cooldownMs,
                        notifyRules = s.notifyRules,
                        enabled = s.enabled,
                        icon = s.icon,
                        iconBackground = s.iconBackground,
                        iconTint = s.iconTint,
                    )
                runCatching {
                    store.save(automation)
                    WidgetRefreshHelper.refresh(context)
                    _saved.value = true
                }.onFailure { _saveError.value = it.message ?: "Save failed" }
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
    val resultFlow =
        remember(backStackEntry) {
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

    // Multi-select catalog result: a JSON array of already-configured conditions, added at once.
    val conditionsResultFlow =
        remember(backStackEntry) {
            backStackEntry?.savedStateHandle?.getStateFlow("condition_results", "")
                ?: MutableStateFlow("")
        }
    val conditionsResult by conditionsResultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(conditionsResult, backStackEntry) {
        if (conditionsResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<List<Condition>>(conditionsResult) }
                .getOrNull()
                ?.let { conditions ->
                    conditions.forEach(viewModel::addCondition)
                }
            backStackEntry?.savedStateHandle?.set("condition_results", "")
        }
    }

    // Handle action result from catalog (already configured by the catalog sheets).
    var editingActionIndex by rememberSaveable { mutableStateOf(-1) }
    var actionSheetAction by remember { mutableStateOf<Action?>(null) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val actionsResultFlow =
        remember(backStackEntry) {
            backStackEntry?.savedStateHandle?.getStateFlow("action_results", "")
                ?: MutableStateFlow("")
        }
    val actionsResult by actionsResultFlow.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(actionsResult, backStackEntry) {
        if (actionsResult.isNotEmpty()) {
            runCatching { Json.decodeFromString<List<Action>>(actionsResult) }
                .getOrNull()
                ?.let { actions ->
                    actions.forEach(viewModel::addAction)
                }
            backStackEntry?.savedStateHandle?.set("action_results", "")
        }
    }

    // Intercept system back and the back-swipe gesture so leaving always
    // goes through the same Save / Discard / Cancel prompt.
    BackHandler { showDiscardDialog = true }

    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    androidx.compose.runtime.LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    androidx.compose.runtime.LaunchedEffect(saved) {
        if (saved) {
            snackbarHostState.showSnackbar(if (automationId != null) "Mode updated" else "Mode saved")
            viewModel.clearSaved()
            onSaved()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NothingTopBar(
                title = if (automationId != null) "EDIT MODE" else "NEW MODE",
                onBack = { showDiscardDialog = true },
            )
        },
        // Sticky bottom bar — save button always visible, not clipped by system insets.
        bottomBar = {
            NothingBottomActionBar(
                text = if (automationId != null) "Save Changes" else "Create Mode",
                onClick = { viewModel.save() },
                enabled = state.actions.isNotEmpty(),
                subtitle = if (state.actions.isEmpty()) "Add at least one action to save." else "",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding =
                PaddingValues(
                    start = NothingSpacing.md,
                    end = NothingSpacing.md,
                    top = NothingSpacing.lg,
                    bottom = NothingSpacing.xxl,
                ),
        ) {
            // Hero — screen title in Doto
            item {
                Text(
                    text = if (automationId != null) "EDIT MODE" else "NEW MODE",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.doto(),
                )
                Text(
                    text = "IF · ONLY IF · THEN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                    letterSpacing = 1.0.sp,
                    modifier = Modifier.padding(top = NothingSpacing.xxs),
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
                    onClick = { showIconPicker = true },
                    modifier = Modifier.padding(vertical = NothingSpacing.md),
                )
            }

            // IF: the trigger decides when the mode fires.
            item {
                NothingCardLarge(modifier = Modifier.padding(vertical = NothingSpacing.md)) {
                    Text(
                        text = "IF",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.doto(),
                        modifier = Modifier.padding(bottom = NothingSpacing.xs),
                    )
                    Text(
                        text = "When this happens",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier = Modifier.padding(bottom = NothingSpacing.md),
                    )
                    TriggerEditor(
                        trigger = state.trigger,
                        onUpdate = viewModel::updateTrigger,
                        onConfigure = { onConfigureTrigger?.invoke(Json.encodeToString(state.trigger)) },
                    )

                    Spacer(modifier = Modifier.height(NothingSpacing.md))
                    Text(
                        text = "ONLY IF",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.doto(),
                        modifier = Modifier.padding(bottom = NothingSpacing.xs),
                    )
                    Text(
                        text = "All of these must be true",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                    NothingDivider()
                    if (state.conditions.isEmpty()) {
                        Text(
                            "ALWAYS — RUNS WHENEVER IF FIRES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
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
                    Text(
                        text = "THEN",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.doto(),
                        modifier = Modifier.padding(bottom = NothingSpacing.md),
                    )
                    NothingDivider()
                    if (state.actions.isEmpty()) {
                        Text(
                            "0 ACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        Text(
                            text = "${state.actions.size} ACTION${if (state.actions.size > 1) "S" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = NothingFonts.mono(),
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

            // AFTER IT ENDS — windowed modes revert by default; per-action opt-out.
            if (state.trigger is Trigger.TimeWindow) {
                item {
                    val endLocal = (state.trigger as Trigger.TimeWindow).endLocal
                    NothingCardLarge(modifier = Modifier.padding(bottom = NothingSpacing.md)) {
                        Text(
                            text = "AFTER IT ENDS ($endLocal)",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = NothingFonts.doto(),
                            modifier = Modifier.padding(bottom = NothingSpacing.sm),
                        )
                        Text(
                            text = "Ticked items return to how they were before the mode started. Unticked items keep their new value.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                            modifier = Modifier.padding(bottom = NothingSpacing.sm),
                        )
                        val restorable =
                            state.actions
                                .mapIndexedNotNull { i, a -> if (a.canRestore) i to a else null }
                        if (restorable.isEmpty()) {
                            Text(
                                text = "No revertible changes — the mode just stops.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                                modifier = Modifier.padding(vertical = NothingSpacing.sm),
                            )
                        } else {
                            NothingDivider()
                            restorable.forEach { (index, action) ->
                                NothingListRow(
                                    title = actionDescription(action),
                                    subtitle = "Revert to previous value",
                                    onClick = {
                                        viewModel.updateAction(
                                            index,
                                            action.withRestore(!action.supportsRestore),
                                        )
                                    },
                                    trailing = {
                                        NothingToggle(
                                            checked = action.supportsRestore,
                                            onCheckedChange = {
                                                viewModel.updateAction(index, action.withRestore(it))
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        if (state.actions.any { it.isGlyphAction }) {
                            Text(
                                text = "Glyph lights always turn off when the window ends.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                                modifier = Modifier.padding(top = NothingSpacing.sm),
                            )
                        }
                    }
                }
            }

            // NOTIFY: per-mode heads-up rules. Off by default.
            item {
                NothingCardLarge(modifier = Modifier.padding(bottom = NothingSpacing.md)) {
                    Text(
                        text = "NOTIFY",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.doto(),
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                    Text(
                        text = "Heads-up about this mode. Off unless you add a rule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier = Modifier.padding(bottom = NothingSpacing.sm),
                    )
                    NotifyRulesEditor(
                        rules = state.notifyRules,
                        onChange = viewModel::updateNotifyRules,
                    )
                }
            }

            // Advanced — folded away; most modes never need it.
            item {
                var showAdvanced by rememberSaveable { mutableStateOf(false) }
                NothingCardLarge(modifier = Modifier.padding(bottom = NothingSpacing.md)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showAdvanced = !showAdvanced }
                                .padding(vertical = NothingSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingLabel(text = "Advanced")
                        Icon(
                            imageVector =
                                if (showAdvanced) {
                                    Icons.Filled.ExpandLess
                                } else {
                                    Icons.Filled.ExpandMore
                                },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    if (showAdvanced) {
                        NothingDivider()

                        NothingListRow(
                            title = "Enabled",
                            subtitle = "Routine is saved and will fire",
                            onClick = { viewModel.updateEnabled(!state.enabled) },
                            trailing = {
                                NothingToggle(
                                    checked = state.enabled,
                                    onCheckedChange = viewModel::updateEnabled,
                                )
                            },
                        )
                        NothingDivider()

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = NothingSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                NothingLabel(text = "Cooldown")
                                Text(
                                    text = "Minimum minutes before the mode can fire again.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = NothingSpacing.xs),
                                )
                            }
                            NothingInput(
                                value = (state.cooldownMs / 60_000).toString(),
                                onValueChange = { text ->
                                    viewModel.updateCooldown(text.toLongOrNull() ?: 0)
                                },
                                label = "min",
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                                    ),
                                modifier = Modifier.width(96.dp),
                            )
                        }

                        NothingDivider()

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = NothingSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                NothingLabel(text = "Priority")
                                Text(
                                    text = "Higher wins when two modes fight.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = NothingSpacing.xs),
                                )
                            }
                            Text(
                                text = state.priority.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = NothingFonts.mono(),
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
                initialTint = state.iconTint,
                onDone = { icon, color, tint ->
                    viewModel.updateIcon(icon)
                    viewModel.updateIconBackground(color)
                    viewModel.updateIconTint(tint)
                    showIconPicker = false
                },
                onDismiss = { showIconPicker = false },
            )
        }

        if (showDiscardDialog) {
            BasicAlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                modifier =
                    Modifier
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
                            fontFamily = NothingFonts.mono(),
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
                                NothingDestructiveButton(
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(NothingColors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontFamily = NothingFonts.mono(),
            )
        }
        Spacer(modifier = Modifier.width(NothingSpacing.md))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = NothingColors.accent,
            fontFamily = NothingFonts.mono(),
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
    NothingListRow(
        title = triggerDescription(trigger),
        subtitle = "IF",
        onClick = onConfigure,
        leading = {
            NothingIconCircle(size = 44f) {
                Text(
                    text = "T",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.mono(),
                )
            }
        },
    )
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
    NothingListRow(
        title = actionDescription(action),
        onClick = { onConfigure(action) },
        leading = {
            Box(
                modifier =
                    Modifier
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
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                    modifier =
                        Modifier
                            .clickable(onClick = onRemove)
                            .padding(horizontal = NothingSpacing.xs),
                )
            }
        },
    )
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
    NothingListRow(
        title = conditionDescription(condition),
        onClick = { onConfigure(condition) },
        leading = {
            Box(
                modifier =
                    Modifier
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
        },
        trailing = {
            Text(
                text = "DEL",
                style = MaterialTheme.typography.labelSmall,
                color = NothingColors.accent,
                fontFamily = NothingFonts.mono(),
                modifier =
                    Modifier
                        .clickable(onClick = onRemove)
                        .padding(horizontal = NothingSpacing.xs),
            )
        },
    )
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.md),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = NothingShapes.dialog,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(NothingSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.mono(),
                    )
                    Text(
                        text = "CLOSE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                }
                NothingDivider()

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                ) {
                    items.forEachIndexed { index, (label, item) ->
                        if (index > 0) NothingDivider()
                        NothingListRow(
                            title = label,
                            leading = {
                                NothingIconCircle(size = 36f) {
                                    Text(
                                        text = String.format("%02d", index + 1),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = NothingFonts.mono(),
                                    )
                                }
                            },
                            onClick = { onPick(item) },
                        )
                    }
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until total) {
                val active = i < filled
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (active) {
                                    NothingColors.accent
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ).clickable { onSegmentClick(i + 1) }
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
                fontFamily = NothingFonts.mono(),
            )
            Text(
                text = "HIGH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }
    }
}

@Composable
private fun AutomationPreviewTile(
    state: BuilderState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NothingCard(modifier = modifier) {
        NothingListRow(
            title = state.name.ifBlank { "Untitled" },
            subtitle = "${state.actions.size} actions · ${state.conditions.size} conditions — tap to set icon",
            onClick = onClick,
            leading = {
                val bgColor =
                    state.iconBackground
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                val tintColor =
                    state.iconTint
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                        ?: MaterialTheme.colorScheme.onSurface
                NothingIconCircle(size = 48f, backgroundColor = bgColor) {
                    if (state.icon.isNotBlank()) {
                        Icon(
                            imageVector = iconForName(state.icon),
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Text(
                            text =
                                state.name
                                    .firstOrNull()
                                    ?.uppercaseChar()
                                    ?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = tintColor,
                            fontFamily = GeistSans,
                        )
                    }
                }
            },
        )
    }
}

internal fun conditionDescription(condition: Condition): String =
    when (condition) {
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
        is Condition.ScreenTime -> "Screen time ${condition.op.name} ${condition.minutes}m"
        is Condition.HeadphonesConnected -> "Headphones ${if (condition.connected) "connected" else "disconnected"}"
        is Condition.DataSaverOn -> "Data saver ${if (condition.on) "on" else "off"}"
        is Condition.AutoSyncOn -> "Auto-sync ${if (condition.on) "on" else "off"}"
        is Condition.AutoRotateOn -> "Auto-rotate ${if (condition.on) "on" else "off"}"
        is Condition.VolumeLevel -> "Volume ${condition.stream.name.lowercase()} ${condition.op.name} ${condition.level}"
        is Condition.ScreenOffFor -> "Screen off ${condition.op.name} ${condition.minutes}m"
        is Condition.ChargingSource -> "Charging via ${condition.source.name.lowercase()}"
        is Condition.BatteryTemp -> "Battery temp ${condition.op.name} ${condition.celsius}°C"
        is Condition.ThermalLevel -> "Thermal ${condition.op.name} level ${condition.level}"
        is Condition.BooleanState -> "${booleanStateLabel(condition.key)} ${if (condition.on) "on" else "off"}"
        is Condition.NumericState -> "${numericStateLabel(condition.key)} ${condition.op.name} ${condition.value}"
        is Condition.AtLocation -> "Within ${condition.radiusM}m of ${condition.lat}, ${condition.lng}"
        is Condition.EventActive -> "Calendar event contains \"${condition.titleMatch}\""
        is Condition.NotificationPresent -> "Notification from ${condition.pkg} contains \"${condition.titleMatch}\""
        is Condition.And -> "AND (${condition.all.size} conditions)"
        is Condition.Or -> "OR (${condition.any.size} conditions)"
        is Condition.Not -> "NOT"
    }

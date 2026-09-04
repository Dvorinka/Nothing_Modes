package com.tdvorak.nothingmodes.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSegmentedControl
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── Builder State ───────────────────────────────────────────────────────────

data class BuilderState(
    val name: String = "",
    val type: AutomationType = AutomationType.ROUTINE,
    val trigger: Trigger = Trigger.Time(cron = "0 12 * * *", tz = "Europe/Prague"),
    val actions: List<Action> = emptyList(),
    val conditions: List<Condition> = emptyList(),
    val priority: Int = 5,
)

@HiltViewModel
class CustomBuilderViewModel @Inject constructor(
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
            )
        }
    }

    fun updateName(name: String) { _state.value = _state.value.copy(name = name) }
    fun updateType(type: AutomationType) { _state.value = _state.value.copy(type = type) }
    fun updateTrigger(trigger: Trigger) { _state.value = _state.value.copy(trigger = trigger) }
    fun updatePriority(priority: Int) { _state.value = _state.value.copy(priority = priority) }

    fun addAction(action: Action) {
        _state.value = _state.value.copy(actions = _state.value.actions + action)
    }

    fun updateAction(index: Int, action: Action) {
        _state.value = _state.value.copy(actions = _state.value.actions.toMutableList().also { it[index] = action })
    }

    fun removeAction(index: Int) {
        _state.value = _state.value.copy(actions = _state.value.actions.toMutableList().also { it.removeAt(index) })
    }

    fun moveAction(index: Int, up: Boolean) {
        val actions = _state.value.actions.toMutableList()
        val target = if (up) index - 1 else index + 1
        if (target < 0 || target >= actions.size) return
        val tmp = actions[index]; actions[index] = actions[target]; actions[target] = tmp
        _state.value = _state.value.copy(actions = actions)
    }

    fun addCondition(condition: Condition) {
        _state.value = _state.value.copy(conditions = _state.value.conditions + condition)
    }

    fun removeCondition(index: Int) {
        _state.value = _state.value.copy(conditions = _state.value.conditions.toMutableList().also { it.removeAt(index) })
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val existingId = _editingId.value
            val id = existingId?.let { AutomationId(it) } ?: AutomationId("auto-${System.currentTimeMillis()}")
            val conditions = when {
                s.conditions.isEmpty() -> null
                s.conditions.size == 1 -> s.conditions[0]
                else -> Condition.And(s.conditions)
            }
            val automation = Automation(
                id = id,
                name = s.name.ifBlank { "Untitled" },
                type = s.type,
                createdBy = if (existingId != null) store.get(id)?.createdBy ?: CreatedBy.USER else CreatedBy.USER,
                status = if (existingId != null) store.get(id)?.status ?: AutomationStatus.ARMED else AutomationStatus.ARMED,
                trigger = s.trigger,
                actions = s.actions,
                conditions = conditions,
                priority = s.priority,
                enabled = if (existingId != null) store.get(id)?.enabled ?: true else true,
            )
            store.save(automation)
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
    viewModel: CustomBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val saved by viewModel.saved.collectAsState()

    androidx.compose.runtime.LaunchedEffect(automationId) {
        if (automationId != null) viewModel.loadForEdit(automationId)
    }

    if (saved) { onSaved(); return }

    var showAddActionDialog by remember { mutableStateOf(false) }
    var showAddConditionDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = if (automationId != null) "Edit" else "New",
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
            // Hero — screen title in Doto
            Text(
                text = if (automationId != null) "EDIT" else "BUILDER",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Doto,
                modifier = Modifier.padding(top = NothingSpacing.lg),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            // Name
            NothingInput(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = "Name",
                placeholder = "Untitled",
            )

            Spacer(modifier = Modifier.height(NothingSpacing.md))

            // Type selector — segmented control
            NothingLabel(text = "Type")
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingSegmentedControl(
                segments = listOf("Mode", "Routine"),
                selectedIndex = if (state.type == AutomationType.MODE) 0 else 1,
                onSelected = { index ->
                    viewModel.updateType(if (index == 0) AutomationType.MODE else AutomationType.ROUTINE)
                },
            )

            // WHEN: Trigger
            NothingSectionHeader(text = "When")
            TriggerEditor(state.trigger, viewModel::updateTrigger)

            // IF: Conditions
            NothingSectionHeader(text = "If")
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
                state.conditions.forEachIndexed { index, condition ->
                    ConditionRow(
                        condition = condition,
                        onRemove = { viewModel.removeCondition(index) },
                    )
                }
            }
            NothingDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddConditionDialog = true }
                    .padding(vertical = NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "+ ADD CONDITION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = SpaceMono,
                )
            }

            // THEN: Actions
            NothingSectionHeader(text = "Then")
            NothingDivider()
            if (state.actions.isEmpty()) {
                Text(
                    "NO ACTIONS — ADD AT LEAST ONE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = SpaceMono,
                    modifier = Modifier.padding(vertical = NothingSpacing.md),
                )
            } else {
                state.actions.forEachIndexed { index, action ->
                    ActionRow(
                        action = action,
                        index = index,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.actions.size - 1,
                        onMoveUp = { viewModel.moveAction(index, up = true) },
                        onMoveDown = { viewModel.moveAction(index, up = false) },
                        onRemove = { viewModel.removeAction(index) },
                        onUpdate = { viewModel.updateAction(index, it) },
                    )
                }
            }
            NothingDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddActionDialog = true }
                    .padding(vertical = NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "+ ADD ACTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = SpaceMono,
                )
            }

            // Priority — segmented bar (10 segments)
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

            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))

            // Save — full width pill button at bottom
            NothingPillButton(
                text = if (automationId != null) "Save Changes" else "Create Automation",
                onClick = viewModel::save,
                enabled = state.actions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
        }
    }

    if (showAddActionDialog) {
        ActionPickerDialog(
            onDismiss = { showAddActionDialog = false },
            onPick = { action ->
                viewModel.addAction(action)
                showAddActionDialog = false
            },
        )
    }

    if (showAddConditionDialog) {
        ConditionPickerDialog(
            onDismiss = { showAddConditionDialog = false },
            onPick = { condition ->
                viewModel.addCondition(condition)
                showAddConditionDialog = false
            },
        )
    }
}

// ─── Trigger Editor ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerEditor(trigger: Trigger, onUpdate: (Trigger) -> Unit) {

    var expanded by remember { mutableStateOf(false) }
    val triggerLabel = triggerDescription(trigger)

    NothingDivider()

    // Trigger type — simple tappable row, not a dropdown box
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = NothingSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NothingLabel(text = "Trigger type")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = triggerLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                Text(
                    text = "▾",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(text = { Text("TIME (CRON SCHEDULE)") }, onClick = {
                onUpdate(Trigger.Time(cron = "0 12 * * *", tz = "Europe/Prague")); expanded = false
            })
            DropdownMenuItem(text = { Text("TIME WINDOW (MODE)") }, onClick = {
                onUpdate(Trigger.TimeWindow("22:00", "07:00", "Europe/Prague")); expanded = false
            })
            DropdownMenuItem(text = { Text("IMMEDIATE (FIRE ONCE)") }, onClick = {
                onUpdate(Trigger.Immediate); expanded = false
            })
            DropdownMenuItem(text = { Text("NOTIFICATION") }, onClick = {
                onUpdate(Trigger.Notification(pkg = "com.example.app")); expanded = false
            })
            DropdownMenuItem(text = { Text("PHONE STATE") }, onClick = {
                onUpdate(Trigger.PhoneState(PhoneEvent.INCOMING_CALL)); expanded = false
            })
            DropdownMenuItem(text = { Text("CONNECTIVITY") }, onClick = {
                onUpdate(Trigger.Connectivity(ConnMedium.WIFI, ConnState.CONNECTED)); expanded = false
            })
            DropdownMenuItem(text = { Text("BOOT") }, onClick = {
                onUpdate(Trigger.Boot); expanded = false
            })
            DropdownMenuItem(text = { Text("BATTERY LEVEL") }, onClick = {
                onUpdate(Trigger.BatteryLevel(20)); expanded = false
            })
            DropdownMenuItem(text = { Text("SCREEN STATE") }, onClick = {
                onUpdate(Trigger.ScreenStateTrigger(ScreenState.OFF)); expanded = false
            })
            DropdownMenuItem(text = { Text("APP OPENED") }, onClick = {
                onUpdate(Trigger.AppOpened("com.example.app")); expanded = false
            })
            DropdownMenuItem(text = { Text("GEOFENCE") }, onClick = {
                onUpdate(Trigger.Geofence(50.0755, 14.4378, 100.0, com.tdvorak.nothingmodes.engine.model.Transition.ENTER)); expanded = false
            })
            DropdownMenuItem(text = { Text("MANUAL") }, onClick = {
                onUpdate(Trigger.Manual); expanded = false
            })
            DropdownMenuItem(text = { Text("BLUETOOTH DEVICE") }, onClick = {
                onUpdate(Trigger.BluetoothDevice(com.tdvorak.nothingmodes.engine.model.ConnState.CONNECTED)); expanded = false
            })
            DropdownMenuItem(text = { Text("WIFI CONNECTED") }, onClick = {
                onUpdate(Trigger.WifiConnected()); expanded = false
            })
            DropdownMenuItem(text = { Text("CALENDAR EVENT") }, onClick = {
                onUpdate(Trigger.CalendarEvent()); expanded = false
            })
        }
    }

    NothingDivider()
    Spacer(modifier = Modifier.height(NothingSpacing.sm))

    // Trigger-specific parameter fields
    TriggerParams(trigger, onUpdate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerParams(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    when (trigger) {
        is Trigger.Time -> {
            NothingInput(
                value = trigger.cron ?: "",
                onValueChange = { onUpdate(trigger.copy(cron = it.ifBlank { null })) },
                label = "Cron expression (min hour day month dow)",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.tz,
                onValueChange = { onUpdate(trigger.copy(tz = it)) },
                label = "Timezone",
            )
        }
        is Trigger.TimeWindow -> {
            NothingInput(
                value = trigger.startLocal,
                onValueChange = { onUpdate(trigger.copy(startLocal = it)) },
                label = "Start (HH:mm)",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.endLocal,
                onValueChange = { onUpdate(trigger.copy(endLocal = it)) },
                label = "End (HH:mm)",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.tz,
                onValueChange = { onUpdate(trigger.copy(tz = it)) },
                label = "Timezone",
            )
        }
        is Trigger.Notification -> {
            NothingInput(
                value = trigger.pkg,
                onValueChange = { onUpdate(trigger.copy(pkg = it)) },
                label = "Package name",
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.titleMatch ?: "",
                onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
                label = "Title match (optional)",
            )
        }
        is Trigger.PhoneState -> {
            NothingEnumSelector(
                label = "Event",
                value = trigger.event.name,
                options = PhoneEvent.entries.map { it.name },
                onSelect = { event ->
                    onUpdate(trigger.copy(event = PhoneEvent.valueOf(event)))
                },
            )
        }
        is Trigger.Connectivity -> {
            Row(horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm)) {
                Box(modifier = Modifier.weight(1f)) {
                    NothingEnumSelector(
                        label = "Medium",
                        value = trigger.medium.name,
                        options = ConnMedium.entries.map { it.name },
                        onSelect = { medium ->
                            onUpdate(trigger.copy(medium = ConnMedium.valueOf(medium)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    NothingEnumSelector(
                        label = "State",
                        value = trigger.state.name,
                        options = ConnState.entries.map { it.name },
                        onSelect = { state ->
                            onUpdate(trigger.copy(state = ConnState.valueOf(state)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        is Trigger.BatteryLevel -> {
            NothingInput(
                value = trigger.level.toString(),
                onValueChange = { onUpdate(trigger.copy(level = it.toIntOrNull() ?: 0)) },
                label = "Battery level %",
            )
        }
        is Trigger.ScreenStateTrigger -> {
            NothingEnumSelector(
                label = "Screen state",
                value = trigger.state.name,
                options = ScreenState.entries.map { it.name },
                onSelect = { state ->
                    onUpdate(trigger.copy(state = ScreenState.valueOf(state)))
                },
            )
        }
        is Trigger.AppOpened -> {
            NothingInput(
                value = trigger.pkg,
                onValueChange = { onUpdate(trigger.copy(pkg = it)) },
                label = "Package name",
            )
        }
        is Trigger.Geofence -> {
            Row(horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm)) {
                NothingInput(
                    value = trigger.lat.toString(),
                    onValueChange = { onUpdate(trigger.copy(lat = it.toDoubleOrNull() ?: 0.0)) },
                    label = "Latitude",
                    modifier = Modifier.weight(1f),
                )
                NothingInput(
                    value = trigger.lng.toString(),
                    onValueChange = { onUpdate(trigger.copy(lng = it.toDoubleOrNull() ?: 0.0)) },
                    label = "Longitude",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = trigger.radiusM.toString(),
                onValueChange = { onUpdate(trigger.copy(radiusM = it.toDoubleOrNull() ?: 100.0)) },
                label = "Radius (meters)",
            )
        }
        is Trigger.Immediate, is Trigger.Boot, is Trigger.Manual -> {}
        is Trigger.BluetoothDevice -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val bondedDevices = remember {
                val hasBtConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                if (!hasBtConnect) {
                    emptyList()
                } else {
                    @SuppressLint("MissingPermission")
                    runCatching {
                        val bm = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
                        bm?.adapter?.bondedDevices?.map { it.name to it.address } ?: emptyList()
                    }.getOrDefault(emptyList())
                }
            }
            NothingInput(
                value = trigger.deviceName ?: "",
                onValueChange = { onUpdate(trigger.copy(deviceName = it.ifBlank { null })) },
                label = "Device name (blank = any)",
            )
            if (bondedDevices.isNotEmpty()) {
                NothingLabel(
                    text = "Paired devices",
                    modifier = Modifier.padding(top = NothingSpacing.md),
                )
                bondedDevices.forEach { (name, address) ->
                    NothingDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdate(trigger.copy(deviceName = name, deviceAddress = address))
                            }
                            .padding(vertical = NothingSpacing.sm),
                    ) {
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = address,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = SpaceMono,
                            )
                        }
                    }
                }
            }
        }
        is Trigger.WifiConnected -> {
            NothingInput(
                value = trigger.ssid ?: "",
                onValueChange = { onUpdate(trigger.copy(ssid = it.ifBlank { null })) },
                label = "SSID (blank = any)",
            )
        }
        is Trigger.CalendarEvent -> {
            NothingInput(
                value = trigger.titleMatch ?: "",
                onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
                label = "Title contains (blank = any)",
            )
        }
    }
}

// ─── Nothing Enum Selector (ExposedDropdownMenuBox with Nothing borders) ─────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NothingEnumSelector(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        NothingLabel(
            text = label,
            modifier = Modifier.padding(bottom = NothingSpacing.xs),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = NothingShapes.technical,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .height(44.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NothingSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = value.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = SpaceMono,
                    )
                    Text(
                        text = "▾",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SpaceMono,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// ─── Action Row & Picker ─────────────────────────────────────────────────────

@Composable
private fun ActionRow(
    action: Action,
    index: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: (Action) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NothingSpacing.md),
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
                text = actionDescription(action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (canMoveUp) {
                Text(
                    text = "UP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                    modifier = Modifier
                        .clickable(onClick = onMoveUp)
                        .padding(horizontal = NothingSpacing.xs),
                )
            }
            if (canMoveDown) {
                Text(
                    text = "DOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                    modifier = Modifier
                        .clickable(onClick = onMoveDown)
                        .padding(horizontal = NothingSpacing.xs),
                )
            }
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

        // Action-specific editors
        when (action) {
            is Action.SendSms -> {
                NothingInput(
                    value = action.number,
                    onValueChange = { onUpdate(action.copy(number = it)) },
                    label = "Phone number",
                )
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                NothingInput(
                    value = action.text,
                    onValueChange = { onUpdate(action.copy(text = it)) },
                    label = "Message",
                    singleLine = false,
                )
            }
            is Action.ShowNotification -> {
                NothingInput(
                    value = action.title,
                    onValueChange = { onUpdate(action.copy(title = it)) },
                    label = "Title",
                )
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                NothingInput(
                    value = action.text,
                    onValueChange = { onUpdate(action.copy(text = it)) },
                    label = "Text",
                    singleLine = false,
                )
            }
            is Action.OpenUrl -> {
                NothingInput(
                    value = action.url,
                    onValueChange = { onUpdate(action.copy(url = it)) },
                    label = "URL",
                )
            }
            is Action.LaunchApp -> {
                NothingInput(
                    value = action.pkg,
                    onValueChange = { onUpdate(action.copy(pkg = it)) },
                    label = "Package name",
                )
            }
            is Action.CopyText -> {
                NothingInput(
                    value = action.text,
                    onValueChange = { onUpdate(action.copy(text = it)) },
                    label = "Text to copy",
                    singleLine = false,
                )
            }
            else -> {}
        }
        NothingDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionPickerDialog(
    onDismiss: () -> Unit,
    onPick: (Action) -> Unit,
) {
    val actions = listOf(
        "Wi-Fi On" to Action.SetWifi(true),
        "Wi-Fi Off" to Action.SetWifi(false),
        "Bluetooth On" to Action.SetBluetooth(true),
        "Bluetooth Off" to Action.SetBluetooth(false),
        "Mobile Data On" to Action.SetMobileData(true),
        "Mobile Data Off" to Action.SetMobileData(false),
        "DND Priority" to Action.SetDnd(DndMode.PRIORITY),
        "DND Total" to Action.SetDnd(DndMode.TOTAL),
        "DND Off" to Action.SetDnd(DndMode.OFF),
        "Dark Mode On" to Action.SetDarkMode(NightMode.ON),
        "Dark Mode Off" to Action.SetDarkMode(NightMode.OFF),
        "Brightness 10%" to Action.SetBrightness(26, restore = true),
        "Brightness 50%" to Action.SetBrightness(128, restore = true),
        "Brightness 100%" to Action.SetBrightness(255, restore = true),
        "Auto Brightness On" to Action.SetAutoBrightness(true),
        "Auto Brightness Off" to Action.SetAutoBrightness(false),
        "Extra Dim On" to Action.SetExtraDim(true, restore = true),
        "Extra Dim Off" to Action.SetExtraDim(false, restore = true),
        "Screen Timeout 15s" to Action.SetScreenTimeout(15_000),
        "Screen Timeout 30s" to Action.SetScreenTimeout(30_000),
        "Screen Timeout 2min" to Action.SetScreenTimeout(120_000),
        "Flashlight On" to Action.SetFlashlight(true),
        "Flashlight Off" to Action.SetFlashlight(false),
        "Vibrate 500ms" to Action.Vibrate(500),
        "Volume Media 50%" to Action.SetVolume(VolumeStream.MEDIA, 8),
        "Volume Media Max" to Action.SetVolume(VolumeStream.MEDIA, 15),
        "Ringer Silent" to Action.SetRinger("SILENT"),
        "Ringer Vibrate" to Action.SetRinger("VIBRATE"),
        "Ringer Normal" to Action.SetRinger("NORMAL"),
        "Glyph On" to Action.SetGlyph(true),
        "Glyph Off" to Action.SetGlyph(false),
        "Glyph Turn Off" to Action.GlyphTurnOff,
        "Glyph Preset: Sleep" to Action.GlyphPreset("sleep"),
        "Glyph Preset: Morning" to Action.GlyphPreset("morning"),
        "Glyph Preset: DND" to Action.GlyphPreset("dnd_active"),
        "Glyph Preset: Charging" to Action.GlyphPreset("charging"),
        "Glyph Animate" to Action.GlyphAnimate(periodMs = 3000, cycles = 3),
        "Glyph Progress 50%" to Action.GlyphProgress(50),
        "Glyph Text" to Action.GlyphText("Hello"),
        "Glyph Scrolling Text" to Action.GlyphScrollingText("Scrolling message"),
        "Show Notification" to Action.ShowNotification("Title", "Text"),
        "Copy Text" to Action.CopyText("clipboard text"),
        "Wait 1s" to Action.Wait(1000),
        "Wait 5s" to Action.Wait(5000),
        "Launch App" to Action.LaunchApp("com.example.app"),
        "Open URL" to Action.OpenUrl("https://example.com"),
        "Open Settings" to Action.OpenSettingsScreen(com.tdvorak.nothingmodes.engine.model.SettingsScreen.SETTINGS),
        "Write Setting" to Action.WriteSetting(SettingNamespace.SYSTEM, "key", "value"),
        // System settings toggles (Phase 4)
        "Auto-Rotate On" to Action.SetAutoRotate(true),
        "Auto-Rotate Off" to Action.SetAutoRotate(false),
        "Battery Saver On" to Action.SetBatterySaver(true),
        "Battery Saver Off" to Action.SetBatterySaver(false),
        "Airplane Mode On" to Action.SetAirplaneMode(true),
        "Airplane Mode Off" to Action.SetAirplaneMode(false),
        "Data Saver On" to Action.SetDataSaver(true),
        "Data Saver Off" to Action.SetDataSaver(false),
        "Hotspot On" to Action.SetHotspot(true),
        "Hotspot Off" to Action.SetHotspot(false),
        "NFC On" to Action.SetNfc(true),
        "NFC Off" to Action.SetNfc(false),
        "Refresh Rate 60Hz" to Action.SetRefreshRate(60),
        "Refresh Rate 90Hz" to Action.SetRefreshRate(90),
        "Refresh Rate 120Hz" to Action.SetRefreshRate(120),
        "Rotation Auto" to Action.SetScreenRotation(com.tdvorak.nothingmodes.engine.model.ScreenOrientation.AUTO),
        "Rotation Portrait" to Action.SetScreenRotation(com.tdvorak.nothingmodes.engine.model.ScreenOrientation.PORTRAIT),
        "Rotation Landscape" to Action.SetScreenRotation(com.tdvorak.nothingmodes.engine.model.ScreenOrientation.LANDSCAPE),
        "Media Play/Pause" to Action.MediaControl(com.tdvorak.nothingmodes.engine.model.MediaCommand.PLAY_PAUSE),
        "Media Next" to Action.MediaControl(com.tdvorak.nothingmodes.engine.model.MediaCommand.NEXT),
        "Media Previous" to Action.MediaControl(com.tdvorak.nothingmodes.engine.model.MediaCommand.PREVIOUS),
        "Media Stop" to Action.MediaControl(com.tdvorak.nothingmodes.engine.model.MediaCommand.STOP),
        "Send SMS" to Action.SendSms(number = "", text = ""),
        "Lock Screen" to Action.LockScreen,
        "Location High Accuracy" to Action.SetLocationMode(com.tdvorak.nothingmodes.engine.model.LocationMode.HIGH_ACCURACY),
        "Location Battery Saving" to Action.SetLocationMode(com.tdvorak.nothingmodes.engine.model.LocationMode.BATTERY_SAVING),
        "Location Device Only" to Action.SetLocationMode(com.tdvorak.nothingmodes.engine.model.LocationMode.DEVICE_ONLY),
        "Location Off" to Action.SetLocationMode(com.tdvorak.nothingmodes.engine.model.LocationMode.OFF),
        "Auto-sync On" to Action.SetAutoSync(on = true),
        "Auto-sync Off" to Action.SetAutoSync(on = false),
        "Clear Notifications" to Action.ClearNotifications,
        "AOD On" to Action.SetAlwaysOnDisplay(on = true),
        "AOD Off" to Action.SetAlwaysOnDisplay(on = false),
        "Screenshot" to Action.TakeScreenshot,
    )

    NothingPickerDialog(
        title = "ADD ACTION",
        items = actions,
        onDismiss = onDismiss,
        onPick = onPick,
    )
}

// ─── Condition Row & Picker ──────────────────────────────────────────────────

@Composable
private fun ConditionRow(condition: Condition, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = conditionDescription(condition),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionPickerDialog(
    onDismiss: () -> Unit,
    onPick: (Condition) -> Unit,
) {
    val conditions = listOf(
        "Battery < 20%" to Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.LT, 20),
        "Battery < 30%" to Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.LT, 30),
        "Battery > 50%" to Condition.BatteryLevel(com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 50),
        "Charging" to Condition.Charging(true),
        "Not Charging" to Condition.Charging(false),
        "Wi-Fi Connected" to Condition.WifiConnected(),
        "Bluetooth Connected" to Condition.BluetoothConnected(),
        "Screen On" to Condition.ScreenStateCondition(ScreenState.ON),
        "Screen Off" to Condition.ScreenStateCondition(ScreenState.OFF),
        "Weekdays" to Condition.DayOfWeekCondition(listOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )),
        "Weekends" to Condition.DayOfWeekCondition(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    )

    NothingPickerDialog(
        title = "ADD CONDITION",
        items = conditions,
        onDismiss = onDismiss,
        onPick = onPick,
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
    height: Float = 8f,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .clip(NothingShapes.technical)
                    .background(
                        if (i < filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    )
                    .clickable { onSegmentClick(i + 1) },
            )
        }
    }
}

private fun conditionDescription(condition: Condition): String = when (condition) {
    is Condition.TimeWindow -> "Time window: ${condition.startLocal}-${condition.endLocal}"
    is Condition.DayOfWeekCondition -> "Days: ${condition.days.joinToString { it.wireName }}"
    is Condition.BatteryLevel -> "Battery ${condition.op.name} ${condition.level}%"
    is Condition.Charging -> if (condition.isCharging) "Charging" else "Not charging"
    is Condition.WifiConnected -> "Wi-Fi connected${condition.ssid?.let { " ($it)" } ?: ""}"
    is Condition.BluetoothConnected -> "Bluetooth connected${condition.deviceName?.let { " ($it)" } ?: ""}"
    is Condition.ScreenStateCondition -> "Screen ${condition.state.name}"
    is Condition.CurrentModeActive -> "Mode ${condition.modeId} active"
    is Condition.AppInForeground -> "App ${condition.pkg} in foreground"
    is Condition.And -> "AND (${condition.all.size} conditions)"
    is Condition.Or -> "OR (${condition.any.size} conditions)"
    is Condition.Not -> "NOT"
}

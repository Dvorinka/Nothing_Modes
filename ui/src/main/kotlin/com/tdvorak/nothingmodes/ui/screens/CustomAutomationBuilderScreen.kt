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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.tdvorak.nothingmodes.engine.model.BatteryDirection
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
        topBar = {
            TopAppBar(
                title = { Text(if (automationId != null) "Edit Automation" else "Custom Automation") },
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
            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Type selector
            TypeSelector(state.type, viewModel::updateType)

            // WHEN: Trigger
            SectionHeader("WHEN")
            TriggerEditor(state.trigger, viewModel::updateTrigger)

            // IF: Conditions
            SectionHeader("IF (conditions)")
            if (state.conditions.isEmpty()) {
                Text(
                    "No conditions — fires on trigger match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.conditions.forEachIndexed { index, condition ->
                    ConditionRow(
                        condition = condition,
                        onRemove = { viewModel.removeCondition(index) },
                    )
                }
            }
            TextButton(onClick = { showAddConditionDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Add condition")
            }

            // THEN: Actions
            SectionHeader("THEN")
            if (state.actions.isEmpty()) {
                Text(
                    "No actions — add at least one",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                state.actions.forEachIndexed { index, action ->
                    ActionRow(
                        action = action,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.actions.size - 1,
                        onMoveUp = { viewModel.moveAction(index, up = true) },
                        onMoveDown = { viewModel.moveAction(index, up = false) },
                        onRemove = { viewModel.removeAction(index) },
                        onUpdate = { viewModel.updateAction(index, it) },
                    )
                }
            }
            TextButton(onClick = { showAddActionDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Add action")
            }

            // Priority
            OutlinedTextField(
                value = state.priority.toString(),
                onValueChange = { viewModel.updatePriority(it.toIntOrNull() ?: 0) },
                label = { Text("Priority (higher wins conflicts)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Save
            Button(
                onClick = viewModel::save,
                enabled = state.actions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (automationId != null) "Save Changes" else "Create Automation")
            }
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

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun TypeSelector(selected: AutomationType, onSelect: (AutomationType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AutomationType.entries.forEach { type ->
            val label = when (type) {
                AutomationType.MODE -> "Mode"
                AutomationType.ROUTINE -> "Routine"
            }
            TextButton(
                onClick = { onSelect(type) },
                colors = if (selected == type)
                    androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                else
                    androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) { Text(label) }
        }
    }
}

// ─── Trigger Editor ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerEditor(trigger: Trigger, onUpdate: (Trigger) -> Unit) {

    var expanded by remember { mutableStateOf(false) }
    val triggerLabel = triggerDescription(trigger)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = triggerLabel,
                    onValueChange = {},
                    label = { Text("Trigger type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Time (cron schedule)") }, onClick = {
                        onUpdate(Trigger.Time(cron = "0 12 * * *", tz = "Europe/Prague")); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Time Window (mode)") }, onClick = {
                        onUpdate(Trigger.TimeWindow("22:00", "07:00", "Europe/Prague")); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Immediate (fire once)") }, onClick = {
                        onUpdate(Trigger.Immediate); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Notification") }, onClick = {
                        onUpdate(Trigger.Notification(pkg = "com.example.app")); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Phone State") }, onClick = {
                        onUpdate(Trigger.PhoneState(PhoneEvent.INCOMING_CALL)); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Connectivity") }, onClick = {
                        onUpdate(Trigger.Connectivity(ConnMedium.WIFI, ConnState.CONNECTED)); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Boot") }, onClick = {
                        onUpdate(Trigger.Boot); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Battery Level") }, onClick = {
                        onUpdate(Trigger.BatteryLevel(20)); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Screen State") }, onClick = {
                        onUpdate(Trigger.ScreenStateTrigger(ScreenState.OFF)); expanded = false
                    })
                    DropdownMenuItem(text = { Text("App Opened") }, onClick = {
                        onUpdate(Trigger.AppOpened("com.example.app")); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Geofence") }, onClick = {
                        onUpdate(Trigger.Geofence(50.0755, 14.4378, 100.0, com.tdvorak.nothingmodes.engine.model.Transition.ENTER)); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Manual") }, onClick = {
                        onUpdate(Trigger.Manual); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Bluetooth Device") }, onClick = {
                        onUpdate(Trigger.BluetoothDevice(com.tdvorak.nothingmodes.engine.model.ConnState.CONNECTED)); expanded = false
                    })
                    DropdownMenuItem(text = { Text("WiFi Connected") }, onClick = {
                        onUpdate(Trigger.WifiConnected()); expanded = false
                    })
                    DropdownMenuItem(text = { Text("Calendar Event") }, onClick = {
                        onUpdate(Trigger.CalendarEvent()); expanded = false
                    })
                }
            }

            // Trigger-specific parameter fields
            TriggerParams(trigger, onUpdate)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerParams(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    when (trigger) {
        is Trigger.Time -> {
            OutlinedTextField(
                value = trigger.cron ?: "",
                onValueChange = { onUpdate(trigger.copy(cron = it.ifBlank { null })) },
                label = { Text("Cron expression (min hour day month dow)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = trigger.tz,
                onValueChange = { onUpdate(trigger.copy(tz = it)) },
                label = { Text("Timezone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        is Trigger.TimeWindow -> {
            OutlinedTextField(
                value = trigger.startLocal,
                onValueChange = { onUpdate(trigger.copy(startLocal = it)) },
                label = { Text("Start (HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = trigger.endLocal,
                onValueChange = { onUpdate(trigger.copy(endLocal = it)) },
                label = { Text("End (HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = trigger.tz,
                onValueChange = { onUpdate(trigger.copy(tz = it)) },
                label = { Text("Timezone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        is Trigger.Notification -> {
            OutlinedTextField(
                value = trigger.pkg,
                onValueChange = { onUpdate(trigger.copy(pkg = it)) },
                label = { Text("Package name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = trigger.titleMatch ?: "",
                onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
                label = { Text("Title match (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        is Trigger.PhoneState -> {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = trigger.event.name,
                    onValueChange = {},
                    label = { Text("Event") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    PhoneEvent.entries.forEach { event ->
                        DropdownMenuItem(text = { Text(event.name) }, onClick = {
                            onUpdate(trigger.copy(event = event)); expanded = false
                        })
                    }
                }
            }
        }
        is Trigger.Connectivity -> {
            var mediumExpanded by remember { mutableStateOf(false) }
            var stateExpanded by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = mediumExpanded, onExpandedChange = { mediumExpanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = trigger.medium.name, onValueChange = {},
                        label = { Text("Medium") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mediumExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                    )
                    ExposedDropdownMenu(expanded = mediumExpanded, onDismissRequest = { mediumExpanded = false }) {
                        ConnMedium.entries.forEach { medium ->
                            DropdownMenuItem(text = { Text(medium.name) }, onClick = {
                                onUpdate(trigger.copy(medium = medium)); mediumExpanded = false
                            })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = trigger.state.name, onValueChange = {},
                        label = { Text("State") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                    )
                    ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
                        ConnState.entries.forEach { state ->
                            DropdownMenuItem(text = { Text(state.name) }, onClick = {
                                onUpdate(trigger.copy(state = state)); stateExpanded = false
                            })
                        }
                    }
                }
            }
        }
        is Trigger.BatteryLevel -> {
            OutlinedTextField(
                value = trigger.level.toString(),
                onValueChange = { onUpdate(trigger.copy(level = it.toIntOrNull() ?: 0)) },
                label = { Text("Battery level %") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        is Trigger.ScreenStateTrigger -> {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = trigger.state.name, onValueChange = {},
                    label = { Text("Screen state") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), readOnly = true,
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ScreenState.entries.forEach { state ->
                        DropdownMenuItem(text = { Text(state.name) }, onClick = {
                            onUpdate(trigger.copy(state = state)); expanded = false
                        })
                    }
                }
            }
        }
        is Trigger.AppOpened -> {
            OutlinedTextField(
                value = trigger.pkg,
                onValueChange = { onUpdate(trigger.copy(pkg = it)) },
                label = { Text("Package name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        is Trigger.Geofence -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = trigger.lat.toString(),
                    onValueChange = { onUpdate(trigger.copy(lat = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = trigger.lng.toString(),
                    onValueChange = { onUpdate(trigger.copy(lng = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }
            OutlinedTextField(
                value = trigger.radiusM.toString(),
                onValueChange = { onUpdate(trigger.copy(radiusM = it.toDoubleOrNull() ?: 100.0)) },
                label = { Text("Radius (meters)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
        }
        is Trigger.Immediate, is Trigger.Boot, is Trigger.Manual -> {}
        is Trigger.BluetoothDevice -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val bondedDevices = remember {
                runCatching {
                    val bm = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
                    bm?.adapter?.bondedDevices?.map { it.name to it.address } ?: emptyList()
                }.getOrDefault(emptyList())
            }
            OutlinedTextField(
                value = trigger.deviceName ?: "",
                onValueChange = { onUpdate(trigger.copy(deviceName = it.ifBlank { null })) },
                label = { Text("Device name (blank = any)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            if (bondedDevices.isNotEmpty()) {
                Text(
                    "Paired devices:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                bondedDevices.forEach { (name, address) ->
                    TextButton(
                        onClick = { onUpdate(trigger.copy(deviceName = name, deviceAddress = address)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(name, style = MaterialTheme.typography.bodySmall)
                            Text(address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        is Trigger.WifiConnected -> {
            OutlinedTextField(
                value = trigger.ssid ?: "",
                onValueChange = { onUpdate(trigger.copy(ssid = it.ifBlank { null })) },
                label = { Text("SSID (blank = any)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
        }
        is Trigger.CalendarEvent -> {
            OutlinedTextField(
                value = trigger.titleMatch ?: "",
                onValueChange = { onUpdate(trigger.copy(titleMatch = it.ifBlank { null })) },
                label = { Text("Title contains (blank = any)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
        }
    }
}

// ─── Action Row & Picker ─────────────────────────────────────────────────────

@Composable
private fun ActionRow(
    action: Action,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: (Action) -> Unit = {},
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(actionDescription(action), style = MaterialTheme.typography.bodyMedium)
                }
                if (canMoveUp) IconButton(onClick = onMoveUp) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                }
                if (canMoveDown) IconButton(onClick = onMoveDown) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            // Action-specific editors
            when (action) {
                is Action.SendSms -> {
                    OutlinedTextField(
                        value = action.number,
                        onValueChange = { onUpdate(action.copy(number = it)) },
                        label = { Text("Phone number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = action.text,
                        onValueChange = { onUpdate(action.copy(text = it)) },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
                is Action.ShowNotification -> {
                    OutlinedTextField(
                        value = action.title,
                        onValueChange = { onUpdate(action.copy(title = it)) },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = action.text,
                        onValueChange = { onUpdate(action.copy(text = it)) },
                        label = { Text("Text") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }
                is Action.OpenUrl -> {
                    OutlinedTextField(
                        value = action.url,
                        onValueChange = { onUpdate(action.copy(url = it)) },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                is Action.LaunchApp -> {
                    OutlinedTextField(
                        value = action.pkg,
                        onValueChange = { onUpdate(action.copy(pkg = it)) },
                        label = { Text("Package name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                is Action.CopyText -> {
                    OutlinedTextField(
                        value = action.text,
                        onValueChange = { onUpdate(action.copy(text = it)) },
                        label = { Text("Text to copy") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
                else -> {}
            }
        }
    }
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Action") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                actions.forEach { (label, action) ->
                    TextButton(onClick = { onPick(action) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ─── Condition Row & Picker ──────────────────────────────────────────────────

@Composable
private fun ConditionRow(condition: Condition, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(conditionDescription(condition), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Condition") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                conditions.forEach { (label, condition) ->
                    TextButton(onClick = { onPick(condition) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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

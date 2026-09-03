package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.ImportResult
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus
import com.tdvorak.nothingmodes.shizuku.ShizukuPermissionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val shizukuGateway: ShizukuGateway,
    private val store: AutomationStore,
) : ViewModel() {

    private val importExportService = ImportExportService(store)

    private val _capabilities = MutableStateFlow<DeviceCapabilities?>(null)
    val capabilities: StateFlow<DeviceCapabilities?> = _capabilities.asStateFlow()

    private val _shizukuStatus = MutableStateFlow(ShizukuGatewayStatus.NOT_INSTALLED)
    val shizukuStatus: StateFlow<ShizukuGatewayStatus> = _shizukuStatus.asStateFlow()

    private val _permissionResult = MutableStateFlow<ShizukuPermissionResult?>(null)
    val permissionResult: StateFlow<ShizukuPermissionResult?> = _permissionResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _exportReady = MutableStateFlow<String?>(null)
    val exportReady: StateFlow<String?> = _exportReady.asStateFlow()

    fun detect(context: android.content.Context) {
        viewModelScope.launch {
            _capabilities.value = CapabilityDetector(context).detect()
            _shizukuStatus.value = shizukuGateway.status()
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            _permissionResult.value = shizukuGateway.requestPermission(rationaleShown = true)
            _shizukuStatus.value = shizukuGateway.status()
        }
    }

    fun export() {
        viewModelScope.launch {
            val result = importExportService.export()
            _exportReady.value = result.json
        }
    }

    fun import(json: String) {
        viewModelScope.launch {
            _importResult.value = importExportService.import(json)
        }
    }

    fun clearExportReady() { _exportReady.value = null }
    fun clearImportResult() { _importResult.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val caps by viewModel.capabilities.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val permissionResult by viewModel.permissionResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val exportReady by viewModel.exportReady.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.detect(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null && exportReady != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(exportReady!!.toByteArray(Charsets.UTF_8))
            }
            viewModel.clearExportReady()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
            if (json != null) viewModel.import(json)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            caps?.let { capabilities ->
                DeviceCard(capabilities)
                PermissionsCard(context, capabilities)
                ShizukuCard(
                    shizukuStatus = shizukuStatus,
                    permissionResult = permissionResult,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                )
                AboutCard(capabilities)
                ImportExportCard(
                    onExport = {
                        viewModel.export()
                        exportLauncher.launch("nothing-modes-export.json")
                    },
                    onImport = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    importResult = importResult,
                    onDismissResult = { viewModel.clearImportResult() },
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(caps: DeviceCapabilities) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Device", style = MaterialTheme.typography.titleMedium)
            InfoRow("Name", caps.deviceName.ifEmpty { "Unknown" })
            InfoRow("Model", caps.deviceModel)
            InfoRow("Android", "API ${caps.androidVersion}")
            InfoRow("Nothing device", if (caps.isNothingDevice) "Yes" else "No")
        }
    }
}

@Composable
private fun PermissionsCard(context: android.content.Context, caps: DeviceCapabilities) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            PermissionRow("Write Settings", caps.hasWriteSettings) {
                context.startActivity(Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            PermissionRow("Notification Policy", caps.hasNotificationPolicyAccess) {
                context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            PermissionRow("Notification Listener", caps.hasNotificationListenerAccess) {
                context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            PermissionRow("Usage Access", caps.hasUsageAccess) {
                context.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            PermissionRow("Location", caps.hasLocationPermission) {
                context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}

@Composable
private fun ShizukuCard(
    shizukuStatus: ShizukuGatewayStatus,
    permissionResult: ShizukuPermissionResult?,
    onRequestPermission: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Shizuku", style = MaterialTheme.typography.titleMedium)
            val statusText = when (shizukuStatus) {
                ShizukuGatewayStatus.NOT_INSTALLED -> "Not installed"
                ShizukuGatewayStatus.INSTALLED_NOT_RUNNING -> "Installed, not running"
                ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED -> "Running, not authorized"
                ShizukuGatewayStatus.AUTHORIZED -> "Authorized"
                ShizukuGatewayStatus.UNSUPPORTED -> "Unsupported"
            }
            val statusColor = if (shizukuStatus == ShizukuGatewayStatus.AUTHORIZED)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = statusColor)

            if (shizukuStatus == ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED) {
                TextButton(onClick = onRequestPermission) { Text("Request Permission") }
            }

            permissionResult?.let { result ->
                val resultText = when (result) {
                    ShizukuPermissionResult.GRANTED -> "Permission granted"
                    ShizukuPermissionResult.DENIED -> "Permission denied"
                    ShizukuPermissionResult.RATIONALE_REQUIRED -> "Rationale required"
                    ShizukuPermissionResult.UNAVAILABLE -> "Unavailable"
                }
                Text(
                    resultText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutCard(caps: DeviceCapabilities) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Capabilities", style = MaterialTheme.typography.titleMedium)
            InfoRow("Flashlight", if (caps.hasFlashlight) "Yes" else "No")
            InfoRow("Vibrator", if (caps.hasVibrator) "Yes" else "No")
            InfoRow("Glyph Stripe", if (caps.hasGlyphLightStripe) "Yes" else "No")
            InfoRow("Glyph Matrix", if (caps.hasGlyphMatrix) "Yes" else "No")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            "$label: ${if (granted) "Granted" else "Not granted"}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!granted) {
            TextButton(onClick = onOpenSettings) { Text("Grant") }
        }
    }
}

@Composable
private fun ImportExportCard(
    onExport: () -> Unit,
    onImport: () -> Unit,
    importResult: ImportResult?,
    onDismissResult: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Backup", style = MaterialTheme.typography.titleMedium)
            Text(
                "Export automations to a JSON file or import from a backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onExport) { Text("Export") }
                TextButton(onClick = onImport) { Text("Import") }
            }
            importResult?.let { result ->
                HorizontalDivider()
                Text(
                    "Imported: ${result.imported}, Skipped: ${result.skipped}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (result.errors.isNotEmpty()) {
                    result.errors.forEach { error ->
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismissResult) { Text("Dismiss") }
            }
        }
    }
}

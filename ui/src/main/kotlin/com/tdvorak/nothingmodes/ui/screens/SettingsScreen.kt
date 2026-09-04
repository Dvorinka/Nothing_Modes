package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
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
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingGhostButton
import com.tdvorak.nothingmodes.ui.theme.NothingInfoRow
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingSecondaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSegmentedControl
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingStatusDot
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.theme.ThemeManager
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus
import com.tdvorak.nothingmodes.shizuku.ShizukuPermissionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
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
            val result = withContext(Dispatchers.IO) { importExportService.export() }
            _exportReady.value = result.json
        }
    }

    fun import(json: String) {
        viewModelScope.launch {
            _importResult.value = withContext(Dispatchers.IO) { importExportService.import(json) }
        }
    }

    fun clearExportReady() { _exportReady.value = null }
    fun clearImportResult() { _importResult.value = null }

    fun writeExportToFile(uri: Uri) {
        viewModelScope.launch {
            val json = _exportReady.value ?: return@launch
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(json.toByteArray(Charsets.UTF_8))
                }
            }
            clearExportReady()
        }
    }

    fun readImportFromFile(uri: Uri) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                }
            }
            if (json != null) import(json)
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOnboarding: () -> Unit = {},
    onGlyphPreview: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val caps by viewModel.capabilities.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val permissionResult by viewModel.permissionResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val exportReady by viewModel.exportReady.collectAsState()

    LaunchedEffect(Unit) { viewModel.detect(context) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.writeExportToFile(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.readImportFromFile(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Settings",
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
            // Hero — title in Doto
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = com.tdvorak.nothingmodes.ui.theme.Doto,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            caps?.let { capabilities ->
                // ── Device ────────────────────────────────────────────────
                NothingSectionHeader(text = "Device")
                NothingDivider()
                NothingInfoRow(label = "Name", value = capabilities.deviceName.ifEmpty { "Unknown" })
                NothingDivider()
                NothingInfoRow(label = "Model", value = capabilities.deviceModel)
                NothingDivider()
                NothingInfoRow(label = "Android", value = "API ${capabilities.androidVersion}")
                NothingDivider()
                NothingInfoRow(
                    label = "Nothing Device",
                    value = if (capabilities.isNothingDevice) "Yes" else "No",
                    valueColor = if (capabilities.isNothingDevice) NothingColors.success
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // ── Permissions ───────────────────────────────────────────
                NothingSectionHeader(text = "Permissions")
                PermissionRow("Write Settings", capabilities.hasWriteSettings) {
                    context.startActivity(Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                PermissionRow("Notification Policy", capabilities.hasNotificationPolicyAccess) {
                    context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                PermissionRow("Notification Listener", capabilities.hasNotificationListenerAccess) {
                    context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                PermissionRow("Usage Access", capabilities.hasUsageAccess) {
                    context.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                PermissionRow("Location", capabilities.hasLocationPermission) {
                    context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }

                // ── Shizuku ───────────────────────────────────────────────
                NothingSectionHeader(text = "Shizuku")
                NothingDivider()
                val shizukuStatusText = when (shizukuStatus) {
                    ShizukuGatewayStatus.NOT_INSTALLED -> "Not Installed"
                    ShizukuGatewayStatus.INSTALLED_NOT_RUNNING -> "Not Running"
                    ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED -> "Not Authorized"
                    ShizukuGatewayStatus.AUTHORIZED -> "Authorized"
                    ShizukuGatewayStatus.UNSUPPORTED -> "Unsupported"
                }
                val shizukuColor = when (shizukuStatus) {
                    ShizukuGatewayStatus.AUTHORIZED -> NothingColors.success
                    ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED -> NothingColors.warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = NothingSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NothingLabel(text = "Status")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NothingStatusDot(color = shizukuColor, size = 6f)
                        Spacer(modifier = Modifier.padding(end = 6.dp))
                        Text(
                            text = shizukuStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = shizukuColor,
                            fontFamily = SpaceMono,
                        )
                    }
                }
                if (shizukuStatus == ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED) {
                    NothingSecondaryButton(
                        text = "Authorize",
                        onClick = { viewModel.requestShizukuPermission() },
                        modifier = Modifier.padding(vertical = NothingSpacing.sm),
                    )
                }
                permissionResult?.let { result ->
                    val resultText = when (result) {
                        ShizukuPermissionResult.GRANTED -> "[ GRANTED ]"
                        ShizukuPermissionResult.DENIED -> "[ DENIED ]"
                        ShizukuPermissionResult.RATIONALE_REQUIRED -> "[ RATIONALE REQUIRED ]"
                        ShizukuPermissionResult.UNAVAILABLE -> "[ UNAVAILABLE ]"
                    }
                    val resultColor = when (result) {
                        ShizukuPermissionResult.GRANTED -> NothingColors.success
                        else -> NothingColors.accent
                    }
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.labelSmall,
                        color = resultColor,
                        fontFamily = SpaceMono,
                        modifier = Modifier.padding(top = NothingSpacing.xs),
                    )
                }

                // ── Device Admin ──────────────────────────────────────────
                NothingSectionHeader(text = "Device Admin")
                DeviceAdminSection(context)

                // ── Capabilities ──────────────────────────────────────────
                NothingSectionHeader(text = "Capabilities")
                NothingDivider()
                NothingInfoRow(
                    label = "Flashlight",
                    value = if (capabilities.hasFlashlight) "Yes" else "No",
                    valueColor = if (capabilities.hasFlashlight) NothingColors.success
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NothingDivider()
                NothingInfoRow(
                    label = "Vibration",
                    value = if (capabilities.hasVibrator) "Yes" else "No",
                    valueColor = if (capabilities.hasVibrator) NothingColors.success
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NothingDivider()
                NothingInfoRow(
                    label = "Glyph Stripe",
                    value = if (capabilities.hasGlyphLightStripe) "Yes" else "No",
                    valueColor = if (capabilities.hasGlyphLightStripe) NothingColors.success
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NothingDivider()
                NothingInfoRow(
                    label = "Glyph Matrix",
                    value = if (capabilities.hasGlyphMatrix) "Yes" else "No",
                    valueColor = if (capabilities.hasGlyphMatrix) NothingColors.success
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // ── Theme ─────────────────────────────────────────────────
                NothingSectionHeader(text = "Theme")
                ThemeSection()

                // ── Backup ────────────────────────────────────────────────
                NothingSectionHeader(text = "Backup")
                NothingDivider()
                Text(
                    text = "Export automations to JSON or import from backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = NothingSpacing.md),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    NothingSecondaryButton(
                        text = "Export",
                        onClick = {
                            viewModel.export()
                            exportLauncher.launch("nothing-modes-export.json")
                        },
                        modifier = Modifier.weight(1f),
                    )
                    NothingSecondaryButton(
                        text = "Import",
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                    )
                }
                importResult?.let { result ->
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    Text(
                        text = "Imported: ${result.imported}  Skipped: ${result.skipped}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = SpaceMono,
                    )
                    result.errors.forEach { error ->
                        Text(
                            text = "[ ERROR: $error ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingColors.accent,
                            fontFamily = SpaceMono,
                        )
                    }
                    NothingGhostButton(
                        text = "Dismiss",
                        onClick = { viewModel.clearImportResult() },
                    )
                }

                // ── Misc ──────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
                NothingDivider()
                NothingGhostButton(
                    text = "Setup Guide",
                    onClick = onOnboarding,
                )
                NothingDivider()
                NothingGhostButton(
                    text = "Glyph Preview",
                    onClick = onGlyphPreview,
                )
                NothingDivider()

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onOpenSettings: () -> Unit) {
    NothingDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NothingSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NothingLabel(text = label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            NothingStatusDot(
                color = if (granted) NothingColors.success else NothingColors.accent,
                size = 6f,
            )
            Spacer(modifier = Modifier.padding(end = 6.dp))
            Text(
                text = if (granted) "Granted" else "Not Granted",
                style = MaterialTheme.typography.bodyMedium,
                color = if (granted) NothingColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
            if (!granted) {
                Text(
                    text = "GRANT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onOpenSettings)
                        .padding(start = NothingSpacing.sm),
                )
            }
        }
    }
}

@Composable
private fun ThemeSection() {
    val themeManager = ThemeManager.instance
    val mode by themeManager.mode.collectAsState()
    val modes = ThemeManager.ThemeMode.entries.toList()
    val labels = modes.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    val selectedIndex = modes.indexOf(mode)

    NothingSegmentedControl(
        segments = labels,
        selectedIndex = selectedIndex,
        onSelected = { index -> themeManager.setMode(modes[index]) },
        modifier = Modifier.padding(vertical = NothingSpacing.sm),
    )
}

@Composable
private fun DeviceAdminSection(context: android.content.Context) {
    val dm = remember { context.getSystemService(android.app.admin.DevicePolicyManager::class.java) }
    val comp = remember {
        android.content.ComponentName(
            context.packageName,
            "com.tdvorak.nothingmodes.automation.lifecycle.NothingDeviceAdminReceiver",
        )
    }
    var isActive by remember { mutableStateOf(dm.isAdminActive(comp)) }

    NothingDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NothingSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NothingLabel(text = "Status")
        Row(verticalAlignment = Alignment.CenterVertically) {
            NothingStatusDot(
                color = if (isActive) NothingColors.success else NothingColors.accent,
                size = 6f,
            )
            Spacer(modifier = Modifier.padding(end = 6.dp))
            Text(
                text = if (isActive) "Active" else "Not Active",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) NothingColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
        }
    }
    NothingDivider()
    Text(
        text = "Required for Lock Screen action.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = NothingSpacing.md),
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isActive = dm.isAdminActive(comp)
    }

    if (isActive) {
        NothingSecondaryButton(
            text = "Revoke",
            onClick = {
                dm.removeActiveAdmin(comp)
                isActive = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NothingSpacing.md),
        )
    } else {
        NothingSecondaryButton(
            text = "Activate",
            onClick = {
                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                    putExtra(
                        android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Nothing Modes needs device admin to lock the screen via automation.",
                    )
                }
                launcher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = NothingSpacing.md),
        )
    }
}

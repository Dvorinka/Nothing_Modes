package com.tdvorak.nothingmodes.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val shizukuGateway: ShizukuGateway,
) : ViewModel() {

    private val _capabilities = MutableStateFlow<DeviceCapabilities?>(null)
    val capabilities: StateFlow<DeviceCapabilities?> = _capabilities.asStateFlow()

    private val _shizukuStatus = MutableStateFlow(ShizukuGatewayStatus.NOT_INSTALLED)
    val shizukuStatus: StateFlow<ShizukuGatewayStatus> = _shizukuStatus.asStateFlow()

    fun detect(context: android.content.Context) {
        viewModelScope.launch {
            _capabilities.value = CapabilityDetector(context).detect()
            _shizukuStatus.value = shizukuGateway.status()
        }
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val caps by viewModel.capabilities.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.detect(context) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Welcome to Nothing Modes", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Automate your Nothing phone with modes, routines, and Glyph integration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            caps?.let { capabilities ->
                StepCard(
                    step = 1,
                    title = "Write Settings",
                    description = "Required to change brightness, screen timeout, and other system settings.",
                    done = capabilities.hasWriteSettings,
                    actionText = "Grant",
                    onAction = {
                        context.startActivity(Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                )

                StepCard(
                    step = 2,
                    title = "Notification Policy",
                    description = "Required to control Do Not Dist mode.",
                    done = capabilities.hasNotificationPolicyAccess,
                    actionText = "Grant",
                    onAction = {
                        context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                )

                StepCard(
                    step = 3,
                    title = "Notification Access",
                    description = "Required for notification-based triggers.",
                    done = capabilities.hasNotificationListenerAccess,
                    actionText = "Grant",
                    onAction = {
                        context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                )

                StepCard(
                    step = 4,
                    title = "Usage Access",
                    description = "Required for app-opened triggers.",
                    done = capabilities.hasUsageAccess,
                    actionText = "Grant",
                    onAction = {
                        context.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                )

                StepCard(
                    step = 5,
                    title = "Location",
                    description = "Required for geofence triggers and WiFi SSID detection.",
                    done = capabilities.hasLocationPermission,
                    actionText = "Grant",
                    onAction = {
                        context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                )

                if (capabilities.isNothingDevice) {
                    StepCard(
                        step = 6,
                        title = "Glyph",
                        description = "Glyph light stripe: ${if (capabilities.hasGlyphLightStripe) "Available" else "Unavailable"}. Glyph Matrix: ${if (capabilities.hasGlyphMatrix) "Available" else "Unavailable"}.",
                        done = capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix,
                        actionText = "Test",
                        onAction = { /* Glyph test would go here */ },
                    )
                }

                StepCard(
                    step = if (capabilities.isNothingDevice) 7 else 6,
                    title = "Shizuku (Optional)",
                    description = "Enables Wi-Fi, Bluetooth, and mobile data toggles. Install Shizuku from Play Store or GitHub.",
                    done = shizukuStatus == ShizukuGatewayStatus.AUTHORIZED,
                    actionText = when (shizukuStatus) {
                        ShizukuGatewayStatus.NOT_INSTALLED -> "Install"
                        ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED -> "Authorize"
                        else -> "Settings"
                    },
                    onAction = {
                        if (shizukuStatus == ShizukuGatewayStatus.NOT_INSTALLED) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases")))
                        }
                    },
                )
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Get Started")
            }
        }
    }
}

@Composable
private fun StepCard(
    step: Int,
    title: String,
    description: String,
    done: Boolean,
    actionText: String,
    onAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$step",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (done) {
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!done) {
                TextButton(onClick = onAction) { Text(actionText) }
            }
        }
    }
}

package com.tdvorak.nothingmodes.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tdvorak.nothingmodes.ui.components.PermissionDisclosureDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.data.crash.CrashReporting
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingStatusDot
import com.tdvorak.nothingmodes.ui.theme.NothingToggle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.provider.Settings as AndroidSettings

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
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
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-detect capabilities every time the screen resumes so permission
    // grants done in system settings are reflected immediately.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.detect(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ -> viewModel.detect(context) }
    var showLocationDisclosure by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NothingSpacing.md),
        ) {
            // Hero — Doto headline, vast gap to content
            Text(
                text = "Nothing Modes",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = NothingFonts.doto(),
                modifier = Modifier.padding(top = NothingSpacing.xxxl),
            )
            Text(
                text = "Automate your Nothing phone with modes and Glyph integration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = NothingSpacing.sm),
            )

            // Vast gap — new context
            Spacer(modifier = Modifier.height(NothingSpacing.xxl))

            NothingSectionHeader(text = "Setup")

            caps?.let { capabilities ->
                NothingCardLarge {
                    OnboardingStep(
                        step = 1,
                        title = "Write Settings",
                        description = "Brightness, screen timeout, system settings.",
                        done = capabilities.hasWriteSettings,
                        onAction = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                    )
                    OnboardingStep(
                        step = 2,
                        title = "Notification Policy",
                        description = "Control Do Not Disturb mode.",
                        done = capabilities.hasNotificationPolicyAccess,
                        onAction = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                    )
                    OnboardingStep(
                        step = 3,
                        title = "Notification Access",
                        description = "Notification-based triggers.",
                        done = capabilities.hasNotificationListenerAccess,
                        onAction = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                    )
                    OnboardingStep(
                        step = 4,
                        title = "Usage Access",
                        description = "App-opened triggers.",
                        done = capabilities.hasUsageAccess,
                        onAction = {
                            context.startActivity(
                                Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                    )
                    OnboardingStep(
                        step = 5,
                        title = "Location",
                        description = "Geofence triggers, WiFi SSID detection.",
                        done = capabilities.hasLocationPermission,
                        onAction = {
                            showLocationDisclosure = true
                        },
                    )

                    if (capabilities.isNothingDevice) {
                        OnboardingStep(
                            step = 6,
                            title = "Glyph",
                            description = "Stripe: ${if (capabilities.hasGlyphLightStripe) "Available" else "Unavailable"}. Matrix: ${if (capabilities.hasGlyphMatrix) "Available" else "Unavailable"}.",
                            done = capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix,
                            onAction = {},
                        )
                    }

                    OnboardingStep(
                        step = if (capabilities.isNothingDevice) 7 else 6,
                        title = "Shizuku (Optional)",
                        description = "Wi-Fi, Bluetooth, mobile data toggles.",
                        done = shizukuStatus == ShizukuGatewayStatus.AUTHORIZED,
                        onAction = {
                            if (shizukuStatus == ShizukuGatewayStatus.NOT_INSTALLED) {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases/latest")),
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xxl))

            NothingSectionHeader(text = "Privacy")
            NothingCard {
                val crashEnabled by CrashReporting.enabled.collectAsState()
                NothingListRow(
                    title = "Crash & error reports",
                    subtitle =
                        "Optional — anonymous reports go to the developer's own server, " +
                            "no third-party SDKs. Off unless you turn it on; change anytime in Settings.",
                    trailing = {
                        NothingToggle(
                            checked = crashEnabled,
                            onCheckedChange = { CrashReporting.setEnabled(it) },
                        )
                    },
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xxl))
            NothingPillButton(
                text = "Get Started",
                onClick = onComplete,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = NothingSpacing.lg),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
        }

        if (showLocationDisclosure) {
            PermissionDisclosureDialog(
                title = "Location disclosure",
                body =
                    "Nothing Modes uses your location to run geofence automations when you enter or leave an area you define. " +
                        "This works in the background so your rules fire even when the app is closed. " +
                        "Your location is processed on your device and is never sold, shared, or transmitted.",
                onConfirm = {
                    showLocationDisclosure = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onDismiss = { showLocationDisclosure = false },
            )
        }
    }
}

@Composable
private fun OnboardingStep(
    step: Int,
    title: String,
    description: String,
    done: Boolean,
    onAction: () -> Unit,
) {
    NothingListRow(
        title = title,
        subtitle = description,
        onClick = onAction,
        leading = {
            NothingIconCircle(size = 40f) {
                Text(
                    text = String.format("%02d", step),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        },
        trailing = {
            if (done) {
                NothingStatusDot(
                    color = MaterialTheme.colorScheme.primary,
                    size = 6f,
                )
            } else {
                Text(
                    text = "GRANT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                    modifier =
                        Modifier
                            .clickable(onClick = onAction)
                            .padding(horizontal = NothingSpacing.sm),
                )
            }
        },
    )
}

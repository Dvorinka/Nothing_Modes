package com.tdvorak.nothingmodes.ui.components

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

/**
 * Gates [content] behind one or more runtime permissions.
 * Shows a rationale card with a grant button while any permission is missing.
 * If [disclosure] is set, [PermissionDisclosureDialog] is shown before the
 * system permission prompt, as required by Google Play for sensitive permissions.
 */
@Composable
fun PermissionGate(
    permissions: List<String>,
    rationale: String,
    modifier: Modifier = Modifier,
    disclosure: String? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var allGranted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }
    var showDisclosure by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            allGranted = result.values.all { it }
        }

    if (allGranted) {
        content()
    } else {
        NothingCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(NothingSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            ) {
                Text(
                    text = "PERMISSION REQUIRED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.mono(),
                )
                Text(
                    text = rationale,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                NothingPillButton(
                    text = "Grant permission",
                    onClick = {
                        if (disclosure.isNullOrBlank()) {
                            launcher.launch(permissions.toTypedArray())
                        } else {
                            showDisclosure = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showDisclosure && disclosure != null) {
        PermissionDisclosureDialog(
            title = "Permission disclosure",
            body = disclosure,
            onConfirm = {
                showDisclosure = false
                launcher.launch(permissions.toTypedArray())
            },
            onDismiss = { showDisclosure = false },
        )
    }
}

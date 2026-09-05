package com.tdvorak.nothingmodes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tdvorak.nothingmodes.nav.NothingModesNavHost
import com.tdvorak.nothingmodes.ui.theme.NothingModesThemeDynamic
import com.tdvorak.nothingmodes.update.UpdateStatus
import com.tdvorak.nothingmodes.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NothingModesThemeDynamic {
                NothingModesNavHost()

                val updateInfo by updateViewModel.updateInfo.collectAsState()
                val updateStatus by updateViewModel.updateStatus.collectAsState()
                var updateDismissed by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    updateViewModel.checkForUpdate()
                }

                if (updateInfo != null && !updateDismissed && updateStatus == UpdateStatus.AVAILABLE) {
                    val canInstall = updateViewModel.hasInstallPermission()
                    AlertDialog(
                        onDismissRequest = { updateDismissed = true },
                        title = { Text("Update available") },
                        text = {
                            Text(
                                "Nothing Modes ${updateInfo?.displayVersion} is available.\n\n" +
                                    (updateInfo?.releaseNotes?.takeIf { it.isNotBlank() }?.let { "$it\n\n" } ?: "") +
                                    if (canInstall) "Your automations and settings will be kept." else "Install permission is required to update.",
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    updateDismissed = true
                                    if (canInstall) {
                                        updateInfo?.let { updateViewModel.startDownload(it) }
                                    } else {
                                        updateViewModel.openInstallPermissionSettings()
                                    }
                                },
                            ) {
                                Text(if (canInstall) "Download" else "Open settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateDismissed = true }) {
                                Text("Later")
                            }
                        },
                    )
                }
            }
        }
    }
}

package com.tdvorak.nothingmodes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import com.tdvorak.nothingmodes.data.community.CommunityApi
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.nav.NothingModesNavHost
import com.tdvorak.nothingmodes.nothing.CustomGlyphStore
import com.tdvorak.nothingmodes.ui.theme.NothingModesThemeDynamic
import com.tdvorak.nothingmodes.update.UpdateStatus
import com.tdvorak.nothingmodes.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val updateViewModel: UpdateViewModel by viewModels()

    @Inject
    lateinit var automationStore: AutomationStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importDesignIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        importDesignIntent(intent)
    }

    /**
     * Handles:
     * - Glyph design JSON shared to or opened with this app (application/json).
     * - One-tap community imports from the website (nothingmodes://import?type=...&id=...).
     */
    private fun importDesignIntent(intent: Intent?) {
        val uri: Uri? =
            when (intent?.action) {
                Intent.ACTION_VIEW -> intent.data
                Intent.ACTION_SEND ->
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                else -> null
            }
        uri ?: return

        if (uri.scheme == "nothingmodes" && uri.host == "import") {
            val type = uri.getQueryParameter("type") ?: "template"
            val id = uri.getQueryParameter("id") ?: return
            importCommunityItem(type, id)
            return
        }

        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()?.let { json ->
            val store = CustomGlyphStore(this)
            val name = store.import(json, uri.lastPathSegment?.substringBeforeLast('.'))
            Toast.makeText(
                this,
                if (name != null) "Glyph design imported as $name" else "Not a glyph design file",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun importCommunityItem(type: String, id: String) {
        lifecycleScope.launch {
            val result =
                runCatching {
                    val payload = CommunityApi.fetchItem(id)
                    when (type) {
                        "glyph" -> {
                            val store = CustomGlyphStore(this@MainActivity)
                            val name = store.import(payload.toString(), null)
                            if (name != null) "Glyph design imported as $name" else "Invalid glyph design"
                        }
                        else -> {
                            val version =
                                runCatching {
                                    packageManager.getPackageInfo(packageName, 0).versionName
                                }.getOrNull().orEmpty()
                            val service = ImportExportService(automationStore, version)
                            val importResult = service.import(payload.toString(), overwrite = true)
                            if (importResult.imported > 0) {
                                "Imported ${importResult.imported} mode(s)"
                            } else {
                                "Import failed: ${importResult.errors.firstOrNull() ?: "unknown"}"
                            }
                        }
                    }
                }.getOrNull() ?: "Could not fetch item from the community library"

            Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
        }
    }
}

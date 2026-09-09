package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.ui.R
import com.tdvorak.nothingmodes.ui.prefs.CreatorPreferences
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingStatusDot
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AutomationDetailViewModel
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
        private val store: AutomationStore,
    ) : ViewModel() {
        private val _automation = MutableStateFlow<Automation?>(null)
        val automation: StateFlow<Automation?> = _automation.asStateFlow()

        fun runNow() {
            val current = _automation.value ?: return
            val intent =
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_MANUAL
                    putExtra(AutomationService.EXTRA_MANUAL_ID, current.id.value)
                }
            ContextCompat.startForegroundService(context, intent)
        }

        fun load(id: String) {
            viewModelScope.launch {
                _automation.value =
                    store.get(
                        com.tdvorak.nothingmodes.engine.model
                            .AutomationId(id),
                    )
            }
        }

        fun share() {
            val current = _automation.value ?: return
            viewModelScope.launch {
                runCatching {
                    val appVersion =
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull().orEmpty()
                    val creator = CreatorPreferences(context).get()
                    val export =
                        com.tdvorak.nothingmodes.engine.runtime
                            .ImportExportService(store, appVersion)
                            .export(listOf(current.id), creator)
                    val share =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "Nothing Modes mode: ${current.name}")
                            putExtra(Intent.EXTRA_TEXT, export.json)
                        }
                    val chooser = Intent.createChooser(share, "Share mode")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }.onFailure {
                    Toast.makeText(context, "Share failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun submitToCatalog() {
            val current = _automation.value ?: return
            viewModelScope.launch {
                runCatching {
                    val appVersion =
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull().orEmpty()
                    val creator = CreatorPreferences(context).get()
                    val export = ImportExportService(store, appVersion).export(listOf(current.id), creator)

                    val submissionDir = File(context.cacheDir, "submissions")
                    submissionDir.mkdirs()
                    val file = File(submissionDir, "${current.id.value}.json")
                    file.writeText(export.json)

                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )

                    val email = context.getString(R.string.template_submission_email)
                    val body =
                        """
                        Hello,

                        I would like to submit the attached Nothing Modes mode for the public template catalog.

                        Mode name: ${current.name}
                        License: ${creator.license}
                        Created by: ${creator.displayName.ifBlank { "Anonymous" }}
                        Handle/email: ${creator.handle}

                        Please review the attached JSON.

                        Kind regards
                        """.trimIndent()

                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                            putExtra(Intent.EXTRA_SUBJECT, "[Nothing Modes Template] ${current.name}")
                            putExtra(Intent.EXTRA_TEXT, body)
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    val chooser = Intent.createChooser(intent, "Submit mode")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }.onFailure {
                    Toast.makeText(context, "Submit failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /** Save this automation into the local "My templates" collection. */
        fun saveAsTemplate() {
            val current = _automation.value ?: return
            viewModelScope.launch {
                runCatching {
                    val dir = File(context.filesDir, "user_templates").apply { mkdirs() }
                    val template =
                        com.tdvorak.nothingmodes.engine.model.UserTemplate(
                            id = current.id.value,
                            name = current.name,
                            automations = listOf(current),
                        )
                    File(dir, "${template.id}.json")
                        .writeText(
                            com.tdvorak.nothingmodes.engine.model.EngineJson.json
                                .encodeToString(
                                    com.tdvorak.nothingmodes.engine.model.UserTemplate.serializer(),
                                    template,
                                ),
                        )
                }.onSuccess {
                    Toast.makeText(context, "Saved to My templates", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(context, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun delete(onDeleted: () -> Unit) {
            val current = _automation.value ?: return
            viewModelScope.launch {
                store.delete(current.id)
                onDeleted()
            }
        }

        fun duplicate(onDuplicated: () -> Unit) {
            val current = _automation.value ?: return
            viewModelScope.launch {
                val copy =
                    current.copy(
                        id =
                            com.tdvorak.nothingmodes.engine.model.AutomationId(
                                "${current.id.value}-copy-${System.currentTimeMillis()}",
                            ),
                        name = "${current.name} (copy)",
                        enabled = false,
                    )
                store.save(copy)
                onDuplicated()
            }
        }
    }

@Composable
fun AutomationDetailScreen(
    automationId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: AutomationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(automationId) { viewModel.load(automationId) }
    val automation by viewModel.automation.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Detail",
                onBack = onBack,
                actions =
                    listOf(
                        TopBarAction("Run", icon = Icons.Filled.PlayArrow, accent = true, onClick = { viewModel.runNow() }),
                        TopBarAction("Share", icon = Icons.Filled.Share, onClick = { viewModel.share() }),
                        TopBarAction("Edit", icon = Icons.Filled.Edit, onClick = onEdit),
                        TopBarAction("Template", icon = Icons.Filled.Bookmark, onClick = { viewModel.saveAsTemplate() }),
                        TopBarAction("Copy", icon = Icons.Filled.ContentCopy, onClick = { viewModel.duplicate(onBack) }),
                        TopBarAction("Delete", icon = Icons.Filled.Delete, onClick = { viewModel.delete(onBack) }),
                    ),
            )
        },
    ) { padding ->
        val data = automation
        if (data == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "[ LOADING... ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = NothingSpacing.md),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                val statusText =
                    when (data.status) {
                        AutomationStatus.ARMED -> "ARMED"
                        AutomationStatus.DISABLED -> "DISABLED"
                        AutomationStatus.PENDING_APPROVAL -> "PENDING"
                        AutomationStatus.NEEDS_REVIEW -> "REVIEW"
                    }
                val statusColor =
                    when (data.status) {
                        AutomationStatus.ARMED -> MaterialTheme.colorScheme.primary
                        AutomationStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        AutomationStatus.PENDING_APPROVAL -> NothingColors.accent
                        AutomationStatus.NEEDS_REVIEW -> NothingColors.accent
                    }

                NothingCardLarge(
                    modifier = Modifier.padding(top = NothingSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingIconCircle(size = 56f) {
                            Text(
                                text =
                                    data.name
                                        .firstOrNull()
                                        ?.uppercaseChar()
                                        ?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                        Spacer(modifier = Modifier.width(NothingSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = data.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = NothingFonts.doto(),
                            )
                            Spacer(modifier = Modifier.height(NothingSpacing.xs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NothingStatusDot(color = statusColor, size = 6f)
                                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                                Text(
                                    text = "MODE · $statusText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = NothingFonts.mono(),
                                )
                            }
                        }
                    }

                    if (!data.enabled) {
                        Spacer(modifier = Modifier.height(NothingSpacing.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NothingStatusDot(color = NothingColors.accent, size = 6f)
                            Spacer(modifier = Modifier.width(NothingSpacing.sm))
                            Text(
                                text = "DISABLED — open the editor to enable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NothingColors.accent,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xl))

                NothingCardLarge {
                    NothingSectionHeader(text = "Trigger")
                    NothingListRow(
                        title = triggerDescription(data.trigger),
                        subtitle = "Tap to reconfigure",
                        onClick = onEdit,
                        leading = {
                            NothingIconCircle(size = 44f, accent = true) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(NothingSpacing.md))

                    NothingSectionHeader(text = "Actions")
                    if (data.actions.isEmpty()) {
                        NothingDivider()
                        Text(
                            text = "No actions configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        data.actions.forEachIndexed { index, action ->
                            NothingDivider()
                            NothingListRow(
                                title = actionDescription(action),
                                onClick = onEdit,
                                leading = {
                                    NothingIconCircle(size = 40f) {
                                        Text(
                                            text = String.format("%02d", index + 1),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = NothingFonts.mono(),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                NothingCard {
                    NothingPillButton(
                        text = "Submit to public catalog",
                        onClick = { viewModel.submitToCatalog() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            }
        }
    }
}

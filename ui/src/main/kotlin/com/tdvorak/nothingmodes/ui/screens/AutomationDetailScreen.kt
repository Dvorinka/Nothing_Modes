package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tdvorak.nothingmodes.data.community.CommunityApi
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.actionDescription
import com.tdvorak.nothingmodes.engine.model.affectedSettings
import com.tdvorak.nothingmodes.engine.model.canRestore
import com.tdvorak.nothingmodes.engine.model.hasStateLifecycle
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.ui.prefs.CreatorPreferences
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingStatusDot
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.TopBarAction
import com.tdvorak.nothingmodes.ui.util.rememberUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
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

        /** Shares the mode as a downloadable .json file via FileProvider. */
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
                    val dir = File(context.cacheDir, "mode_exports").apply { mkdirs() }
                    val safeName = current.name.replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "mode" }
                    val file = File(dir, "$safeName.nothingmode.json")
                    file.writeText(export.json)
                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                    val share =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Nothing Modes mode: ${current.name}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    val chooser = Intent.createChooser(share, "Save or share mode file")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }.onFailure {
                    Toast.makeText(context, "Share failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val creatorProfile = CreatorPreferences(context)

        private val _shareOpen = MutableStateFlow(false)
        val shareOpen: StateFlow<Boolean> = _shareOpen.asStateFlow()

        private val _sharing = MutableStateFlow(false)
        val sharing: StateFlow<Boolean> = _sharing.asStateFlow()

        private val _shareResult = MutableStateFlow<CommunityApi.SubmitResult?>(null)
        val shareResult: StateFlow<CommunityApi.SubmitResult?> = _shareResult.asStateFlow()

        fun stageShare() {
            _shareResult.value = null
            _shareOpen.value = true
        }

        fun dismissShare() {
            _shareOpen.value = false
            _shareResult.value = null
        }

        /**
         * Push this mode to the public catalog — POSTs an export bundle to
         * the website's /api/share (queues for admin review) and keeps a
         * local copy in "My templates" once accepted into the queue.
         */
        fun publish(
            title: String,
            description: String,
            handle: String,
            email: String,
            github: String,
        ) {
            val current = _automation.value ?: return
            viewModelScope.launch {
                _sharing.value = true
                try {
                    val appVersion =
                        runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        }.getOrNull().orEmpty()
                    val export =
                        ImportExportService(store, appVersion)
                            .export(listOf(current.id), creatorProfile.get())
                    val payload = EngineJson.json.parseToJsonElement(export.json)
                    val result =
                        CommunityApi.submit(
                            type = "template",
                            title = title,
                            description = description,
                            handle = handle,
                            email = email,
                            github = github,
                            payload = payload,
                        )
                    _shareResult.value = result
                    if (result is CommunityApi.SubmitResult.Queued) {
                        saveAsTemplate()
                    }
                } catch (e: Exception) {
                    _shareResult.value =
                        CommunityApi.SubmitResult.Failed(e.message ?: "submit failed")
                }
                _sharing.value = false
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
                                    com.tdvorak.nothingmodes.engine.model.UserTemplate
                                        .serializer(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDetailScreen(
    automationId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: AutomationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(automationId) { viewModel.load(automationId) }
    val automation by viewModel.automation.collectAsState()
    val shareOpen by viewModel.shareOpen.collectAsState()
    val sharing by viewModel.sharing.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    var detailAction by remember { mutableStateOf<com.tdvorak.nothingmodes.engine.model.Action?>(null) }
    val appLabel = rememberAppLabelResolver()
    val appIconResolver = rememberAppIconResolver()
    val units = rememberUnits()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "MODE",
                onBack = onBack,
                actions =
                    listOf(
                        TopBarAction("Run", icon = Icons.Filled.PlayArrow, accent = true, onClick = { viewModel.runNow() }),
                        TopBarAction("Share", icon = Icons.Filled.Share, onClick = { viewModel.share() }),
                        TopBarAction("Edit", icon = Icons.Filled.Edit, onClick = onEdit),
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
                    if (!data.enabled) {
                        "OFF"
                    } else {
                        when (data.status) {
                            AutomationStatus.ARMED -> "ON"
                            AutomationStatus.DISABLED -> "OFF"
                            AutomationStatus.PENDING_APPROVAL -> "PENDING REVIEW"
                            AutomationStatus.NEEDS_REVIEW -> "NEEDS REVIEW"
                        }
                    }
                val statusColor =
                    if (!data.enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        when (data.status) {
                            AutomationStatus.ARMED -> MaterialTheme.colorScheme.primary
                            AutomationStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                            AutomationStatus.PENDING_APPROVAL -> NothingColors.accent
                            AutomationStatus.NEEDS_REVIEW -> NothingColors.accent
                        }
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
                    NothingSectionHeader(text = "If")
                    NothingListRow(
                        title = triggerDescription(data.trigger, units, appLabel),
                        subtitle = "Tap to reconfigure",
                        onClick = onEdit,
                        leading = {
                            NothingIconCircle(size = 44f, accent = true) {
                                val iconBitmap = appPackageOf(data.trigger)?.let(appIconResolver)
                                if (iconBitmap != null) {
                                    Image(
                                        bitmap = iconBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = iconForTrigger(data.trigger),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(NothingSpacing.md))

                    NothingSectionHeader(text = "Only if")
                    val conditionRows = flattenConditions(data.conditions)
                    if (conditionRows.isEmpty()) {
                        NothingDivider()
                        Text(
                            text = "Always — no extra conditions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        conditionRows.forEach { condition ->
                            NothingDivider()
                            NothingListRow(
                                title = conditionDescription(condition, units),
                                subtitle = "Must hold while the mode runs",
                                onClick = onEdit,
                                leading = {
                                    NothingIconCircle(size = 40f) {
                                        val iconBitmap = appPackageOf(condition)?.let(appIconResolver)
                                        if (iconBitmap != null) {
                                            Image(
                                                bitmap = iconBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = iconForCondition(condition),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(NothingSpacing.md))

                    NothingSectionHeader(text = "Then")
                    if (data.actions.isEmpty()) {
                        NothingDivider()
                        Text(
                            text = "No actions configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = NothingSpacing.md),
                        )
                    } else {
                        data.actions.forEach { action ->
                            NothingDivider()
                            NothingListRow(
                                title = actionDescription(action),
                                subtitle = actionFeatureDescription(action) ?: "Tap for details",
                                onClick = { detailAction = action },
                                leading = {
                                    NothingIconCircle(size = 40f) {
                                        val iconBitmap = appPackageOf(action)?.let(appIconResolver)
                                        if (iconBitmap != null) {
                                            Image(
                                                bitmap = iconBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = iconForAction(action),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }

                    // Imported modes can carry endActions on non-lifecycle
                    // triggers (e.g. a Manual MODE stopped via disable) — show
                    // them whenever there is something to display.
                    if (data.trigger.hasStateLifecycle || data.endActions.isNotEmpty()) {
                        val restorable = data.actions.filter { it.canRestore }
                        if (restorable.isNotEmpty() || data.endActions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(NothingSpacing.md))
                            NothingSectionHeader(text = "When it ends")
                            restorable.forEach { action ->
                                val endAction =
                                    data.endActions.firstOrNull {
                                        (it.affectedSettings intersect action.affectedSettings).isNotEmpty()
                                    }
                                val label =
                                    when {
                                        endAction != null -> "Then set to: ${actionDescription(endAction)}"
                                        action.supportsRestore -> "Reverts to previous value"
                                        else -> "Keeps the mode's value"
                                    }
                                NothingDivider()
                                NothingListRow(
                                    title = actionDescription(action),
                                    subtitle = label,
                                    onClick = onEdit,
                                    leading = {
                                        NothingIconCircle(size = 40f) {
                                            Icon(
                                                imageVector = iconForAction(action),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    },
                                )
                            }
                            data.endActions.forEach { endAction ->
                                val claimed =
                                    data.actions.any {
                                        it.canRestore &&
                                            (it.affectedSettings intersect endAction.affectedSettings).isNotEmpty()
                                    }
                                if (!claimed) {
                                    NothingDivider()
                                    NothingListRow(
                                        title = actionDescription(endAction),
                                        subtitle = "Runs when the mode ends",
                                        onClick = onEdit,
                                        leading = {
                                            NothingIconCircle(size = 40f) {
                                                Icon(
                                                    imageVector = iconForAction(endAction),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xl))

                NothingCardLarge {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingIconCircle(size = 44f) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(NothingSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Share this mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Publish it to the public catalog after a quick review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    NothingPillButton(
                        text = "Submit to public catalog",
                        onClick = { viewModel.stageShare() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            }
        }
    }

    detailAction?.let { action ->
        com.tdvorak.nothingmodes.ui.components.InfoDialog(
            title = actionDescription(action),
            text = actionDetailText(action),
            onDismiss = { detailAction = null },
        )
    }

    if (shareOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissShare() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ShareSheet(
                initialTitle = automation?.name.orEmpty(),
                initialDescription = "",
                profile = viewModel.creatorProfile.get(),
                sharing = sharing,
                result = shareResult,
                onPublish = { t, d, h, e, g -> viewModel.publish(t, d, h, e, g) },
                onDismiss = { viewModel.dismissShare() },
            )
        }
    }
}

/** Detail text for the action overview dialog — description plus requirement. */
private fun actionDetailText(action: com.tdvorak.nothingmodes.engine.model.Action): String {
    val lines = mutableListOf<String>()
    actionFeatureDescription(action)?.let { lines += it }
    actionRequirementHint(action)?.let { lines += "\nRequires: $it" }
    return lines.joinToString("\n").ifBlank { "No extra details." }
}

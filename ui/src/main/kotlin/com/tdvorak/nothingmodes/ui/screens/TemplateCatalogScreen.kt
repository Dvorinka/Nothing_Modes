package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.CapabilityResolver
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.CapabilityLabels
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.TemplateIndex
import com.tdvorak.nothingmodes.engine.model.TemplateSummary
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ExportBundle
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.ImportResult
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTag
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

// jarvis: ceiling raw GitHub index; upgrade to a pinned release asset if the repo layout changes
private const val TEMPLATE_BASE_URL =
    "https://raw.githubusercontent.com/Dvorinka/Nothing_Modes/main/templates"

/** A template whose bundle was fetched and inspected, awaiting user confirmation. */
data class PendingTemplateInstall(
    val summary: TemplateSummary,
    val automations: List<Automation>,
    /** Human-readable requirements the device cannot satisfy. */
    val warnings: List<String>,
    /** Human-readable requirements the device can satisfy. */
    val satisfied: List<String>,
    val errors: List<String>,
)

@HiltViewModel
class TemplateCatalogViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: android.content.Context,
        private val store: AutomationStore,
    ) : ViewModel() {
        private val _templates = MutableStateFlow<List<TemplateSummary>>(emptyList())
        val templates: StateFlow<List<TemplateSummary>> = _templates.asStateFlow()

        private val _loading = MutableStateFlow(true)
        val loading: StateFlow<Boolean> = _loading.asStateFlow()

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        private val _pending = MutableStateFlow<PendingTemplateInstall?>(null)
        val pending: StateFlow<PendingTemplateInstall?> = _pending.asStateFlow()

        private val _installing = MutableStateFlow(false)
        val installing: StateFlow<Boolean> = _installing.asStateFlow()

        private val _installed = MutableStateFlow<ImportResult?>(null)
        val installed: StateFlow<ImportResult?> = _installed.asStateFlow()

        private val importExportService =
            ImportExportService(
                store,
                appVersion =
                    runCatching {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    }.getOrNull().orEmpty(),
            )

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _loading.value = true
                _error.value = null
                try {
                    val indexJson = fetchText("$TEMPLATE_BASE_URL/index.json")
                    val index = EngineJson.json.decodeFromString(TemplateIndex.serializer(), indexJson)
                    _templates.value = index.templates
                } catch (e: Exception) {
                    _error.value = "Could not load templates: ${e.message}"
                }
                _loading.value = false
            }
        }

        /** Fetch a template bundle and stage it for review before install. */
        fun select(template: TemplateSummary) {
            viewModelScope.launch {
                _error.value = null
                try {
                    val bundleJson = fetchText("$TEMPLATE_BASE_URL/${template.file}")
                    val preview = importExportService.preview(bundleJson)
                    if (!preview.isSupported) {
                        _pending.value =
                            PendingTemplateInstall(
                                summary = template,
                                automations = emptyList(),
                                warnings = emptyList(),
                                satisfied = emptyList(),
                                errors = preview.errors,
                            )
                        return@launch
                    }

                    val caps = withContext(Dispatchers.IO) { CapabilityDetector(context).detect() }
                    val resolver = CapabilityResolver(caps)
                    val resolution = resolver.resolve(template.id, preview.requiredCapabilities)
                    val missing =
                        resolution.missing
                            .mapNotNull { CapabilityLabels.describe(it).ifBlank { null } }
                    val satisfied =
                        (preview.requiredCapabilities - resolution.missing)
                            .mapNotNull { CapabilityLabels.describe(it).ifBlank { null } }

                    _pending.value =
                        PendingTemplateInstall(
                            summary = template,
                            automations = preview.automations,
                            warnings = missing.distinct(),
                            satisfied = satisfied.distinct(),
                            errors = emptyList(),
                        )
                } catch (e: Exception) {
                    _error.value = "Could not load template: ${e.message}"
                }
            }
        }

        /** Install with fresh IDs and local timezone so templates never collide and fire correctly. */
        fun confirmInstall() {
            val pending = _pending.value ?: return
            viewModelScope.launch {
                _installing.value = true
                try {
                    val now = System.currentTimeMillis()
                    val tz = defaultTimeZone()
                    val localized =
                        pending.automations.mapIndexed { index, automation ->
                            automation.copy(
                                id = AutomationId("${automation.id.value}-$now-$index"),
                                trigger = localizeTz(automation.trigger, tz),
                            )
                        }
                    val bundle =
                        ExportBundle(
                            schemaVersion = 1,
                            exportedAt = now,
                            automations = localized,
                            appVersion = "template:${pending.summary.id}",
                        )
                    _installed.value =
                        importExportService.import(
                            EngineJson.json.encodeToString(bundle),
                        )
                    _pending.value = null
                } catch (e: Exception) {
                    _error.value = "Install failed: ${e.message}"
                }
                _installing.value = false
            }
        }

        fun dismissPending() {
            _pending.value = null
        }

        fun clearInstalled() {
            _installed.value = null
        }

        private fun localizeTz(
            trigger: Trigger,
            tz: String,
        ): Trigger =
            when (trigger) {
                is Trigger.Time -> trigger.copy(tz = tz)
                is Trigger.TimeWindow -> trigger.copy(tz = tz)
                else -> trigger
            }

        private suspend fun fetchText(url: String): String =
            withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                try {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    conn.disconnect()
                }
            }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCatalogScreen(
    onBack: () -> Unit,
    viewModel: TemplateCatalogViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val installing by viewModel.installing.collectAsState()
    val installed by viewModel.installed.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Templates",
                showLeadingDot = true,
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                loading ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "LOADING…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                error != null ->
                    NothingEmptyState(
                        title = "Templates unavailable",
                        description = error.orEmpty(),
                        action = {
                            NothingPrimaryButton(
                                text = "Retry",
                                onClick = { viewModel.refresh() },
                            )
                        },
                    )
                templates.isEmpty() ->
                    NothingEmptyState(
                        title = "No templates yet",
                        description = "Community templates will appear here",
                    )
                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                horizontal = NothingSpacing.md,
                                vertical = NothingSpacing.lg,
                            ),
                        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
                    ) {
                        items(templates, key = { it.id }) { template ->
                            TemplateRow(
                                template = template,
                                onClick = { viewModel.select(template) },
                            )
                        }
                    }
            }
        }
    }

    pending?.let { install ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissPending() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            TemplateInstallSheet(
                install = install,
                installing = installing,
                onConfirm = { viewModel.confirmInstall() },
            )
        }
    }

    installed?.let { result ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearInstalled() },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(NothingSpacing.lg)
                        .navigationBarsPadding(),
            ) {
                NothingLabel(text = "Install complete")
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Text(
                    text = "Imported ${result.imported} routine(s). They start disabled — enable them when ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (result.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    result.errors.forEach { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingColors.accent,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
                NothingPrimaryButton(
                    text = "Done",
                    onClick = { viewModel.clearInstalled() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: TemplateSummary,
    onClick: () -> Unit,
) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingListRow(
            title = template.name,
            subtitle = template.description,
            onClick = onClick,
            leading = {
                NothingIconCircle {
                    Text(
                        text = template.emoji.ifBlank { "•" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            },
        )
        if (template.creator.isNotBlank() || template.tags.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (template.creator.isNotBlank()) {
                    Text(
                        text = "by ${template.creator}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                }
                template.tags.take(3).forEach { tag ->
                    NothingTag(text = tag, active = false)
                }
            }
        }
    }
}

@Composable
private fun TemplateInstallSheet(
    install: PendingTemplateInstall,
    installing: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(NothingSpacing.lg)
                .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
        ) {
            NothingIconCircle {
                Text(
                    text = install.summary.emoji.ifBlank { "•" },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column {
                Text(
                    text = install.summary.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (install.summary.creator.isNotBlank()) {
                    Text(
                        text = "by ${install.summary.creator}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                }
            }
        }

        if (install.summary.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(NothingSpacing.md))
            Text(
                text = install.summary.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(NothingSpacing.md))
        NothingLabel(text = "Includes")
        install.automations.forEach { automation ->
            Text(
                text = "• ${automation.name} — ${triggerDescription(automation.trigger)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (install.errors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.md))
            NothingLabel(text = "Incompatible")
            install.errors.forEach { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }

        if (install.warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.md))
            NothingLabel(text = "May not work on this device")
            install.warnings.forEach { warning ->
                Text(
                    text = "• Requires $warning",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }

        if (install.satisfied.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            Text(
                text = "Supported: ${install.satisfied.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        }

        Spacer(modifier = Modifier.height(NothingSpacing.lg))
        NothingPrimaryButton(
            text = if (installing) "Installing…" else "Install",
            onClick = onConfirm,
            enabled = install.errors.isEmpty() && !installing,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.md))
    }
}

package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.capabilities.CapabilitiesCache
import com.tdvorak.nothingmodes.capabilities.CapabilityResolver
import com.tdvorak.nothingmodes.data.community.CommunityApi
import com.tdvorak.nothingmodes.engine.canonicalJson
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.CapabilityLabels
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.TemplateSummary
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.actionDescription
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ExportBundle
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.ImportResult
import com.tdvorak.nothingmodes.ui.prefs.CreatorPreferences
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingEmptyState
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTag
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import com.tdvorak.nothingmodes.ui.util.rememberUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

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
        private val _userTemplates =
            MutableStateFlow<List<com.tdvorak.nothingmodes.engine.model.UserTemplate>>(emptyList())
        val userTemplates: StateFlow<List<com.tdvorak.nothingmodes.engine.model.UserTemplate>> =
            _userTemplates.asStateFlow()

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

        /** Approved community items from the website library (type=template). */
        private val _library = MutableStateFlow<List<CommunityApi.LibraryItem>>(emptyList())
        val library: StateFlow<List<CommunityApi.LibraryItem>> = _library.asStateFlow()

        private val _sharing = MutableStateFlow(false)
        val sharing: StateFlow<Boolean> = _sharing.asStateFlow()

        private val _shareResult = MutableStateFlow<CommunityApi.SubmitResult?>(null)
        val shareResult: StateFlow<CommunityApi.SubmitResult?> = _shareResult.asStateFlow()

        /** User template currently staged for publishing. */
        private val _shareTarget =
            MutableStateFlow<com.tdvorak.nothingmodes.engine.model.UserTemplate?>(null)
        val shareTarget: StateFlow<com.tdvorak.nothingmodes.engine.model.UserTemplate?> =
            _shareTarget.asStateFlow()

        val creatorProfile = CreatorPreferences(context)

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
                _userTemplates.value = loadUserTemplates()
                _library.value =
                    runCatching { CommunityApi.list(type = "template") }
                        .getOrElse {
                            _error.value = "Could not load the community library: ${it.message}"
                            emptyList()
                        }
                _loading.value = false
            }
        }

        /** Local templates saved from the detail screen ("Template" action). */
        private fun templateDir() = java.io.File(context.filesDir, "user_templates").apply { mkdirs() }

        private suspend fun loadUserTemplates() =
            withContext(Dispatchers.IO) {
                templateDir()
                    .listFiles { f -> f.extension == "json" }
                    ?.mapNotNull { f ->
                        runCatching {
                            EngineJson.json.decodeFromString(
                                com.tdvorak.nothingmodes.engine.model.UserTemplate
                                    .serializer(),
                                f.readText(),
                            )
                        }.getOrNull()
                    }?.sortedBy { it.name }
                    ?: emptyList()
            }

        fun stageShare(template: com.tdvorak.nothingmodes.engine.model.UserTemplate) {
            _shareResult.value = null
            _shareTarget.value = template
        }

        fun dismissShare() {
            _shareTarget.value = null
            _shareResult.value = null
        }

        /** Publish a user template to the community library (goes to admin review). */
        fun publish(
            title: String,
            description: String,
            handle: String,
            email: String,
            github: String,
        ) {
            val template = _shareTarget.value ?: return
            viewModelScope.launch {
                _sharing.value = true
                try {
                    val bundle =
                        ExportBundle(
                            schemaVersion = 1,
                            exportedAt = System.currentTimeMillis(),
                            automations = template.automations,
                            appVersion =
                                runCatching {
                                    context.packageManager
                                        .getPackageInfo(context.packageName, 0)
                                        .versionName
                                }.getOrNull().orEmpty(),
                        )
                    val payload =
                        com.tdvorak.nothingmodes.engine.model.EngineJson.json
                            .parseToJsonElement(EngineJson.json.encodeToString(bundle))
                    _shareResult.value =
                        CommunityApi.submit(
                            type = "template",
                            title = title,
                            description = description,
                            handle = handle,
                            email = email,
                            github = github,
                            payload = payload,
                        )
                } catch (e: Exception) {
                    _shareResult.value = CommunityApi.SubmitResult.Failed(e.message ?: "submit failed")
                }
                _sharing.value = false
            }
        }

        /** Fetch an approved library item and stage it through the same review sheet. */
        fun selectLibraryItem(item: CommunityApi.LibraryItem) {
            viewModelScope.launch {
                _error.value = null
                try {
                    val payload = CommunityApi.fetchItem(item.id)
                    val payloadText = payload.canonicalJson()
                    val preview = importExportService.preview(payloadText, item.contentHash)
                    val summary =
                        TemplateSummary(
                            id = "lib:${item.id}",
                            name = item.title,
                            description = item.description,
                            creator = item.handle,
                            file = "",
                        )
                    if (!preview.isSupported) {
                        _pending.value =
                            PendingTemplateInstall(summary, emptyList(), emptyList(), emptyList(), preview.errors)
                        return@launch
                    }
                    val caps = CapabilitiesCache.peek() ?: CapabilitiesCache.refresh(context)
                    val resolution = CapabilityResolver(caps).resolve(item.id, preview.requiredCapabilities)
                    _pending.value =
                        PendingTemplateInstall(
                            summary = summary,
                            automations = preview.automations,
                            warnings =
                                resolution.missing
                                    .mapNotNull { CapabilityLabels.describe(it).ifBlank { null } }
                                    .distinct(),
                            satisfied =
                                (preview.requiredCapabilities - resolution.missing)
                                    .mapNotNull { CapabilityLabels.describe(it).ifBlank { null } }
                                    .distinct(),
                            errors = emptyList(),
                        )
                } catch (e: Exception) {
                    _error.value = "Could not load item: ${e.message}"
                }
            }
        }

        fun deleteUserTemplate(id: String) {
            java.io.File(templateDir(), "$id.json").delete()
            _userTemplates.value = _userTemplates.value.filterNot { it.id == id }
        }

        /** Stage a locally saved template — same review sheet as remote ones. */
        fun selectUserTemplate(template: com.tdvorak.nothingmodes.engine.model.UserTemplate) {
            _pending.value =
                PendingTemplateInstall(
                    summary =
                        TemplateSummary(
                            id = "user:${template.id}",
                            name = template.name,
                            description = template.description,
                            file = "",
                        ),
                    automations = template.automations,
                    warnings = emptyList(),
                    satisfied = emptyList(),
                    errors = emptyList(),
                )
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
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCatalogScreen(
    onBack: () -> Unit,
    viewModel: TemplateCatalogViewModel = hiltViewModel(),
) {
    val userTemplates by viewModel.userTemplates.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val installing by viewModel.installing.collectAsState()
    val installed by viewModel.installed.collectAsState()
    val library by viewModel.library.collectAsState()
    val shareTarget by viewModel.shareTarget.collectAsState()
    val sharing by viewModel.sharing.collectAsState()
    val shareResult by viewModel.shareResult.collectAsState()
    var search by remember { mutableStateOf("") }
    var librarySort by remember { mutableStateOf("newest") }
    var selectedCaps by remember { mutableStateOf<Set<String>>(emptySet()) }
    val allCaps = remember(library) { library.flatMap { it.capabilities }.distinct().sorted() }

    val visibleUser =
        userTemplates.filter {
            search.isBlank() ||
                it.name.contains(search, true) ||
                it.description.contains(search, true)
        }
    val visibleLibrary =
        library
            .filter {
                search.isBlank() ||
                    it.title.contains(search, true) ||
                    it.description.contains(search, true) ||
                    it.handle.contains(search, true)
            }.filter { selectedCaps.isEmpty() || it.capabilities.any { c -> selectedCaps.contains(c) } }
            .let { list ->
                when (librarySort) {
                    "download" -> list.sortedByDescending { it.downloads }
                    "alpha" -> list.sortedBy { it.title.lowercase() }
                    else -> list
                }
            }

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
                error != null && library.isEmpty() && userTemplates.isEmpty() ->
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
                userTemplates.isEmpty() && library.isEmpty() ->
                    NothingEmptyState(
                        title = "No templates yet",
                        description = "Save a mode as a template from its detail page, or check back for community templates",
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
                        item {
                            com.tdvorak.nothingmodes.ui.theme.NothingInput(
                                value = search,
                                onValueChange = { search = it },
                                label = "Search",
                                placeholder = "Find a template",
                            )
                        }

                        if (visibleUser.isNotEmpty()) {
                            item {
                                NothingLabel(text = "My templates")
                            }
                            items(visibleUser, key = { "u:${it.id}" }) { template ->
                                NothingCard(modifier = Modifier.fillMaxWidth()) {
                                    NothingListRow(
                                        title = template.name,
                                        subtitle =
                                            template.description.ifBlank {
                                                val n = template.automations.size
                                                if (n == 1) "1 mode" else "$n modes"
                                            },
                                        onClick = { viewModel.selectUserTemplate(template) },
                                        trailing = {
                                            Row {
                                                Text(
                                                    text = "SHARE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = NothingFonts.mono(),
                                                    modifier =
                                                        Modifier
                                                            .clickable {
                                                                viewModel.stageShare(template)
                                                            }.padding(NothingSpacing.sm),
                                                )
                                                Text(
                                                    text = "DELETE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NothingColors.accent,
                                                    fontFamily = NothingFonts.mono(),
                                                    modifier =
                                                        Modifier
                                                            .clickable {
                                                                viewModel.deleteUserTemplate(template.id)
                                                            }.padding(NothingSpacing.sm),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        if (library.isNotEmpty()) {
                            item {
                                NothingLabel(text = "Community library — reviewed")
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                    ) {
                                        listOf(
                                            "newest" to "Newest",
                                            "download" to "Most downloaded",
                                            "alpha" to "A-Z",
                                        ).forEach { (key, label) ->
                                            NothingTag(
                                                text = label,
                                                active = librarySort == key,
                                                onClick = { librarySort = key },
                                            )
                                        }
                                    }
                                    if (allCaps.isNotEmpty()) {
                                        NothingLabel(text = "Filter by what your phone supports")
                                        // Scrollable — the row clips to "a…" otherwise.
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                        ) {
                                            allCaps.forEach { cap ->
                                                NothingTag(
                                                    text = friendlyCapabilityName(cap),
                                                    active = selectedCaps.contains(cap),
                                                    onClick = {
                                                        selectedCaps =
                                                            if (selectedCaps.contains(cap)) {
                                                                selectedCaps - cap
                                                            } else {
                                                                selectedCaps + cap
                                                            }
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            items(visibleLibrary, key = { "lib:${it.id}" }) { item ->
                                LibraryRow(
                                    item = item,
                                    onClick = { viewModel.selectLibraryItem(item) },
                                )
                            }
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

    shareTarget?.let { template ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissShare() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ShareSheet(
                initialTitle = template.name,
                initialDescription = template.description,
                profile = viewModel.creatorProfile.get(),
                sharing = sharing,
                result = shareResult,
                onPublish = { t, d, h, e, g -> viewModel.publish(t, d, h, e, g) },
                onDismiss = { viewModel.dismissShare() },
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
                    text = "Imported ${result.imported} ${if (result.imported == 1) "mode" else "modes"}. They start disabled — enable them when ready.",
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
private fun LibraryRow(
    item: CommunityApi.LibraryItem,
    onClick: () -> Unit,
) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        val iconName =
            item.preview
                ?.get("icon")
                ?.jsonPrimitive
                ?.content ?: "star"
        val bgHex =
            item.preview
                ?.get("iconBackground")
                ?.jsonPrimitive
                ?.content
        val fallback = MaterialTheme.colorScheme.surfaceVariant
        val bgColor =
            remember(bgHex, fallback) {
                runCatching { Color(android.graphics.Color.parseColor(bgHex)) }.getOrNull()
                    ?: fallback
            }
        val icon = iconForName(iconName)
        NothingListRow(
            title = item.title,
            subtitle = item.description.ifBlank { item.summary },
            onClick = onClick,
            leading = {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(NothingShapes.iconChip)
                            .background(bgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "by @${item.handle}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
            if (item.summary.isNotBlank()) {
                NothingTag(text = item.summary, active = false)
            }
            if ("shizuku_required" in item.capabilities) {
                NothingTag(text = "Shizuku", active = false)
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
        val appLabel = rememberAppLabelResolver()
        val units = rememberUnits()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
        ) {
            NothingIconCircle {
                Icon(
                    imageVector = if (install.summary.icon.isNotBlank()) iconForName(install.summary.icon) else iconForEmoji(install.summary.emoji),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
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

        if (install.automations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NothingSpacing.md))
            NothingLabel(text = "Includes")
            install.automations.forEach { automation ->
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Text(
                    text = automation.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.mono(),
                )
                Text(
                    text = "Trigger: ${triggerDescription(automation.trigger, units, appLabel)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
                automation.actions.forEach { action ->
                    if (action is com.tdvorak.nothingmodes.engine.model.Action.Group) {
                        // List the group's children so users see what it does.
                        action.actions.forEach { child ->
                            Text(
                                text = "  ▸ ${actionDescription(child)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                    } else {
                        Text(
                            text = "  ▸ ${actionDescription(action)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }
            }
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
                text = "Works on this device: ${install.satisfied.map { friendlyCapabilityName(it) }.distinct().joinToString(", ")}",
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

/** Human-readable name for a satisfied capability ID in the install sheet. */
private fun friendlyCapabilityName(id: String): String =
    when {
        id.contains("glyph_matrix") -> "Glyph matrix"
        id.contains("glyph") -> "Glyph lights"
        id.contains("flashlight") -> "Flashlight"
        id.contains("vibrate") -> "Vibration"
        id.contains("wifi") -> "Wi-Fi"
        id.contains("bluetooth") || id.contains("bt") -> "Bluetooth"
        id.contains("notification") -> "Notification access"
        id.contains("foreground_app") || id.contains("app_opened") -> "Usage access"
        id.contains("location") || id.contains("geofence") -> "Location"
        id.contains("calendar") -> "Calendar"
        id.contains("phone") || id.contains("sms") -> "Phone"
        id.contains("shizuku") || id.startsWith("state_reader") -> "Shizuku"
        else -> id.substringAfterLast('_').replaceFirstChar { it.uppercase() }
    }

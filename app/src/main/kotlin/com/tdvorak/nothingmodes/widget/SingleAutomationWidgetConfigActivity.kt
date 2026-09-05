package com.tdvorak.nothingmodes.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    private val store: AutomationStore,
) : ViewModel() {
    private val _items = MutableStateFlow<List<Automation>>(emptyList())
    val items: StateFlow<List<Automation>> = _items.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _items.value = store.all().sortedBy { it.priority }
        }
    }
}

@AndroidEntryPoint
class SingleAutomationWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                WidgetConfigScreen(
                    onPick = { automationId ->
                        // Save the selected automation ID for this widget.
                        getSharedPreferences("single_widget_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("widget_$appWidgetId", automationId)
                            .apply()

                        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(RESULT_OK, resultValue)

                        // Update the widget and only then finish — otherwise
                        // lifecycleScope is cancelled before the update lands
                        // and the widget keeps its pre-config content.
                        lifecycleScope.launch {
                            runCatching {
                                val glanceId = GlanceAppWidgetManager(this@SingleAutomationWidgetConfigActivity)
                                    .getGlanceIdBy(appWidgetId)
                                SingleAutomationWidget().update(this@SingleAutomationWidgetConfigActivity, glanceId)
                            }
                            finish()
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    onPick: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: WidgetConfigViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NothingSpacing.md),
        ) {
            Text(
                text = "SELECT AUTOMATION",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = SpaceMono,
                modifier = Modifier.padding(bottom = NothingSpacing.md),
            )

            if (items.isEmpty()) {
                Text(
                    text = "No automations available. Create one first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    items(items, key = { it.id.value }) { automation ->
                        AutomationPickerRow(
                            automation = automation,
                            onClick = { onPick(automation.id.value) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationPickerRow(
    automation: Automation,
    onClick: () -> Unit,
) {
    val iconColor = if (automation.iconBackground.isNotBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(automation.iconBackground)) }
            .getOrDefault(Color(0xFFD71921))
    } else {
        Color(0xFFD71921)
    }
    val iconTextColor = if (iconColor.luminance() > 0.5f) Color.Black else Color.White

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NothingSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = automation.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = iconTextColor,
                    fontFamily = Doto,
                )
            }
            Text(
                text = automation.name.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = NothingSpacing.md),
            )
        }
    }
}

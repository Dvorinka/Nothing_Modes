package com.tdvorak.nothingmodes.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback

import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tdvorak.nothingmodes.automation.quickactions.QuickActionTrigger
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AutomationIdKey = ActionParameters.Key<String>("automation_id")

class NothingModesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
        val store = entryPoint.automationStore()

        val automations = withContext(Dispatchers.IO) {
            runCatching {
                // quickAction is always enabled — show enabled armed manual automations as tiles.
                store.all().filter {
                    it.enabled && it.status == AutomationStatus.ARMED && it.trigger is Trigger.Manual
                }.take(2) // allow up to 2 tiles
            }.getOrDefault(emptyList())
        }

        provideContent {
            NothingModesWidgetContent(automations)
        }
    }
}

@Composable
private fun NothingModesWidgetContent(automations: List<Automation>) {
    val accent = ColorProvider(Color(0xFFD71921))
    val surface = ColorProvider(Color(0xFF0D0D0D))
    val onSurface = ColorProvider(Color(0xFFEAEAEA))
    val onSurfaceVariant = ColorProvider(Color(0xFF7E7E7E))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surface)
            .padding(12.dp),
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            Text(
                text = "NOTHING MODES",
                style = TextStyle(
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp),
            )

            if (automations.isEmpty()) {
                Text(
                    text = "No quick actions yet",
                    style = TextStyle(
                        color = onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                    modifier = GlanceModifier.padding(top = 8.dp),
                )
            } else {
                LazyColumn {
                    items(
                        items = automations,
                        itemId = { it.id.value.hashCode().toLong() },
                    ) { automation ->
                        QuickActionItem(
                            automation = automation,
                            accent = accent,
                            onSurface = onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    automation: Automation,
    accent: ColorProvider,
    onSurface: ColorProvider,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ColorProvider(Color(0xFF1A1A1A)))
            .padding(12.dp)
            .clickable(
                actionRunCallback<RunAutomationAction>(
                    actionParametersOf(AutomationIdKey to automation.id.value),
                ),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = automation.name.ifBlank { "Untitled" },
                style = TextStyle(color = onSurface, fontSize = 13.sp),
            )
        }
        if (automation.trigger is Trigger.Manual) {
            Text(
                text = "RUN",
                style = TextStyle(color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
    Spacer(modifier = GlanceModifier.height(6.dp))
}

class RunAutomationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[AutomationIdKey] ?: return
        QuickActionTrigger.run(context, id)
    }
}

class NothingModesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NothingModesWidget()
}

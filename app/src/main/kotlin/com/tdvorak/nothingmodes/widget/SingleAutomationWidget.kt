package com.tdvorak.nothingmodes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tdvorak.nothingmodes.automation.quickactions.QuickActionTrigger
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AutomationIdKey = ActionParameters.Key<String>("automation_id")

class SingleAutomationWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
        val store = entryPoint.automationStore()

        // Read the configured automation ID from SharedPreferences.
        val prefs = context.getSharedPreferences("single_widget_prefs", Context.MODE_PRIVATE)
        val automationId = prefs.getString("widget_${id}", null)

        val automation = withContext(Dispatchers.IO) {
            runCatching {
                automationId?.let { store.get(com.tdvorak.nothingmodes.engine.model.AutomationId(it)) }
            }.getOrNull()
        }

        provideContent {
            SingleAutomationWidgetContent(automation)
        }
    }
}

@Composable
private fun SingleAutomationWidgetContent(automation: Automation?) {
    val accent = ColorProvider(Color(0xFFD71921))
    val surface = ColorProvider(Color(0xFF0D0D0D))
    val onSurface = ColorProvider(Color(0xFFEAEAEA))
    val onSurfaceVariant = ColorProvider(Color(0xFF7E7E7E))

    if (automation == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(surface)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Tap to configure",
                style = TextStyle(color = onSurfaceVariant, fontSize = 11.sp),
            )
        }
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surface)
            .padding(8.dp)
            .clickable(
                actionRunCallback<RunSingleAutomationAction>(
                    actionParametersOf(AutomationIdKey to automation.id.value),
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon circle — first letter of the name (black and white per Nothing aesthetic)
        Text(
            text = automation.icon.ifBlank { automation.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?" },
            style = TextStyle(
                color = onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = automation.name.ifBlank { "Untitled" },
            style = TextStyle(color = onSurface, fontSize = 11.sp),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
    }
}

class RunSingleAutomationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[AutomationIdKey] ?: return
        QuickActionTrigger.run(context, id)
    }
}

class SingleAutomationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleAutomationWidget()
}

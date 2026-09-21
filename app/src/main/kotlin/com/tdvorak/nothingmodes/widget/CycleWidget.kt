package com.tdvorak.nothingmodes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleActions
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleEngine
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpec
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val WidgetIdKey = ActionParameters.Key<Int>("app_widget_id")

/**
 * Home-screen companion to the cycle tiles: one tap applies the next step of
 * the spec chosen in [CycleWidgetConfigActivity]. Shows the current value so
 * a second tap is predictable — same engine, same prefs namespace.
 */
class CycleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId =
            runCatching {
                GlanceAppWidgetManager(context).getAppWidgetId(id)
            }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

        val spec = CycleTilePrefs.load(context, CycleTilePrefs.widgetKey(appWidgetId))
        val action = spec?.let { CycleActions.byId(it.actionId) }
        val current =
            if (spec != null && action != null && action.permissionFix(context) == null) {
                withContext(Dispatchers.IO) { runCatching { action.current(context) }.getOrNull() }
            } else {
                null
            }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        provideContent {
            CycleWidgetContent(spec, current, appWidgetId, launchIntent)
        }
    }
}

@Composable
private fun CycleWidgetContent(
    spec: CycleSpec?,
    current: String?,
    appWidgetId: Int,
    launchIntent: android.content.Intent?,
) {
    val surface = ColorProvider(Color(0xFF0D0D0D))
    val onSurface = ColorProvider(Color(0xFFEAEAEA))
    val onSurfaceVariant = ColorProvider(Color(0xFF7E7E7E))
    val accent = ColorProvider(Color(0xFFD71921))
    val context = LocalContext.current

    val action = spec?.let { CycleActions.byId(it.actionId) }

    // Unconfigured or permission-blocked widgets open the app instead of
    // pretending a tap did something.
    val tapModifier =
        when {
            spec == null || action == null ->
                launchIntent?.let { GlanceModifier.clickable(actionStartActivity(it)) }
                    ?: GlanceModifier
            else -> {
                val fix = action.permissionFix(context)
                if (fix != null) {
                    GlanceModifier.clickable(actionStartActivity(fix))
                } else {
                    GlanceModifier.clickable(
                        actionRunCallback<CycleWidgetAction>(
                            actionParametersOf(WidgetIdKey to appWidgetId),
                        ),
                    )
                }
            }
        }

    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(surface)
                .padding(8.dp)
                .then(tapModifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = action?.label?.uppercase() ?: "CYCLE",
                style = TextStyle(color = onSurfaceVariant, fontSize = 11.sp),
            )
            Text(
                text =
                    when {
                        action == null -> "Tap to set up"
                        current != null -> action.format(current)
                        else -> "—"
                    },
                style =
                    TextStyle(
                        color = if (current != null) accent else onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
            if (spec != null && action != null && current != null) {
                val next = CycleEngine.nextValue(current, spec.steps)
                Text(
                    text = "next: ${next?.let { action.format(it) } ?: "?"}",
                    style = TextStyle(color = onSurfaceVariant, fontSize = 10.sp),
                    modifier = GlanceModifier.padding(top = 2.dp),
                )
            }
        }
    }
}

class CycleWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = parameters[WidgetIdKey] ?: return
        val spec = CycleTilePrefs.load(context, CycleTilePrefs.widgetKey(appWidgetId)) ?: return
        withContext(Dispatchers.IO) {
            runCatching { CycleEngine.advance(context, spec) }
        }
        CycleWidget().update(context, glanceId)
    }
}

class CycleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CycleWidget()
}

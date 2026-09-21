package com.tdvorak.nothingmodes.widget

import android.appwidget.AppWidgetManager
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
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val WidgetIdKey = ActionParameters.Key<Int>("app_widget_id")

/**
 * Home-screen companion to the cycle tiles: one tap applies the next step of
 * the spec chosen in [CycleWidgetConfigActivity]. Stateful actions show the
 * current value plus the next step; stateless ones (the mode runner) show
 * what the tap will run — same engine, same prefs namespace.
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

        val key = CycleTilePrefs.widgetKey(appWidgetId)
        val spec = CycleTilePrefs.load(context, key)
        val action = spec?.let { CycleActions.byId(it.actionId) }
        val ready = action != null && action.permissionFix(context) == null

        // Resolve display strings here — Glance content is not suspend-friendly.
        var main: String? = null
        var hint: String? = null
        if (spec != null && action != null && ready) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val steps = CycleEngine.steps(context, spec)
                    val current = action.current(context) ?: CycleTilePrefs.cursor(context, key)
                    val next = CycleEngine.nextValue(current, steps)
                    if (action.showsNext) {
                        main = next?.let { action.describe(context, it) } ?: "None armed"
                        hint = if (next != null) "tap to run" else null
                    } else {
                        main = current?.let { action.describe(context, it) }
                        hint = next?.let { "next: ${action.describe(context, it)}" }
                    }
                }
            }
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        provideContent {
            CycleWidgetContent(
                label = action?.label?.uppercase() ?: "CYCLE",
                main = main,
                hint = hint,
                action = action,
                appWidgetId = appWidgetId,
                launchIntent = launchIntent,
            )
        }
    }
}

@Composable
private fun CycleWidgetContent(
    label: String,
    main: String?,
    hint: String?,
    action: com.tdvorak.nothingmodes.quicksettings.cycle.CycleAction?,
    appWidgetId: Int,
    launchIntent: Intent?,
) {
    val surface = ColorProvider(Color(0xFF0D0D0D))
    val onSurface = ColorProvider(Color(0xFFEAEAEA))
    val onSurfaceVariant = ColorProvider(Color(0xFF7E7E7E))
    val accent = ColorProvider(Color(0xFFD71921))
    val context = androidx.glance.LocalContext.current

    // Unconfigured or permission-blocked widgets open the app instead of
    // pretending a tap did something.
    val tapModifier =
        when {
            action == null ->
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
                text = label,
                style = TextStyle(color = onSurfaceVariant, fontSize = 11.sp),
            )
            Text(
                text = main ?: if (action == null) "Tap to set up" else "—",
                style =
                    TextStyle(
                        color = if (main != null) accent else onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                modifier = GlanceModifier.padding(top = 4.dp),
                maxLines = 1,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = TextStyle(color = onSurfaceVariant, fontSize = 10.sp),
                    modifier = GlanceModifier.padding(top = 2.dp),
                    maxLines = 1,
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
        val key = CycleTilePrefs.widgetKey(appWidgetId)
        val spec = CycleTilePrefs.load(context, key) ?: return
        withContext(Dispatchers.IO) {
            runCatching { CycleEngine.advance(context, spec, key) }
        }
        CycleWidget().update(context, glanceId)
    }
}

class CycleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CycleWidget()
}

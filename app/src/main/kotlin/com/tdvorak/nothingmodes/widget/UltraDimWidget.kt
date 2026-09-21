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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dedicated ultra-dim button: tap cycles the dimmer through its default
 * steps (25% → 50% → 75% → off). Unlike the generic cycle widget it needs
 * no configuration — the dim level is readable live.
 */
class UltraDimWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val action = CycleActions.ultraDim
        val spec = CycleSpec(action.id, action.defaultSteps)
        val permissionFix = action.permissionFix(context)

        var main: String? = null
        var hint: String? = null
        if (permissionFix == null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val current = action.current(context)
                    val next = CycleEngine.nextValue(current, spec.steps)
                    main = current?.let { action.format(it) }
                    hint = next?.let { "next: ${action.format(it)}" }
                }
            }
        }

        provideContent {
            UltraDimWidgetContent(
                main = main,
                hint = hint,
                permissionFix = permissionFix,
            )
        }
    }
}

@Composable
private fun UltraDimWidgetContent(
    main: String?,
    hint: String?,
    permissionFix: Intent?,
) {
    val surface = ColorProvider(Color(0xFF0D0D0D))
    val onSurface = ColorProvider(Color(0xFFEAEAEA))
    val onSurfaceVariant = ColorProvider(Color(0xFF7E7E7E))
    val accent = ColorProvider(Color(0xFFD71921))

    val tapModifier =
        if (permissionFix != null) {
            GlanceModifier.clickable(actionStartActivity(permissionFix))
        } else {
            GlanceModifier.clickable(actionRunCallback<UltraDimWidgetAction>())
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
                text = "DIM",
                style = TextStyle(color = onSurfaceVariant, fontSize = 11.sp),
            )
            Text(
                text = main ?: if (permissionFix != null) "Setup" else "—",
                style =
                    TextStyle(
                        color = if (main != null && main != "Off") accent else onSurface,
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

class UltraDimWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val action = CycleActions.ultraDim
        withContext(Dispatchers.IO) {
            runCatching {
                CycleEngine.advance(context, CycleSpec(action.id, action.defaultSteps))
            }
        }
        UltraDimWidget().update(context, glanceId)
    }
}

class UltraDimWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UltraDimWidget()
}

package com.tdvorak.nothingmodes.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpecEditorScreen
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs
import kotlinx.coroutines.launch

/**
 * Picker shown when a Cycle widget is placed on the home screen: choose the
 * action and its steps, then the widget taps cycle through them.
 */
class CycleWidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            CycleSpecEditorScreen(
                title = "Cycle widget",
                initial = CycleTilePrefs.load(this, CycleTilePrefs.widgetKey(appWidgetId)),
                onSave = { spec ->
                    // A cleared spec leaves a tap-to-open-app widget — still
                    // useful, and the launcher keeps offering config on resize.
                    CycleTilePrefs.save(this, CycleTilePrefs.widgetKey(appWidgetId), spec)
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                    )
                    lifecycleScope.launch {
                        runCatching {
                            val glanceId =
                                GlanceAppWidgetManager(this@CycleWidgetConfigActivity)
                                    .getGlanceIdBy(appWidgetId)
                            CycleWidget().update(this@CycleWidgetConfigActivity, glanceId)
                        }
                        finish()
                    }
                },
                onBack = { finish() },
            )
        }
    }
}

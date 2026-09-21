package com.tdvorak.nothingmodes.quicksettings.cycle

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.core.net.toUri
import com.tdvorak.nothingmodes.R
import com.tdvorak.nothingmodes.automation.quickactions.QuickActionTrigger
import com.tdvorak.nothingmodes.capabilities.controllers.AndroidBrightnessController
import com.tdvorak.nothingmodes.capabilities.controllers.AndroidDarkModeController
import com.tdvorak.nothingmodes.capabilities.controllers.AndroidDndController
import com.tdvorak.nothingmodes.capabilities.controllers.AndroidScreenTimeoutController
import com.tdvorak.nothingmodes.capabilities.controllers.AndroidVolumeController
import com.tdvorak.nothingmodes.capabilities.controllers.ControllerResult
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.nothing.GlyphResult
import com.tdvorak.nothingmodes.widget.WidgetEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tap-to-cycle engine shared by Quick Settings tiles and the home-screen
 * widget. A [CycleSpec] names an action plus an ordered list of raw values;
 * each tap applies the value after the current one, wrapping at the end.
 *
 * Values are plain strings ("30000", "auto", "priority") so a spec survives
 * SharedPreferences and the editor needs no per-action model. Actions with a
 * live step set (the mode runner) override [CycleAction.dynamicSteps].
 */
data class CycleSpec(
    val actionId: String,
    val steps: List<String>,
) {
    fun encode(): String = "$actionId|${steps.joinToString(",")}"

    companion object {
        fun decode(raw: String?): CycleSpec? {
            if (raw.isNullOrBlank() || '|' !in raw) return null
            val action = raw.substringBefore('|')
            val steps =
                raw
                    .substringAfter('|')
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (steps.isEmpty() && CycleActions.byId(action)?.usesDynamicSteps != true) return null
            return CycleSpec(action, steps)
        }
    }
}

interface CycleAction {
    val id: String

    /** Tile label and editor heading. */
    val label: String

    /** Tile icon resource in the app package. */
    val iconRes: Int

    /** Steps a fresh custom slot starts with. */
    val defaultSteps: List<String>

    /** Fixed vocabulary for enum-like actions; null = freeform numeric steps. */
    val allowedValues: List<String>?

    /** Hint above the freeform steps field in the editor. */
    val stepsHint: String

    /** Live step set overriding spec steps — the mode runner returns automation ids. */
    val usesDynamicSteps: Boolean
        get() = false

    /** The subtitle shows what the next tap does rather than the current state. */
    val showsNext: Boolean
        get() = false

    /** Intent fixing the missing permission, or null when ready to run. */
    fun permissionFix(context: Context): Intent?

    /** Canonical current value, or null when unreadable / stateless. */
    suspend fun current(context: Context): String?

    /** Steps resolved live; null means "use the spec's". */
    suspend fun dynamicSteps(context: Context): List<String>? = null

    suspend fun apply(
        context: Context,
        value: String,
    ): ControllerResult

    /** Short tile/widget subtitle for a value. */
    fun format(value: String): String

    /** Display label for values needing a lookup (e.g. automation names). */
    suspend fun describe(
        context: Context,
        value: String,
    ): String = format(value)

    /** Tile STATE_ACTIVE when the applied state counts as "engaged". */
    fun isOn(value: String): Boolean = true

    /** Parse a comma-separated editor list into canonical values. Null = invalid. */
    fun parseSteps(text: String): List<String>? =
        text
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
}

object CycleActions {
    private fun writeSettingsFix(context: Context): Intent? =
        if (Settings.System.canWrite(context)) {
            null
        } else {
            Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun policyAccessFix(context: Context): Intent? {
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        return if (nm?.isNotificationPolicyAccessGranted == true) {
            null
        } else {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    val screenTimeout =
        object : CycleAction {
            override val id = "screen_timeout"
            override val label = "Screen timeout"
            override val iconRes = R.drawable.ic_tile_timeout
            override val defaultSteps = listOf("30000", "60000", "300000")
            override val allowedValues: List<String>? = null
            override val stepsHint = "Values like 15s, 30s, 1m, 5m, never"

            override fun permissionFix(context: Context) = writeSettingsFix(context)

            override suspend fun current(context: Context): String? =
                AndroidScreenTimeoutController(context).getScreenTimeout()?.toString()

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                val ms = value.toIntOrNull() ?: return ControllerResult.Failure("Bad timeout: $value")
                return AndroidScreenTimeoutController(context).setScreenTimeout(ms)
            }

            override fun format(value: String): String = value.toLongOrNull()?.let { formatDuration(it) } ?: value

            override fun parseSteps(text: String): List<String>? {
                val tokens = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                val parsed = tokens.map { parseDuration(it) }
                if (tokens.isEmpty() || parsed.any { it == null }) return null
                return parsed.map { it!!.toString() }
            }

            private fun parseDuration(token: String): Long? {
                val t = token.trim().lowercase()
                if (t.isEmpty()) return null
                if (t == "never" || t == "∞") return Int.MAX_VALUE.toLong()
                val match = Regex("""^(\d+)\s*(s|sec|m|min|h)?$""").find(t) ?: return null
                val n = match.groupValues[1].toLongOrNull() ?: return null
                val unit = match.groupValues[2]
                val ms =
                    when (unit) {
                        "m", "min" -> n * 60_000
                        "h" -> n * 3_600_000
                        else -> n * 1_000
                    }
                return ms.coerceIn(1_000, Int.MAX_VALUE.toLong())
            }

            private fun formatDuration(ms: Long): String =
                when {
                    ms >= Int.MAX_VALUE.toLong() -> "Never"
                    ms % 3_600_000L == 0L -> "${ms / 3_600_000}h"
                    ms % 60_000L == 0L -> "${ms / 60_000}m"
                    else -> "${ms / 1_000}s"
                }
        }

    val brightness =
        object : CycleAction {
            override val id = "brightness"
            override val label = "Brightness"
            override val iconRes = R.drawable.ic_tile_brightness
            override val defaultSteps = listOf("25", "50", "100")
            override val allowedValues: List<String>? = null
            override val stepsHint = "Percents 0-100, or auto, e.g. 25, 50, 100"

            override fun permissionFix(context: Context) = writeSettingsFix(context)

            override suspend fun current(context: Context): String? {
                val c = AndroidBrightnessController(context)
                if (c.isAutoBrightness() == true) return "auto"
                return c.getBrightness()?.let { (it * 100 / 255).toString() }
            }

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                val c = AndroidBrightnessController(context)
                if (value == "auto") return c.setAutoBrightness(true)
                val pct = value.toIntOrNull()?.coerceIn(0, 100) ?: return ControllerResult.Failure("Bad level: $value")
                c.setAutoBrightness(false)
                return c.setBrightness(pct * 255 / 100)
            }

            override fun format(value: String): String = if (value == "auto") "Auto" else "$value%"

            override fun parseSteps(text: String): List<String>? {
                val tokens = text.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                val parsed =
                    tokens.map { token ->
                        when {
                            token == "auto" -> "auto"
                            token.removeSuffix("%").toIntOrNull() in 0..100 -> token.removeSuffix("%")
                            else -> null
                        }
                    }
                if (tokens.isEmpty() || parsed.any { it == null }) return null
                return parsed.filterNotNull()
            }
        }

    val ultraDim =
        object : CycleAction {
            override val id = "ultra_dim"
            override val label = "Ultra dim"
            override val iconRes = R.drawable.ic_tile_ultra_dim
            override val defaultSteps = listOf("25", "50", "75", "0")
            override val allowedValues: List<String>? = null
            override val stepsHint = "Percents 1-100 plus 0 or off, e.g. 25, 50, 75, off"

            override fun permissionFix(context: Context): Intent? =
                if (UltraDimController.canDim(context)) null else UltraDimController.permissionIntent(context)

            override suspend fun current(context: Context): String? = UltraDimController.percent.toString()

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                val pct = value.toIntOrNull() ?: return ControllerResult.Failure("Bad percent: $value")
                if (pct <= 0) UltraDimController.hide(context) else UltraDimController.show(context, pct)
                return ControllerResult.Success
            }

            override fun format(value: String): String = if (value == "0") "Off" else "$value%"

            override fun isOn(value: String): Boolean = value != "0"

            override fun parseSteps(text: String): List<String>? =
                text
                    .split(',')
                    .map { it.trim().lowercase().removeSuffix("%") }
                    .filter { it.isNotEmpty() }
                    .map { if (it == "off") "0" else it }
                    .takeIf { tokens -> tokens.all { it.toIntOrNull() in 0..100 } }
        }

    val mediaVolume =
        object : CycleAction {
            override val id = "media_volume"
            override val label = "Media volume"
            override val iconRes = R.drawable.ic_tile_volume
            override val defaultSteps = listOf("0", "25", "50", "75", "100")
            override val allowedValues: List<String>? = null
            override val stepsHint = "Percents 0-100, e.g. 0, 33, 66, 100"

            override fun permissionFix(context: Context): Intent? = null

            override suspend fun current(context: Context): String? {
                val c = AndroidVolumeController(context)
                val level = c.getVolume(VolumeStream.MEDIA) ?: return null
                val max = streamMax(context)
                if (max <= 0) return null
                return (level * 100 / max).toString()
            }

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                val pct = value.toIntOrNull()?.coerceIn(0, 100) ?: return ControllerResult.Failure("Bad volume: $value")
                val max = streamMax(context)
                return AndroidVolumeController(context).setVolume(VolumeStream.MEDIA, pct * max / 100)
            }

            override fun format(value: String): String = "$value%"

            override fun isOn(value: String): Boolean = value != "0"

            override fun parseSteps(text: String): List<String>? =
                text
                    .split(',')
                    .map { it.trim().lowercase().removeSuffix("%") }
                    .filter { it.isNotEmpty() }
                    .takeIf { tokens -> tokens.all { it.toIntOrNull() in 0..100 } }

            private fun streamMax(context: Context): Int =
                context
                    .getSystemService(AudioManager::class.java)
                    ?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
        }

    /** Glyph lights: toggle() all-on or turnOff() on the light stripe; on
     *  matrix devices a full-percent frame stands in — same as SetGlyph. */
    val glyph =
        object : CycleAction {
            override val id = "glyph"
            override val label = "Glyph lights"
            override val iconRes = R.drawable.ic_tile_glyph
            override val defaultSteps = listOf("on", "off")
            override val allowedValues = listOf("on", "off")
            override val stepsHint = ""

            /** Providers expose no readable state — track what we set. */
            @Volatile
            private var lastSet: String? = null

            override fun permissionFix(context: Context): Intent? = null

            override suspend fun current(context: Context): String? = lastSet

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                val ep =
                    runCatching {
                        EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
                    }.getOrNull() ?: return ControllerResult.Failure("app graph unavailable")
                val stripe = ep.glyphProvider().takeIf { it.isAvailable() }
                val matrix = ep.glyphMatrixProvider().takeIf { it.isAvailable() }
                if (stripe == null && matrix == null) return ControllerResult.Unsupported
                val result =
                    if (stripe != null) {
                        if (!stripe.ensureConnected()) return ControllerResult.Failure("glyph service unavailable")
                        if (value == "on") stripe.toggle() else stripe.turnOff()
                    } else {
                        if (matrix?.ensureConnected() != true) {
                            return ControllerResult.Failure("glyph service unavailable")
                        }
                        // A clean ring beats a solid 625-dot slab.
                        if (value == "on") matrix.displayProgressArc(100) else matrix.turnOff()
                    }
                return when (result) {
                    GlyphResult.Success -> {
                        lastSet = value
                        ControllerResult.Success
                    }
                    is GlyphResult.Failure -> ControllerResult.Failure(result.reason)
                    GlyphResult.Unsupported -> ControllerResult.Unsupported
                    GlyphResult.PermissionRequired -> ControllerResult.PermissionRequired
                    GlyphResult.ServiceUnavailable -> ControllerResult.Failure("glyph service unavailable")
                }
            }

            override fun format(value: String): String = value.replaceFirstChar { it.uppercase() }

            override fun isOn(value: String): Boolean = value == "on"
        }

    /**
     * Curated scene preset — a composite step applying DND + ultra dim +
     * dark mode in one tap. Ultra dim is opportunistic: skipped silently
     * when the device can't overlay, so scenes never hard-fail on it.
     */
    val scene =
        object : CycleAction {
            override val id = "scene"
            override val label = "Scene"
            override val iconRes = R.drawable.ic_tile_scene
            override val defaultSteps = listOf("reset", "bedtime", "focus")
            override val allowedValues = listOf("reset", "bedtime", "focus")
            override val stepsHint = ""

            override fun permissionFix(context: Context): Intent? = policyAccessFix(context)

            override suspend fun current(context: Context): String? =
                when (AndroidDndController(context).getDndMode()) {
                    DndMode.PRIORITY -> "bedtime"
                    DndMode.TOTAL -> "focus"
                    DndMode.OFF -> "reset"
                    null -> null
                }

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                val dnd = AndroidDndController(context)
                val dark = AndroidDarkModeController(context)
                val results =
                    when (value) {
                        "bedtime" ->
                            listOf(
                                dnd.setDnd(DndMode.PRIORITY),
                                dimTo(context, 60),
                                dark.setDarkMode(NightMode.ON),
                            )
                        "focus" ->
                            listOf(
                                dnd.setDnd(DndMode.TOTAL),
                                dimTo(context, 0),
                                dark.setDarkMode(NightMode.ON),
                            )
                        else ->
                            listOf(
                                dnd.setDnd(DndMode.OFF),
                                dimTo(context, 0),
                                dark.setDarkMode(NightMode.AUTO),
                            )
                    }
                return results.firstOrNull { it !is ControllerResult.Success }
                    ?: ControllerResult.Success
            }

            private fun dimTo(
                context: Context,
                percent: Int,
            ): ControllerResult =
                if (!UltraDimController.canDim(context)) {
                    ControllerResult.Success
                } else {
                    runCatching {
                        if (percent <= 0) UltraDimController.hide(context) else UltraDimController.show(context, percent)
                        ControllerResult.Success
                    }.getOrElse { ControllerResult.Failure(it.message ?: "dim failed") }
                }

            override fun format(value: String): String =
                when (value) {
                    "bedtime" -> "Bedtime"
                    "focus" -> "Focus"
                    else -> "Reset"
                }

            override fun isOn(value: String): Boolean = value != "reset"
        }

    /**
     * Stateless runner: each tap fires the next armed manual mode, wrapping at
     * the end of the list. Steps are live automation ids, so the spec's own
     * step list is ignored and the engine's per-slot cursor drives rotation.
     */
    val modeRunner =
        object : CycleAction {
            override val id = "mode_runner"
            override val label = "Run a mode"
            override val iconRes = R.drawable.ic_tile_modes
            override val defaultSteps = emptyList<String>()
            override val allowedValues: List<String>? = null
            override val stepsHint = ""
            override val usesDynamicSteps = true
            override val showsNext = true

            override fun permissionFix(context: Context): Intent? = null

            override suspend fun current(context: Context): String? = null

            override suspend fun dynamicSteps(context: Context): List<String>? =
                withContext(Dispatchers.IO) {
                    val store =
                        runCatching {
                            EntryPoints
                                .get(context.applicationContext, WidgetEntryPoint::class.java)
                                .automationStore()
                        }.getOrNull() ?: return@withContext null
                    runCatching {
                        store
                            .all()
                            .filter {
                                it.enabled &&
                                    it.quickAction &&
                                    it.status == AutomationStatus.ARMED &&
                                    it.trigger is Trigger.Manual
                            }.sortedBy { it.priority }
                            .map { it.id.value }
                    }.getOrNull()
                }

            override suspend fun apply(
                context: Context,
                value: String,
            ): ControllerResult {
                QuickActionTrigger.run(context, value)
                return ControllerResult.Success
            }

            override fun format(value: String): String = "Mode"

            override suspend fun describe(
                context: Context,
                value: String,
            ): String =
                withContext(Dispatchers.IO) {
                    val store =
                        runCatching {
                            EntryPoints
                                .get(context.applicationContext, WidgetEntryPoint::class.java)
                                .automationStore()
                        }.getOrNull() ?: return@withContext "Mode"
                    runCatching {
                        store.get(AutomationId(value))?.name?.ifBlank { "Untitled" }
                    }.getOrNull() ?: "Mode"
                }

            override fun isOn(value: String): Boolean = false
        }

    val all: List<CycleAction> =
        listOf(
            screenTimeout,
            brightness,
            ultraDim,
            mediaVolume,
            glyph,
            scene,
            modeRunner,
        )

    fun byId(id: String): CycleAction? = all.firstOrNull { it.id == id }
}

object CycleEngine {
    /** Outcome of one tap — the controller result plus the applied step. */
    data class AdvanceResult(
        val result: ControllerResult,
        val applied: String?,
    )

    /** The value the next tap applies: the step after [current], or the first. */
    fun nextValue(
        current: String?,
        steps: List<String>,
    ): String? {
        if (steps.isEmpty()) return null
        val index = steps.indexOf(current)
        return steps[(index + 1) % steps.size]
    }

    /**
     * Advance one step and apply it. [cursorKey] scopes the fallback position
     * for stateless actions (mode runner) — pass the spec's storage key.
     */
    suspend fun advance(
        context: Context,
        spec: CycleSpec,
        cursorKey: String? = null,
    ): AdvanceResult {
        val action =
            CycleActions.byId(spec.actionId)
                ?: return AdvanceResult(ControllerResult.Failure("Unknown action"), null)
        val steps = action.dynamicSteps(context)?.takeIf { it.isNotEmpty() } ?: spec.steps
        if (steps.isEmpty()) return AdvanceResult(ControllerResult.Failure("No steps"), null)
        val current = action.current(context) ?: cursorKey?.let { CycleTilePrefs.cursor(context, it) }
        val next = nextValue(current, steps) ?: return AdvanceResult(ControllerResult.Failure("No steps"), null)
        val result = action.apply(context, next)
        if (result is ControllerResult.Success && cursorKey != null) {
            CycleTilePrefs.saveCursor(context, cursorKey, next)
        }
        return AdvanceResult(result, next)
    }

    /** Resolve the steps a spec would cycle right now (dynamic or static). */
    suspend fun steps(
        context: Context,
        spec: CycleSpec,
    ): List<String> =
        CycleActions
            .byId(spec.actionId)
            ?.dynamicSteps(context)
            ?.takeIf { it.isNotEmpty() }
            ?: spec.steps
}

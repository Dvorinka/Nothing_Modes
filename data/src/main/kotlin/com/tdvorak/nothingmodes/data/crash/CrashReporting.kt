package com.tdvorak.nothingmodes.data.crash

import android.content.Context
import android.os.Build
import android.util.Log
import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Opt-in, self-hosted crash/error reporting.
 *
 * Reports are written to [filesDir]/crash_reports on the crashing thread (network
 * I/O is not safe there), then flushed to [endpoint] on the next opportunity.
 * Nothing leaves the device unless the user enabled reporting in Settings and the
 * process starts again or another report is queued.
 *
 * No third-party SDK — plain HttpURLConnection + kotlinx.serialization.
 */
object CrashReporting {
    private const val TAG = "CrashReporting"
    private const val PREFS_NAME = "crash_reporting"
    private const val KEY_ENABLED = "enabled"
    private const val REPORT_DIR = "crash_reports"
    private const val MAX_STACK_BYTES = 48 * 1024

    /** Ingest endpoint. Same for all builds; override only for tests. */
    var endpoint: String = "https://nothing-modes.vercel.app/api/crash"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var appContext: Context? = null
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        appContext = context.applicationContext
        _enabled.value =
            appContext!!
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
        installHandler()
        // Reports queued by the crashing process only reach the server if we
        // flush here — without this they wait on disk until the user toggles
        // the setting or a non-fatal error is logged.
        if (_enabled.value) flushQueue()
    }

    fun setEnabled(enabled: Boolean) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        _enabled.value = enabled
        if (enabled) flushQueue()
    }

    /** Record a non-fatal error. No-op unless the user opted in. */
    fun logError(error: Throwable, context: String = "") {
        if (!_enabled.value) return
        runCatching {
            saveReport(buildReport(Thread.currentThread(), error, kind = "error", context = context))
            flushQueue()
        }
    }

    // Handler is always installed; it only writes when the user opted in, so
    // toggling takes effect without a restart.
    private fun installHandler() {
        if (previousHandler != null) return
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                if (_enabled.value) {
                    saveReport(buildReport(thread, error, kind = "crash"))
                }
            }
            previousHandler?.uncaughtException(thread, error)
        }
    }

    private fun buildReport(
        thread: Thread,
        error: Throwable,
        kind: String,
        context: String = "",
    ): JsonObject {
        val ctx = appContext
        val packageInfo =
            ctx?.let {
                runCatching { it.packageManager.getPackageInfo(it.packageName, 0) }.getOrNull()
            }
        val stacktrace =
            StringWriter().also { sw ->
                error.printStackTrace(PrintWriter(sw))
            }.toString().take(MAX_STACK_BYTES)

        return buildJsonObject {
            put("kind", kind)
            put("app", "nothing-modes")
            put("app_version", packageInfo?.versionName ?: "")
            put("version_code", packageInfo?.longVersionCode?.toInt() ?: 0)
            put("flavor", FeatureFlags.distribution)
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("android_release", Build.VERSION.RELEASE)
            put("sdk_int", Build.VERSION.SDK_INT)
            put("exception_class", error.javaClass.name)
            put("message", error.message?.take(2000) ?: "")
            put("thread", thread.name)
            put("stacktrace", stacktrace)
            put("context", context)
            put("client_time", System.currentTimeMillis())
        }
    }

    private fun saveReport(report: JsonObject) {
        val dir = reportDir() ?: return
        val file =
            dir.resolve("report-${System.currentTimeMillis()}-${(0..9999).random()}.json")
        file.writeText(Json.encodeToString(JsonObject.serializer(), report))
    }

    private fun flushQueue() {
        val dir = reportDir() ?: return
        scope.launch {
            dir.listFiles()
                ?.sortedBy { it.name }
                ?.forEach { file ->
                    val ok = runCatching { post(file.readText()) }.getOrDefault(false)
                    if (ok) file.delete()
                }
        }
    }

    private fun post(body: String): Boolean {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.d(TAG, "report upload failed: ${e.message}")
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun reportDir(): java.io.File? =
        appContext
            ?.filesDir
            ?.resolve(REPORT_DIR)
            ?.also { it.mkdirs() }
}

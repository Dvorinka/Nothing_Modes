package com.tdvorak.nothingmodes.data.feedback

import android.content.Context
import android.os.Build
import android.util.Log
import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/**
 * User-initiated feedback channel — the Settings "Send feedback" form POSTs
 * here. Unlike crash reports there is no queue: sending is itself the consent,
 * so a failure is reported back to the dialog instead of being retried.
 *
 * No third-party SDK — plain HttpURLConnection + kotlinx.serialization.
 */
object Feedback {
    private const val TAG = "Feedback"
    private const val MAX_MESSAGE = 4000
    private const val MAX_CONTACT = 200

    /** Ingest endpoint. Same for all builds; override only for tests. */
    var endpoint: String = "https://nothing-modes.vercel.app/api/feedback"

    /**
     * @param category one of "bug" | "idea" | "other" (server clamps unknowns)
     * @return true when the server accepted the report
     */
    suspend fun send(
        context: Context,
        category: String,
        message: String,
        contact: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val packageInfo =
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }.getOrNull()

            val body =
                buildJsonObject {
                    put("app", "nothing-modes")
                    put("category", category)
                    put("message", message.trim().take(MAX_MESSAGE))
                    put("contact", contact.trim().take(MAX_CONTACT))
                    put("app_version", packageInfo?.versionName ?: "")
                    put("version_code", packageInfo?.longVersionCode?.toInt() ?: 0)
                    put("flavor", FeatureFlags.distribution)
                    put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("android_release", Build.VERSION.RELEASE)
                    put("sdk_int", Build.VERSION.SDK_INT)
                    put("client_time", System.currentTimeMillis())
                }.toString()

            runCatching { post(body) }
                .onFailure { Log.d(TAG, "feedback upload failed: ${it.message}") }
                .getOrDefault(false)
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
        } finally {
            conn.disconnect()
        }
    }
}

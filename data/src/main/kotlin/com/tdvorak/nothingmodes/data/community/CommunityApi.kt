package com.tdvorak.nothingmodes.data.community

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Client for the community library API (nothing-modes.vercel.app).
 *
 * Public reads: [list] / [fetchItem] — approved items only, server-sanitized.
 * Publishing: [submit] — queues for admin review; analysis findings come back
 * in the response so the UI can show why a submission was rejected.
 */
object CommunityApi {
    private const val BASE = "https://nothing-modes.vercel.app"

    private val json = Json { ignoreUnknownKeys = true }

    data class LibraryItem(
        val id: String,
        val type: String,
        val title: String,
        val description: String,
        val handle: String,
        val github: String,
        val contentHash: String,
        val summary: String,
        val capabilities: List<String>,
        val downloads: Long,
        val createdAt: String,
    )

    data class Finding(
        val severity: String,
        val code: String,
        val detail: String,
    )

    sealed class SubmitResult {
        data class Queued(val id: String, val verdict: String, val findings: List<Finding>) : SubmitResult()
        data class Rejected(val findings: List<Finding>) : SubmitResult()
        data class Duplicate(val id: String) : SubmitResult()
        data class Failed(val error: String) : SubmitResult()
    }

    suspend fun list(
        type: String? = null,
        query: String = "",
        sort: String = "newest",
        caps: List<String> = emptyList(),
    ): List<LibraryItem> =
        withContext(Dispatchers.IO) {
            val params = buildString {
                if (!type.isNullOrBlank()) append("type=").append(type)
                if (query.isNotBlank()) {
                    if (isNotEmpty()) append('&')
                    append("q=").append(URLEncoder.encode(query, "UTF-8"))
                }
                if (sort != "newest") {
                    if (isNotEmpty()) append('&')
                    append("sort=").append(URLEncoder.encode(sort, "UTF-8"))
                }
                if (caps.isNotEmpty()) {
                    if (isNotEmpty()) append('&')
                    append("caps=").append(URLEncoder.encode(caps.joinToString(","), "UTF-8"))
                }
            }
            val body = get("$BASE/api/library" + if (params.isNotEmpty()) "?$params" else "")
            json.parseToJsonElement(body).jsonObject["items"]
                ?.jsonArray
                ?.mapNotNull { el ->
                    val o = el.jsonObject
                    LibraryItem(
                        id = o.str("id") ?: return@mapNotNull null,
                        type = o.str("type") ?: "template",
                        title = o.str("title") ?: "",
                        description = o.str("description") ?: "",
                        handle = o.str("handle") ?: "",
                        github = o.str("github") ?: "",
                        contentHash = o.str("content_hash") ?: "",
                        summary = o.str("summary") ?: "",
                        capabilities =
                            o["capabilities"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList(),
                        downloads = o["downloads"]?.jsonPrimitive?.longOrNull ?: 0,
                        createdAt = o.str("created_at") ?: "",
                    )
                } ?: emptyList()
        }

    /** Returns the raw payload JsonObject for an approved item. */
    suspend fun fetchItem(id: String): JsonObject =
        withContext(Dispatchers.IO) {
            val body = get("$BASE/api/item?id=${URLEncoder.encode(id, "UTF-8")}")
            json.parseToJsonElement(body).jsonObject["item"]
                ?.jsonObject?.get("payload")?.jsonObject
                ?: throw IllegalStateException("item has no payload")
        }

    suspend fun submit(
        type: String,
        title: String,
        description: String,
        handle: String,
        email: String,
        github: String,
        payload: JsonElement,
    ): SubmitResult =
        withContext(Dispatchers.IO) {
            val req =
                buildJsonObject {
                    put("type", type)
                    put("title", title)
                    put("description", description)
                    put("handle", handle)
                    put("email", email)
                    put("github", github)
                    put("payload", payload)
                }
            val (code, body) = post("$BASE/api/share", req.toString())
            val o = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                ?: return@withContext SubmitResult.Failed("HTTP $code")
            val findings = parseFindings(o)
            when {
                code == 201 && o["ok"]?.jsonPrimitive?.content == "true" ->
                    SubmitResult.Queued(o.str("id") ?: "", o.str("verdict") ?: "ok", findings)
                code == 409 -> SubmitResult.Duplicate(o.str("id") ?: "")
                code == 422 -> SubmitResult.Rejected(findings)
                code == 429 -> SubmitResult.Failed("Rate limited — try again tomorrow")
                else -> SubmitResult.Failed(o.str("detail") ?: "HTTP $code")
            }
        }

    private fun parseFindings(o: JsonObject): List<Finding> =
        o["findings"]?.jsonArray?.mapNotNull { f ->
            val fo = f.jsonObject
            Finding(
                fo.str("severity") ?: "info",
                fo.str("code") ?: "",
                fo.str("detail") ?: "",
            )
        } ?: emptyList()

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.content

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${text.take(200)}")
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun post(
        url: String,
        body: String,
    ): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.doOutput = true
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            conn.disconnect()
        }
    }
}

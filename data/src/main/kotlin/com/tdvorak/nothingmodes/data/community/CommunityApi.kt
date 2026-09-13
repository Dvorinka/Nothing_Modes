package com.tdvorak.nothingmodes.data.community

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
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
        val preview: JsonObject? = null,
        /** Inline payload — present when the item came from the seed fallback. */
        val payload: JsonObject? = null,
    )

    data class Finding(
        val severity: String,
        val code: String,
        val detail: String,
    )

    sealed class SubmitResult {
        data class Queued(
            val id: String,
            val verdict: String,
            val findings: List<Finding>,
        ) : SubmitResult()

        data class Rejected(
            val findings: List<Finding>,
        ) : SubmitResult()

        data class Duplicate(
            val id: String,
        ) : SubmitResult()

        data class Failed(
            val error: String,
        ) : SubmitResult()
    }

    suspend fun list(
        type: String? = null,
        query: String = "",
        sort: String = "newest",
        caps: List<String> = emptyList(),
    ): List<LibraryItem> =
        withContext(Dispatchers.IO) {
            val params =
                buildString {
                    if (isNotEmpty()) append('&')
                    append("preview=1")
                    if (!type.isNullOrBlank()) {
                        append('&')
                        append("type=").append(type)
                    }
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
            val body =
                runCatching { get("$BASE/api/library" + if (params.isNotEmpty()) "?$params" else "") }
                    .getOrElse { return@withContext seedList(type, query, sort, caps) }
            json
                .parseToJsonElement(body)
                .jsonObject["items"]
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
                        preview = o["preview"]?.jsonObject,
                    )
                } ?: emptyList()
        }

    /** Returns the raw payload JsonObject for an approved item. */
    suspend fun fetchItem(id: String): JsonObject =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = get("$BASE/api/item?id=${URLEncoder.encode(id, "UTF-8")}")
                json
                    .parseToJsonElement(body)
                    .jsonObject["item"]
                    ?.jsonObject
                    ?.get("payload")
                    ?.jsonObject
                    ?: throw IllegalStateException("item has no payload")
            }.getOrNull()
                ?: seedPayload(id)
                ?: throw IllegalStateException("item not found")
        }

    /**
     * The live site falls back to seed.json when the API is unreachable; the
     * app does the same so the picker shows the same items the website shows.
     * Seed ids are title/file slugs, matching the site's normalizeSeed().
     */
    private fun seedList(
        type: String?,
        query: String,
        sort: String,
        caps: List<String>,
    ): List<LibraryItem> =
        runCatching {
            val items =
                json
                    .parseToJsonElement(get("$BASE/seed.json"))
                    .jsonObject["items"]
                    ?.jsonArray ?: return@runCatching emptyList()
            val q = query.trim().lowercase()
            items.mapIndexedNotNull { i, el ->
                val o = el.jsonObject
                val payload = o["payload"]?.jsonObject ?: return@mapIndexedNotNull null
                val itemType = o.str("type") ?: "template"
                if (!type.isNullOrBlank() && itemType != type) return@mapIndexedNotNull null
                val title = o.str("title") ?: return@mapIndexedNotNull null
                val desc = o.str("description") ?: ""
                if (q.isNotEmpty() &&
                    !title.lowercase().contains(q) &&
                    !desc.lowercase().contains(q) &&
                    !"nothing-modes".contains(q)
                ) {
                    return@mapIndexedNotNull null
                }
                var capabilities = emptyList<String>()
                var summary = ""
                var preview: JsonObject? = null
                if (itemType == "glyph") {
                    val frames = payload["frames"]?.jsonArray ?: emptyList()
                    val first = frames.firstOrNull()?.jsonObject
                    if (first != null) {
                        preview =
                            buildJsonObject {
                                put("size", if (payload["v"]?.jsonPrimitive?.intOrNull == 4) 13 else 25)
                                put("frameCount", frames.size)
                                first["p"]?.let { put("firstFrame", it) }
                                first["d"]?.let { put("duration", it) }
                            }
                    }
                    capabilities = listOf("glyph")
                    summary = "${frames.size} frames"
                } else {
                    capabilities =
                        payload["requiredCapabilities"]?.jsonArray
                            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    summary =
                        payload["automations"]?.jsonArray?.firstOrNull()
                            ?.jsonObject?.get("trigger")?.jsonObject
                            ?.get("type")?.jsonPrimitive?.contentOrNull
                            ?.replace('_', ' ') ?: "mode"
                }
                if (caps.isNotEmpty() && !caps.all { it in capabilities }) {
                    return@mapIndexedNotNull null
                }
                LibraryItem(
                    id = seedId(o, i),
                    type = itemType,
                    title = title,
                    description = desc,
                    handle = "nothing-modes",
                    github = "",
                    contentHash = "",
                    summary = summary,
                    capabilities = capabilities,
                    downloads = 0,
                    createdAt = "",
                    preview = preview,
                    payload = payload,
                )
            }.let { list -> if (sort == "alpha") list.sortedBy { it.title.lowercase() } else list }
        }.getOrDefault(emptyList())

    private fun seedId(
        o: JsonObject,
        index: Int,
    ): String =
        slugify(o.str("file")?.removeSuffix(".json") ?: o.str("title") ?: "")
            .ifBlank { "item-$index" }

    private suspend fun seedPayload(id: String): JsonObject? =
        runCatching {
            json
                .parseToJsonElement(get("$BASE/seed.json"))
                .jsonObject["items"]
                ?.jsonArray
                ?.withIndex()
                ?.firstNotNullOfOrNull { (i, el) ->
                    val o = el.jsonObject
                    val payload = o["payload"]?.jsonObject ?: return@firstNotNullOfOrNull null
                    if (seedId(o, i) == id) payload else null
                }
        }.getOrNull()

    private fun slugify(s: String): String =
        s.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

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
            val o =
                runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
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

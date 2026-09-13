package com.tdvorak.nothingmodes.nothing

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Client for fetching a single Glyph Museum post (app.glyphmuseum.com) by id.
 *
 * The museum publishes each design through a public PostgREST endpoint — the
 * same one the web app itself uses, gated by its public anon key. Per the
 * museum's developer rules (glyphmuseum.com/developers) this fetches one
 * design at a time, only when the user asks for it; no catalog scraping.
 * Attribution is re-attached as the open-format `meta` block so credit stays
 * with the original author.
 */
object GlyphMuseumApi {
    private const val REST_BASE = "https://supabase.pauwma.com/rest/v1"
    const val POST_URL_BASE = "https://app.glyphmuseum.com/post/"

    // Public anon key shipped in the museum's own web bundle. RLS-limited to
    // published, non-hidden posts — equivalent to what any browser can read.
    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzU3MjgyNDAwLCJleHAiOjE5MTUwNDg4MDB9." +
            "LQTVbnnnGU70OV89j-rSGPDi1XOWt4zT3A2hMHRAQfg"

    data class MuseumPost(
        val id: Long,
        val title: String,
        val author: String?,
        val designJson: String,
        val url: String,
    )

    /** Post id from a `…glyphmuseum.com/post/<id>` link, or null. */
    fun postIdFrom(uri: Uri?): Long? {
        uri ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (host != "app.glyphmuseum.com" && host != "glyphmuseum.com" && host != "www.glyphmuseum.com") return null
        val segments = uri.pathSegments
        val postIdx = segments.indexOfFirst { it.equals("post", ignoreCase = true) }
        return segments.getOrNull(postIdx + 1)?.toLongOrNull()
    }

    /** First supported URL inside arbitrary shared text, or null. */
    fun postIdFromText(text: String?): Long? {
        text ?: return null
        val m = Regex("""https?://[^\s"'<>]+""").find(text) ?: return null
        return postIdFrom(Uri.parse(m.value))
    }

    /**
     * Fetch one published post and return it with its design JSON. The `data`
     * column is the open-format design; `meta` attribution is merged in so
     * imports keep author + post link per the museum's format rules.
     */
    suspend fun fetchPost(postId: Long): MuseumPost? =
        withContext(Dispatchers.IO) {
            val rows =
                getJson(
                    "$REST_BASE/posts?id=eq.$postId" +
                        "&select=id,title,data,user_uid,is_hidden&is_hidden=eq.false&limit=1",
                ) ?: return@withContext null
            val row = rows.optJSONObject(0) ?: return@withContext null
            val data = row.optString("data").takeIf { it.isNotBlank() } ?: return@withContext null

            val author =
                row.optString("user_uid").takeIf { it.isNotBlank() }?.let { uid ->
                    val profiles =
                        getJson(
                            "$REST_BASE/profiles?uid=eq." +
                                URLEncoder.encode(uid, "UTF-8") +
                                "&select=handle,display_name&limit=1",
                        )
                    profiles?.optJSONObject(0)?.let { p ->
                        p.optString("handle").takeIf { it.isNotBlank() }
                            ?: p.optString("display_name").takeIf { it.isNotBlank() }
                    }
                }

            val design = JSONObject(data)
            val meta = design.optJSONObject("meta") ?: JSONObject().also { design.put("meta", it) }
            if (author != null && meta.optString("author").isBlank()) meta.put("author", author)
            meta.put("postId", postId)
            meta.put("url", "$POST_URL_BASE$postId")

            // Reject early — an invalid design should fail before we claim an import.
            runCatching { GlyphFrameCodec.decode(design.toString()) }.getOrNull()
                ?: return@withContext null

            MuseumPost(
                id = postId,
                title = row.optString("title").ifBlank { "post_$postId" },
                author = author,
                designJson = design.toString(),
                url = "$POST_URL_BASE$postId",
            )
        }

    /**
     * Fetch post [postId] and store it in [CustomGlyphStore]. Returns the
     * stored design name, or null when the post is missing/invalid.
     */
    suspend fun importPost(
        context: Context,
        postId: Long,
    ): String? =
        runCatching { fetchPost(postId) }.getOrNull()?.let { post ->
            CustomGlyphStore(context).import(post.designJson, post.title)
        }

    private fun getJson(url: String): org.json.JSONArray? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("apikey", ANON_KEY)
        conn.setRequestProperty("Authorization", "Bearer $ANON_KEY")
        conn.setRequestProperty("Accept", "application/json")
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: return null
            if (code !in 200..299) return null
            org.json.JSONArray(text)
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}

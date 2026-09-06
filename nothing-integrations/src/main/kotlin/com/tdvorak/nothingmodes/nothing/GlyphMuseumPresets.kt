package com.tdvorak.nothingmodes.nothing

import android.content.Context
import org.json.JSONObject

/**
 * Bundled Glyph Museum designs shipped as assets under `glyph_presets/`.
 *
 * Each asset is an open-format JSON file. The asset file name (without
 * `.json`) is the preset key; the JSON `meta` block keeps the original
 * author, post id, and url.
 */
class GlyphMuseumPresets(
    context: Context,
) {
    private val assetManager = context.assets

    /** All bundled preset keys, sorted alphabetically. */
    fun names(): List<String> =
        runCatching {
            assetManager.list(PRESETS_DIR)
                ?.filter { it.endsWith(".json") }
                ?.map { it.removeSuffix(".json") }
                ?.sorted()
                .orEmpty()
        }.getOrDefault(emptyList())

    /** Decode a bundled design by [key], or null if missing/broken. */
    fun design(key: String): GlyphFrameCodec.Design? =
        runCatching {
            val json = assetManager.open("$PRESETS_DIR/$key.json").bufferedReader().use { it.readText() }
            GlyphFrameCodec.decode(json)
        }.getOrNull()

    /** Metadata for a bundled preset (cheap — no full frame decode). */
    data class Info(
        val key: String,
        val title: String,
        val author: String?,
        val postId: Long?,
        val url: String?,
        val source: String,
        val gridSize: Int,
        val frameCount: Int,
    )

    /** All bundled presets with display metadata, sorted by title. */
    fun infos(): List<Info> =
        names().mapNotNull { info(it) }.sortedBy { it.title }

    /** Read lightweight metadata for [key] without decoding the full frame buffer. */
    fun info(key: String): Info? =
        runCatching {
            val json = assetManager.open("$PRESETS_DIR/$key.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val meta = root.optJSONObject("meta")
            val version = root.optInt("v", 1)
            val gridSize = if (version == 4) 13 else 25
            val frames = root.optJSONArray("frames")
            Info(
                key = key,
                title = titleForKey(key),
                author = meta?.optString("author")?.takeIf { it.isNotBlank() },
                postId = meta?.optLong("postId", -1)?.takeIf { it > 0 },
                url = meta?.optString("url")?.takeIf { it.isNotBlank() },
                source = GLYPH_MUSEUM,
                gridSize = gridSize,
                frameCount = frames?.length() ?: 0,
            )
        }.getOrNull()

    /** A human title derived from the asset key. */
    fun titleForKey(key: String): String {
        val is4a = key.endsWith("_4a")
        val base = if (is4a) key.removeSuffix("_4a") else key
        val words = base.split('_').filter { it.isNotBlank() }
        val title = words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return if (is4a) "$title (4a)" else title
    }

    /** The Glyph Museum post url for [key], or null if absent. */
    fun url(key: String): String? = info(key)?.url

    companion object {
        private const val PRESETS_DIR = "glyph_presets"
        private const val GLYPH_MUSEUM = "https://app.glyphmuseum.com"
    }
}

package com.tdvorak.nothingmodes.nothing

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * On-device store for custom Glyph Matrix designs.
 *
 * Each entry keeps the full open-format design (frames + `meta` attribution)
 * as raw JSON, so imports from the GlyphMatrixEditor / Glyph Museum round-trip
 * without losing credit. File: `custom_glyphs.json` in app files.
 */
class CustomGlyphStore(
    context: Context,
) {
    private val file = File(context.filesDir, "custom_glyphs.json")

    fun names(): List<String> =
        read()
            .keys()
            .asSequence()
            .toList()
            .sorted()

    /** @return the stored open-format JSON object for [name], or null. */
    fun designJson(name: String): String? = read().optJSONObject(name)?.toString()

    /** @return the decoded design for [name], or null when absent/broken. */
    fun design(name: String): GlyphFrameCodec.Design? = designJson(name)?.let { runCatching { GlyphFrameCodec.decode(it) }.getOrNull() }

    /**
     * Save [designJson] under [name]. The JSON must already be a valid design
     * — callers build it via [GlyphFrameCodec.encode] or pass an import
     * through [import]. Returns false on invalid input.
     */
    fun save(
        name: String,
        designJson: String,
    ): Boolean {
        val clean = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")
        if (clean.isEmpty()) return false
        runCatching { GlyphFrameCodec.decode(designJson) }.getOrNull() ?: return false
        val all = read()
        all.put(clean, JSONObject(designJson))
        return write(all)
    }

    /**
     * Save a raw pixel frame (25x25, 0-4095) as a single-frame design.
     */
    fun saveFrame(
        name: String,
        frame: IntArray,
        size: Int = 25,
    ): Boolean = save(name, GlyphFrameCodec.encode(listOf(frame), size))

    /**
     * Import an exported design (GlyphMatrixEditor / Glyph Museum JSON).
     * [suggestedName] wins; falls back to the file's `meta.author`-less
     * generated name. Returns the stored name, or null on invalid JSON.
     */
    fun import(
        designJson: String,
        suggestedName: String?,
    ): String? {
        val design = runCatching { GlyphFrameCodec.decode(designJson) }.getOrNull() ?: return null
        val base =
            suggestedName
                ?.trim()
                ?.lowercase()
                ?.replace(Regex("[^a-z0-9_-]"), "_")
                ?.takeIf { it.isNotBlank() }
                ?: "import_${System.currentTimeMillis() % 1_000_000}"
        var name = base
        var n = 2
        while (designJson(name) != null) name = "${base}_${n++}"
        return if (save(name, designJson)) name else null
    }

    fun delete(name: String): Boolean {
        val all = read()
        all.remove(name)
        return write(all)
    }

    private fun read(): JSONObject = runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())

    private fun write(obj: JSONObject): Boolean =
        runCatching {
            file.writeText(obj.toString())
            true
        }.getOrDefault(false)
}

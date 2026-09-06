package com.tdvorak.nothingmodes.nothing

import android.content.Context
import java.io.IOException

/**
 * Bundled Glyph Museum designs shipped as assets under `glyph_presets/`.
 *
 * Each asset is an open-format JSON file. The asset file name (without
 * `.json`) is the preset key; the JSON `meta` block keeps the original
 * author and post id.
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

    companion object {
        private const val PRESETS_DIR = "glyph_presets"
    }
}

package com.tdvorak.nothingmodes.nothing

import org.json.JSONArray
import org.json.JSONObject

/**
 * Codec for the open Glyph Museum design format
 * (https://glyphmuseum.com/developers — same format the GlyphMatrixEditor
 * exports):
 *
 * ```json
 * { "v": 1, "meta": { "author": "...", "postId": 0, "url": "..." },
 *   "frames": [ { "d": 100, "p": [489 ints 0-255] } ] }
 * ```
 *
 * `p` holds only the LEDs that physically exist — the matrix is a circle cut
 * out of a square grid, so each row contributes its [rowWidths] count, in
 * reading order. v:1 = Phone (3) 25x25/489 LEDs, v:4 = Phone (4a) Pro
 * 13x13/137 LEDs.
 *
 * Per the published rules: `meta` is optional attribution and must be carried
 * through untouched when a design is re-saved; unknown keys are ignored and a
 * missing or malformed `meta` never fails an import.
 */
object GlyphFrameCodec {

    data class Frame(
        val pixels: IntArray, // full square matrix, row-major, 0-4095
        val durationMs: Int?,
    )

    data class Design(
        val version: Int,
        val gridSize: Int,
        val frames: List<Frame>,
        val author: String?, // meta.author — always show when present
        val postId: Long?,
        val url: String?,
    ) {
        val animated: Boolean get() = frames.size > 1
    }

    /** LEDs per row, top to bottom — the physical circle inside the grid. */
    val ROWS_25 = intArrayOf(7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25, 25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7)
    val ROWS_13 = intArrayOf(5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5)

    fun rowWidths(size: Int): IntArray =
        when (size) {
            25 -> ROWS_25
            13 -> ROWS_13
            else -> IntArray(size) { size }
        }

    /** Column offset of the first LED in [row] — rows are centered. */
    fun rowStart(size: Int, row: Int): Int = (size - rowWidths(size)[row]) / 2

    /** Number of physical LEDs (489 for 25x25, 137 for 13x13). */
    fun ledCount(size: Int): Int = rowWidths(size).sum()

    /**
     * Decode the open JSON format into a [Design]. Throws
     * [IllegalArgumentException] on anything that is not a usable design.
     */
    fun decode(json: String): Design {
        val root = JSONObject(json)
        val v = root.optInt("v", 1)
        val size =
            when (v) {
                4 -> 13
                else -> 25
            }
        val framesJson =
            root.optJSONArray("frames")
                ?: throw IllegalArgumentException("no frames")
        if (framesJson.length() == 0) throw IllegalArgumentException("no frames")
        val expected = ledCount(size)
        val widths = rowWidths(size)
        val frames =
            (0 until framesJson.length()).map { i ->
                val f = framesJson.getJSONObject(i)
                val p =
                    f.optJSONArray("p")
                        ?: throw IllegalArgumentException("frame ${i + 1} has no pixels")
                if (p.length() != expected) {
                    throw IllegalArgumentException(
                        "frame ${i + 1} has ${p.length()} pixels, expected $expected",
                    )
                }
                val grid = IntArray(size * size)
                var idx = 0
                for (row in 0 until size) {
                    val start = (size - widths[row]) / 2
                    for (c in 0 until widths[row]) {
                        val v8 = p.optInt(idx++, 0).coerceIn(0, 255)
                        grid[row * size + start + c] = v8 * 4095 / 255
                    }
                }
                Frame(grid, f.optInt("d", 0).takeIf { it > 0 })
            }
        // meta is best-effort attribution — absent/malformed must never fail.
        val meta = root.optJSONObject("meta")
        return Design(
            version = v,
            gridSize = size,
            frames = frames,
            author = meta?.optString("author")?.takeIf { it.isNotBlank() },
            postId = meta?.optLong("postId")?.takeIf { it > 0 },
            url = meta?.optString("url")?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Encode full-square frames (row-major, 0-4095) into the open JSON format.
     * [durationsMs] is optional per-frame; [author]/[url] preserve attribution
     * when re-exporting a design that already carries a `meta` block.
     */
    fun encode(
        frames: List<IntArray>,
        size: Int,
        durationsMs: List<Int?> = emptyList(),
        author: String? = null,
        postId: Long? = null,
        url: String? = null,
    ): String {
        val widths = rowWidths(size)
        val root = JSONObject()
        root.put("v", if (size == 13) 4 else 1)
        if (author != null || postId != null || url != null) {
            val meta = JSONObject()
            author?.let { meta.put("author", it) }
            postId?.let { meta.put("postId", it) }
            url?.let { meta.put("url", it) }
            root.put("meta", meta)
        }
        val arr = JSONArray()
        frames.forEachIndexed { i, grid ->
            val f = JSONObject()
            durationsMs.getOrNull(i)?.let { if (it > 0) f.put("d", it) }
            val p = JSONArray()
            for (row in 0 until size) {
                val start = (size - widths[row]) / 2
                for (c in 0 until widths[row]) {
                    p.put(grid[row * size + start + c].coerceIn(0, 4095) * 255 / 4095)
                }
            }
            f.put("p", p)
            arr.put(f)
        }
        root.put("frames", arr)
        return root.toString()
    }
}

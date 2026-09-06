package com.tdvorak.nothingmodes.nothing

/**
 * Hand-authored 5x7 pixel font for the Glyph Matrix.
 *
 * The SDK's NDot letter table renders digits only ~5 dots tall and draws a
 * closed-loop "9" that reads badly inside the progress ring. These glyphs are
 * the classic 5x7 pixel shapes — crisp, evenly spaced, and fully under our
 * control. Supported characters: 0-9, %, -, . and space.
 *
 * Each glyph is 5 dots wide; characters are spaced by 1 dot. Height is 7.
 */
object GlyphDigitFont {
    const val GLYPH_W = 5
    const val GLYPH_H = 7
    const val SPACING = 1

    /** True when every character in [text] has a glyph. */
    fun supports(text: String): Boolean = text.all { it in glyphs }

    /** Pixel width of [text] when drawn with this font. */
    fun measure(text: String): Int =
        if (text.isEmpty()) 0 else text.length * GLYPH_W + (text.length - 1) * SPACING

    /**
     * Draw [text] into [out] (a size*size matrix) at top-left ([x], [y]).
     * [on] is the dot brightness (0-4095). Returns false if any character is
     * unsupported; callers should fall back to another renderer.
     */
    fun draw(
        text: String,
        out: IntArray,
        size: Int,
        x: Int,
        y: Int,
        on: Int,
    ): Boolean {
        if (!supports(text)) return false
        var cx = x
        text.forEach { ch ->
            val rows = glyphs[ch] ?: return false
            if (ch != ' ') {
                rows.forEachIndexed { dy, row ->
                    row.forEachIndexed { dx, c ->
                        if (c == '#') {
                            val px = cx + dx
                            val py = y + dy
                            if (px in 0 until size && py in 0 until size) {
                                out[py * size + px] = on
                            }
                        }
                    }
                }
            }
            cx += GLYPH_W + SPACING
        }
        return true
    }

    private val glyphs: Map<Char, List<String>> =
        mapOf(
            '0' to listOf(
                ".###.",
                "#...#",
                "#..##",
                "#.#.#",
                "##..#",
                "#...#",
                ".###.",
            ),
            '1' to listOf(
                "..#..",
                ".##..",
                "..#..",
                "..#..",
                "..#..",
                "..#..",
                ".###.",
            ),
            '2' to listOf(
                ".###.",
                "#...#",
                "....#",
                "..##.",
                ".#...",
                "#....",
                "#####",
            ),
            '3' to listOf(
                ".###.",
                "#...#",
                "....#",
                "..##.",
                "....#",
                "#...#",
                ".###.",
            ),
            '4' to listOf(
                "...#.",
                "..##.",
                ".#.#.",
                "#..#.",
                "#####",
                "...#.",
                "...#.",
            ),
            '5' to listOf(
                "#####",
                "#....",
                "####.",
                "....#",
                "....#",
                "#...#",
                ".###.",
            ),
            '6' to listOf(
                ".###.",
                "#....",
                "#....",
                "####.",
                "#...#",
                "#...#",
                ".###.",
            ),
            '7' to listOf(
                "#####",
                "....#",
                "...#.",
                "..#..",
                "..#..",
                "..#..",
                "..#..",
            ),
            '8' to listOf(
                ".###.",
                "#...#",
                "#...#",
                ".###.",
                "#...#",
                "#...#",
                ".###.",
            ),
            '9' to listOf(
                ".###.",
                "#...#",
                "#...#",
                ".####",
                "....#",
                "#...#",
                ".###.",
            ),
            '%' to listOf(
                "##..#",
                "##.#.",
                "...#.",
                "..#..",
                ".#...",
                ".#.##",
                "#..##",
            ),
            '-' to listOf(
                ".....",
                ".....",
                ".....",
                ".###.",
                ".....",
                ".....",
                ".....",
            ),
            '.' to listOf(
                ".....",
                ".....",
                ".....",
                ".....",
                ".....",
                ".##..",
                ".##..",
            ),
            ' ' to List(GLYPH_H) { "....." },
        )
}

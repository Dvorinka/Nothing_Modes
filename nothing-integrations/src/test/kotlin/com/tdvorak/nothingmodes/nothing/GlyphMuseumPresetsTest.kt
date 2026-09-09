package com.tdvorak.nothingmodes.nothing

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GlyphMuseumPresetsTest {
    @Test
    fun `all bundled assets decode and round-trip`() {
        val dir = File("src/main/assets/glyph_presets")
        val files = dir.listFiles { _, name -> name.endsWith(".json") }?.sortedBy { it.name } ?: emptyList()

        org.junit.Assert.assertTrue("No bundled presets found", files.isNotEmpty())
        org.junit.Assert.assertEquals(28, files.size)

        for (file in files) {
            val json = file.readText()
            val design = GlyphFrameCodec.decode(json)
            val active = design.frames.sumOf { GlyphFrameCodec.activeCount(it.pixels, design.gridSize) }
            val avgActive = if (design.frames.isNotEmpty()) active / design.frames.size else 0
            println(
                "%-25s v=${design.version} grid=${design.gridSize} frames=${design.frames.size} avgActive=$avgActive author=${design.author ?: "—"} post=${design.postId ?: "—"}"
                    .format(file.name),
            )

            // Re-encode and re-decode to make sure round-trip is safe.
            val re = GlyphFrameCodec.rescaleDesign(design, design.gridSize)
            val rejson = GlyphFrameCodec.encode(re.frames.map { it.pixels }, re.gridSize, re.frames.map { it.durationMs })
            val round = GlyphFrameCodec.decode(rejson)
            assert(round.frames.size == design.frames.size)
        }
    }
}

package com.tdvorak.nothingmodes.engine.runtime

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ActiveMediaTest {
    @Test
    fun `update emits media info`() =
        runTest {
            ActiveMedia.update("com.spotify.music", "Artist", "Title")
            val info = ActiveMedia.info.first()
            assertEquals("com.spotify.music", info?.packageName)
            assertEquals("Artist", info?.artist)
            assertEquals("Title", info?.title)
        }

    @Test
    fun `update overwrites previous value`() =
        runTest {
            ActiveMedia.update("com.old", "Old", "Old")
            ActiveMedia.update("com.new", "New", "New")
            val info = ActiveMedia.info.first()
            assertEquals("com.new", info?.packageName)
        }

    @Test
    fun `initial state is null`() =
        runTest {
            val info = ActiveMedia.info.first()
            assertNull(info)
        }
}

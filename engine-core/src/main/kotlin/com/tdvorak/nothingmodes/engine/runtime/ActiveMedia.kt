package com.tdvorak.nothingmodes.engine.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Current media session metadata, written by MediaSessionMonitor and
 * read by actions such as the now-playing glyph preset.
 */
object ActiveMedia {
    data class MediaInfo(
        val packageName: String?,
        val artist: String?,
        val title: String?,
    )

    private val _info = MutableStateFlow<MediaInfo?>(null)
    val info: StateFlow<MediaInfo?> = _info.asStateFlow()

    fun update(
        packageName: String?,
        artist: String?,
        title: String?,
    ) {
        _info.value = MediaInfo(packageName, artist, title)
    }
}

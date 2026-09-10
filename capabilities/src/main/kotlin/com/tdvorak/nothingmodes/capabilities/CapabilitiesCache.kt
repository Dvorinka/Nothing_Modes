package com.tdvorak.nothingmodes.capabilities

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * App-wide cache for [DeviceCapabilities]. Detection reads a dozen permissions,
 * sysfs nodes, and Shizuku — far too slow to re-run on every screen open.
 * Screens should seed from [peek] for an instant first paint and call
 * [refresh] in the background when fresh data matters.
 */
object CapabilitiesCache {
    private val _current = MutableStateFlow<DeviceCapabilities?>(null)
    val current: StateFlow<DeviceCapabilities?> = _current.asStateFlow()

    /** The last detected capabilities, or null before the first detection. */
    fun peek(): DeviceCapabilities? = _current.value

    /** Runs full detection and updates the cache. */
    suspend fun refresh(context: Context): DeviceCapabilities {
        val caps = withContext(Dispatchers.IO) { CapabilityDetector(context).detect() }
        _current.value = caps
        return caps
    }
}

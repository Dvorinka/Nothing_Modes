package com.tdvorak.nothingmodes.engine.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory holder for active notification snapshots.
 * Updated by the notification listener service; read by the state provider.
 */
object ActiveNotifications {
    private val _snapshots = MutableStateFlow<List<NotificationSnapshot>>(emptyList())
    val snapshots: StateFlow<List<NotificationSnapshot>> = _snapshots.asStateFlow()

    fun update(snapshots: List<NotificationSnapshot>) {
        _snapshots.value = snapshots
    }

    data class NotificationSnapshot(
        val pkg: String,
        val title: String,
        val text: String,
    )
}

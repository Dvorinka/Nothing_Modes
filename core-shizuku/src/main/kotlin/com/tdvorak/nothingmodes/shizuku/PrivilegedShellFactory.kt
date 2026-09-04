package com.tdvorak.nothingmodes.shizuku

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lazily resolves a [PrivilegedShell] on each call.
 * Unlike injecting a static [PrivilegedShell?] at construction time,
 * this re-checks Shizuku status on every [resolve] call, so that
 * a user who installs/authorizes Shizuku after app launch gets
 * a working shell without requiring a process restart.
 */
class PrivilegedShellFactory(
    private val context: Context,
    private val gateway: ShizukuGateway,
) {
    private var cached: PrivilegedShell? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Returns a [PrivilegedShell] if Shizuku is authorized, null otherwise.
     *  Caches the shell instance; re-checks gateway status on each call. */
    fun resolve(): PrivilegedShell? {
        if (gateway.status() != ShizukuGatewayStatus.AUTHORIZED) {
            cached = null
            return null
        }
        if (cached == null) {
            cached = ShizukuPrivilegedShell(context, gateway, scope)
        }
        return cached
    }
}

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
     *  Caches the shell instance; re-checks gateway status on each call.
     *  Also the choke point for user-visible loss reporting: a dead/revoked
     *  Shizuku surfaces a throttled heads-up instead of failing silently in
     *  the action log; a working one gets remembered and clears the alert. */
    fun resolve(): PrivilegedShell? {
        if (gateway.status() != ShizukuGatewayStatus.AUTHORIZED) {
            cached = null
            ShizukuStatusNotifier.notifyUnavailable(context)
            return null
        }
        ShizukuStatusNotifier.markAuthorized(context)
        ShizukuStatusNotifier.cancel(context)
        if (cached == null) {
            cached = ShizukuPrivilegedShell(context, gateway, scope)
        }
        return cached
    }
}

package com.tdvorak.nothingmodes.update

import androidx.activity.ComponentActivity

/**
 * GitHub flavor: updates are handled by UpdateViewModel/UpdateManager, so
 * the platform in-app update hook is a no-op here.
 */
class PlatformInAppUpdate(
    @Suppress("unused") activity: ComponentActivity,
) {
    fun check() = Unit

    fun onResume() = Unit
}

package com.tdvorak.nothingmodes.engine.runtime

/**
 * Runtime feature flags. Set once from the app module during [Application.onCreate]
 * based on the active product flavor (GitHub vs Play).
 */
object FeatureFlags {
    var distribution: String = "github"
    var enableInAppUpdates: Boolean = true
    var enableLockScreen: Boolean = true
}

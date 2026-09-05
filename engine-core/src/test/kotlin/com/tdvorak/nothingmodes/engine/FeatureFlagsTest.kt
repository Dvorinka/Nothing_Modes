package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeatureFlagsTest {
    @Test
    fun `github defaults are permissive`() {
        FeatureFlags.distribution = "github"
        FeatureFlags.enableInAppUpdates = true
        FeatureFlags.enableLockScreen = true

        assertEquals("github", FeatureFlags.distribution)
        assertEquals(true, FeatureFlags.enableInAppUpdates)
        assertEquals(true, FeatureFlags.enableLockScreen)
    }

    @Test
    fun `play defaults disable policy-sensitive features`() {
        FeatureFlags.distribution = "play"
        FeatureFlags.enableInAppUpdates = false
        FeatureFlags.enableLockScreen = false

        assertEquals("play", FeatureFlags.distribution)
        assertEquals(false, FeatureFlags.enableInAppUpdates)
        assertEquals(false, FeatureFlags.enableLockScreen)
    }
}

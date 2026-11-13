package dev.enzotraini.tvlauncher.apps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledAppsTest {
    @Test
    fun leanbackOpensBeforeTheNormalLauncher() {
        var launcher = 0
        val opened = firstWorkingLaunch(
            leanback = { true },
            launcher = { launcher += 1; true },
        )
        assertTrue(opened)
        assertTrue(launcher == 0)
    }

    @Test
    fun failedLeanbackFallsBackToTheLauncher() {
        val opened = firstWorkingLaunch(
            leanback = { false },
            launcher = { true },
        )
        assertTrue(opened)
    }

    @Test
    fun missingIntentsDoNotOpen() {
        assertFalse(firstWorkingLaunch(leanback = null, launcher = null))
        assertFalse(firstWorkingLaunch(leanback = { false }, launcher = { false }))
    }
}

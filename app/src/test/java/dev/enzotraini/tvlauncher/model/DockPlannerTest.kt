package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DockPlannerTest {
    @Test
    fun keepsConfiguredOrderAndMarksMissingApps() {
        val entries = listOf(
            DockEntry("com.netflix.ninja", "Netflix"),
            DockEntry("com.globo.globotv", "Globoplay"),
            DockEntry("com.stremio.one", "Stremio"),
        )
        val slots = planDock(entries, setOf("com.netflix.ninja", "com.stremio.one"))
        assertEquals(
            listOf("com.netflix.ninja", "com.globo.globotv", "com.stremio.one"),
            slots.map { it.packageName },
        )
        assertEquals(listOf(true, false, true), slots.map { it.installed })
        assertEquals(listOf("Netflix", "Globoplay", "Stremio"), slots.map { it.label })
    }

    @Test
    fun gridOmitsDockAndHiddenPackages() {
        val visible = gridPackages(
            installed = listOf(
                "com.netflix.ninja",
                "com.plexapp.android",
                "org.jellyfin.androidtv",
                "com.tcl.tv",
            ),
            dockPackages = setOf("com.netflix.ninja"),
            hidden = setOf("com.tcl.tv"),
        )
        assertEquals(listOf("com.plexapp.android", "org.jellyfin.androidtv"), visible)
    }
}

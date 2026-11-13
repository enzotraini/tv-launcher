package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCatalogTest {
    @Test
    fun channelsAreOpenBroadcastersAndFreeShelfIsOfficial() {
        assertEquals(listOf("Globo", "Band", "SBT", "Record", "Cultura"), LiveCatalog.channels.map { it.name })
        assertEquals(
            listOf("Pluto TV", "Plex", "Libreflix", "Spcine Play", "YouTube"),
            LiveCatalog.free.map { it.name },
        )
        assertEquals("com.globo.globotv", LiveCatalog.channels.first { it.id == "globo" }.packages.single())
        assertEquals("br.com.band.guiatv", LiveCatalog.channels.first { it.id == "band" }.packages.single())
        assertEquals("br.com.sbt.mais", LiveCatalog.channels.first { it.id == "sbt" }.packages.single())
        assertEquals(
            listOf("com.mobilus.recordplay.tv", "com.mobilus.recordplay"),
            LiveCatalog.channels.first { it.id == "record" }.packages,
        )
        assertTrue(LiveCatalog.channels.first { it.id == "cultura" }.packages.isEmpty())
        assertEquals("tv.pluto.android", LiveCatalog.free.first { it.id == "pluto" }.packages.single())
        assertEquals("org.libreflix.app", LiveCatalog.free.first { it.id == "libreflix" }.packages.single())
        assertEquals("com.eitv.spcineplay", LiveCatalog.free.first { it.id == "spcine" }.packages.single())
        val joined = (LiveCatalog.channels.map { it.name } + LiveCatalog.free.map { it.name }).joinToString(" ")
        assertTrue(!joined.contains("Hora do Faro"))
        assertTrue(LiveCatalog.installedPackages().none { it.contains("iptv", ignoreCase = true) })
    }

    @Test
    fun installedOfficialAppBeatsTheTuner() {
        val globo = LiveCatalog.channels.first { it.id == "globo" }
        assertEquals(
            ChannelTarget.App("com.globo.globotv"),
            resolveChannel(globo, setOf("com.globo.globotv"), "com.tcl.tv"),
        )
        assertEquals(
            ChannelTarget.Tuner("com.tcl.tv"),
            resolveChannel(globo, emptySet(), "com.tcl.tv"),
        )
        assertEquals(
            ChannelTarget.Unavailable,
            resolveChannel(globo, emptySet(), "  "),
        )
        assertEquals("Globoplay", channelHint(ChannelTarget.App("com.globo.globotv"), "Globoplay"))
        assertEquals("Sintonizador", channelHint(ChannelTarget.Tuner("com.tcl.tv"), "Globoplay"))
        val record = LiveCatalog.channels.first { it.id == "record" }
        assertEquals(
            ChannelTarget.App("com.mobilus.recordplay.tv"),
            resolveChannel(record, setOf("com.mobilus.recordplay", "com.mobilus.recordplay.tv"), "com.tcl.tv"),
        )
        assertNull(resolveService(LiveCatalog.free.first { it.id == "plex" }, emptySet()))
        assertEquals(
            "com.google.android.youtube.tv",
            resolveService(LiveCatalog.free.first { it.id == "youtube" }, setOf("com.google.android.youtube.tv")),
        )
    }
}

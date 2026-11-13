package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WallpapersTest {
    @Test
    fun prefsRoundTripAndPlaylistSkipsHidden() {
        val pref = WallpaperPref(
            hidden = setOf("b.jpg"),
            quarters = mapOf("a.jpg" to 5, "c.jpg" to -1),
            shuffle = true,
            intervalSeconds = 9,
            idleMinutes = 0,
        ).normalized()
        assertEquals(setOf("b.jpg"), pref.hidden)
        assertEquals(1, pref.quarters.getValue("a.jpg"))
        assertEquals(3, pref.quarters.getValue("c.jpg"))
        assertEquals(10, pref.intervalSeconds)
        assertEquals(1, pref.idleMinutes)
        val parsed = parseWallpaperPref(formatWallpaperPref(pref))
        assertEquals(pref.hidden, parsed.hidden)
        assertEquals(pref.quarters, parsed.quarters)
        assertEquals(pref.shuffle, parsed.shuffle)
        assertEquals(pref.intervalSeconds, parsed.intervalSeconds)
        assertEquals(pref.idleMinutes, parsed.idleMinutes)
        assertEquals(listOf("a.jpg", "c.jpg"), playlist(listOf("a.jpg", "b.jpg", "c.jpg"), pref.copy(shuffle = false), 1L))
        val shuffled = playlist(listOf("a.jpg", "c.jpg", "d.jpg"), pref.copy(shuffle = true), 7L)
        assertEquals(shuffled, playlist(listOf("a.jpg", "c.jpg", "d.jpg"), pref.copy(shuffle = true), 7L))
        assertEquals(setOf("a.jpg", "c.jpg", "d.jpg"), shuffled.toSet())
        assertEquals(30, nextInterval(15))
        assertEquals(15, nextInterval(120))
        assertEquals(5, nextIdle(1))
        assertEquals(1, nextIdle(30))
    }

    @Test
    fun photoListIgnoresOtherFilesAndDedupesNames() {
        val root = File(System.getProperty("java.io.tmpdir"), "wallpapers-${System.nanoTime()}")
        val external = File(root, "external").apply { mkdirs() }
        val internal = File(root, "internal").apply { mkdirs() }
        File(external, "B.JPG").writeBytes(byteArrayOf(1))
        File(external, "nota.txt").writeText("não")
        File(external, "a.png").writeBytes(byteArrayOf(2))
        File(internal, "b.jpg").writeBytes(byteArrayOf(3))
        File(internal, "c.webp").writeBytes(byteArrayOf(4))
        val names = collectPhotos(listOf(external, internal)).map { it.name.lowercase() }
        assertEquals(listOf("a.png", "b.jpg", "c.webp"), names)
        assertTrue(photoFiles(File(root, "ausente")).isEmpty())
    }
}

package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFitTest {
    @Test
    fun exifAndUserRotationAddUp() {
        assertEquals(0, PhotoFit.fromExif(1).clockwiseDegrees)
        assertEquals(90, PhotoFit.fromExif(6).clockwiseDegrees)
        assertEquals(270, PhotoFit.fromExif(8).clockwiseDegrees)
        assertEquals(180, PhotoFit.fromExif(3).clockwiseDegrees)
        assertTrue(PhotoFit.fromExif(2).flipHorizontal)
        assertEquals(90, PhotoFit.totalClockwise(PhotoFit.fromExif(1), 1))
        assertEquals(270, PhotoFit.totalClockwise(PhotoFit.fromExif(1), -1))
        assertEquals(180, PhotoFit.totalClockwise(PhotoFit.fromExif(6), 1))
        assertEquals(0, PhotoFit.normalizedQuarters(4))
        assertEquals(3, PhotoFit.normalizedQuarters(-1))
    }

    @Test
    fun portraitFileBecomesLandscapeAfterAQuarterTurn() {
        assertTrue(PhotoFit.isPortrait(1080, 1920))
        assertFalse(PhotoFit.isPortrait(1920, 1080))
        assertEquals(PixelSize(1920, 1080), PhotoFit.orientedSize(1080, 1920, 90))
        assertEquals(PixelSize(1080, 1920), PhotoFit.orientedSize(1080, 1920, 180))
    }

    @Test
    fun centerFillCropsToSixteenByNine() {
        assertEquals(CropWindow(0, 0, 1920, 1080), PhotoFit.centerFill(1920, 1080))
        val portrait = PhotoFit.centerFill(1080, 1920)
        assertEquals(0, portrait.x)
        assertEquals(1080, portrait.width)
        assertEquals(607, portrait.height)
        assertTrue(portrait.y > 0)
        assertTrue(portrait.y + portrait.height <= 1920)
        val wide = PhotoFit.centerFill(2000, 1000)
        assertEquals(0, wide.y)
        assertEquals(1000, wide.height)
        assertEquals(1777, wide.width)
        assertTrue(wide.x > 0)
        val turned = PhotoFit.orientedSize(1080, 1920, PhotoFit.totalClockwise(PhotoFit.fromExif(1), 1))
        assertEquals(CropWindow(0, 0, 1920, 1080), PhotoFit.centerFill(turned.width, turned.height))
    }
}

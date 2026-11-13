package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

class WeatherCopyTest {
    @Test
    fun parsesOpenMeteoAndKeepsAStaleReadingOffline() {
        val parsed = parseOpenMeteo("""{"current":{"temperature_2m":24.6,"weather_code":1}}""")
        requireNotNull(parsed)
        assertEquals(25, parsed.celsius)
        assertEquals(WeatherKind.CLEAR, parsed.kind)
        assertEquals(WeatherKind.STORM, weatherKind(95))
        assertEquals(WeatherKind.RAIN, weatherKind(61))
        assertEquals(WeatherKind.SNOW, weatherKind(71))
        assertEquals(WeatherKind.FOG, weatherKind(45))
        assertNull(parseOpenMeteo("{"))
        assertNull(parseOpenMeteo("""{"current":{}}"""))

        val cache = MemoryTextCache()
        var now = 0L
        val http = object : HttpGet {
            var fail = false
            override fun get(url: String): String {
                assertTrue(url.startsWith("https://api.open-meteo.com/"))
                assertTrue(url.contains("latitude=-23.5505"))
                if (fail) throw IOException("offline")
                return """{"current":{"temperature_2m":19.2,"weather_code":3}}"""
            }
        }
        val repo = WeatherRepository(http, cache, { now }, ttlMillis = 100L)
        val first = repo.current(-23.5505, -46.6333) as Load.Ready
        assertEquals(19, first.value.celsius)
        assertEquals(WeatherKind.CLOUD, first.value.kind)
        now = 500L
        http.fail = true
        val stale = repo.current(-23.5505, -46.6333) as Load.Ready
        assertTrue(stale.stale)
        assertEquals(19, stale.value.celsius)
    }

    @Test
    fun datesClockAndWeekUseBrazilianWords() {
        assertEquals("sexta, 13 de novembro", ambientDate(LocalDate.parse("2026-11-13")))
        assertEquals("sexta, 1 de janeiro", ambientDate(LocalDate.parse("2027-01-01")))
        assertEquals("9:05", clockLabel(LocalTime.of(9, 5)))
        assertEquals("22:47", clockLabel(LocalTime.of(22, 47)))
        val week = broadcastWeek(LocalDate.parse("2026-11-13"))
        assertEquals(LocalDate.parse("2026-11-09"), week.first)
        assertEquals(LocalDate.parse("2026-11-15"), week.second)
        assertEquals("8,1", ratingLabel(8.14))
        assertNull(ratingLabel(0.0))
        assertEquals("24° São Paulo", temperatureLabel(24, "São Paulo"))
    }

    @Test
    fun pageTintStaysLight() {
        val tint = pageTint(0xFF000000.toInt())
        val red = (tint shr 16) and 0xFF
        val green = (tint shr 8) and 0xFF
        val blue = tint and 0xFF
        assertTrue(red > 180 && green > 180 && blue > 170)
        assertEquals(OFF_WHITE, mixColor(OFF_WHITE, INK, 0f))
    }
}

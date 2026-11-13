package dev.enzotraini.tvlauncher.model

import org.json.JSONObject
import java.io.IOException
import java.util.Locale

enum class WeatherKind {
    CLEAR,
    CLOUD,
    RAIN,
    STORM,
    SNOW,
    FOG,
    UNKNOWN,
}

data class WeatherNow(
    val celsius: Int,
    val code: Int,
    val kind: WeatherKind,
)

fun weatherKind(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.CLEAR
    2, 3 -> WeatherKind.CLOUD
    45, 48 -> WeatherKind.FOG
    95, 96, 99 -> WeatherKind.STORM
    71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
    in 51..67, in 80..82 -> WeatherKind.RAIN
    else -> WeatherKind.UNKNOWN
}

fun parseOpenMeteo(json: String): WeatherNow? {
    val root = try {
        JSONObject(json)
    } catch (_: Exception) {
        return null
    }
    val current = root.optJSONObject("current") ?: return null
    if (!current.has("temperature_2m") || current.isNull("temperature_2m")) return null
    val temperature = current.optDouble("temperature_2m")
    if (temperature.isNaN()) return null
    val code = current.optInt("weather_code", -1)
    return WeatherNow(temperature.roundToIntSafe(), code, weatherKind(code))
}

class WeatherRepository(
    private val http: HttpGet,
    private val cache: TextCache,
    private val now: () -> Long,
    private val ttlMillis: Long = 30L * 60L * 1000L,
) {
    fun current(latitude: Double, longitude: Double): Load<WeatherNow> {
        val key = cacheKey(latitude, longitude)
        val cached = cache.read(key)
        val age = cached?.let { now() - it.savedAtMillis }
        if (cached != null && age != null && age in 0..ttlMillis) {
            val parsed = parseOpenMeteo(cached.body) ?: return Load.Unavailable
            return Load.Ready(parsed, fromCache = true, stale = false)
        }
        return try {
            val body = http.get(url(latitude, longitude))
            if (body.isBlank()) throw IOException("resposta vazia")
            val parsed = parseOpenMeteo(body) ?: return staleOrUnavailable(cached)
            cache.write(key, body, now())
            Load.Ready(parsed, fromCache = false, stale = false)
        } catch (_: Exception) {
            staleOrUnavailable(cached)
        }
    }

    private fun staleOrUnavailable(cached: CacheEntry?): Load<WeatherNow> {
        val parsed = cached?.let { parseOpenMeteo(it.body) } ?: return Load.Unavailable
        return Load.Ready(parsed, fromCache = true, stale = true)
    }

    companion object {
        fun cacheKey(latitude: Double, longitude: Double): String {
            return "weather-${format(latitude)}-${format(longitude)}"
        }

        fun url(latitude: Double, longitude: Double): String {
            val lat = format(latitude)
            val lon = format(longitude)
            return "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code&timezone=America%2FSao_Paulo"
        }

        private fun format(value: Double): String = String.format(Locale.US, "%.4f", value)
    }
}

private fun Double.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

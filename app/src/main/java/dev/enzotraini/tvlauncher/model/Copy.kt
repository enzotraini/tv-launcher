package dev.enzotraini.tvlauncher.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

fun broadcastWeek(today: LocalDate): Pair<LocalDate, LocalDate> {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return start to end
}

fun ambientDate(date: LocalDate): String {
    val weekday = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "segunda"
        DayOfWeek.TUESDAY -> "terça"
        DayOfWeek.WEDNESDAY -> "quarta"
        DayOfWeek.THURSDAY -> "quinta"
        DayOfWeek.FRIDAY -> "sexta"
        DayOfWeek.SATURDAY -> "sábado"
        DayOfWeek.SUNDAY -> "domingo"
    }
    val month = when (date.monthValue) {
        1 -> "janeiro"
        2 -> "fevereiro"
        3 -> "março"
        4 -> "abril"
        5 -> "maio"
        6 -> "junho"
        7 -> "julho"
        8 -> "agosto"
        9 -> "setembro"
        10 -> "outubro"
        11 -> "novembro"
        else -> "dezembro"
    }
    return "$weekday, ${date.dayOfMonth} de $month"
}

fun clockLabel(time: LocalTime): String {
    val minute = time.minute.toString().padStart(2, '0')
    return "${time.hour}:$minute"
}

fun ratingLabel(rating: Double?): String? {
    if (rating == null || rating <= 0.0) return null
    return String.format(Locale("pt", "BR"), "%.1f", rating)
}

fun metaLine(item: MediaItem): String {
    return listOfNotNull(ratingLabel(item.rating), item.year?.toString()).joinToString("  ·  ")
}

fun mixColor(base: Int, accent: Int, accentWeight: Float): Int {
    val weight = accentWeight.coerceIn(0f, 1f)
    fun channel(shift: Int): Int {
        val from = (base shr shift) and 0xFF
        val to = (accent shr shift) and 0xFF
        return (from * (1f - weight) + to * weight).roundToInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

/** Mistura curta com o off-white para o fundo continuar claro. */
fun pageTint(dominant: Int): Int = mixColor(OFF_WHITE, dominant, 0.16f)

fun temperatureLabel(celsius: Int, city: String): String {
    val place = city.trim()
    return if (place.isEmpty()) "$celsius°" else "$celsius° $place"
}

const val OFF_WHITE = 0xFFF4F1EB.toInt()
const val INK = 0xFF1C1917.toInt()

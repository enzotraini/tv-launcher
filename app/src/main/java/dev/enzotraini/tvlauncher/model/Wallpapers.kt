package dev.enzotraini.tvlauncher.model

import java.io.File
import kotlin.random.Random

data class WallpaperPref(
    val hidden: Set<String> = emptySet(),
    val quarters: Map<String, Int> = emptyMap(),
    val shuffle: Boolean = false,
    val intervalSeconds: Int = 30,
    val idleMinutes: Int = 5,
) {
    fun normalized(): WallpaperPref = copy(
        hidden = hidden.filter { it.isNotBlank() }.toSet(),
        quarters = quarters
            .filterKeys { it.isNotBlank() }
            .mapValues { PhotoFit.normalizedQuarters(it.value) },
        intervalSeconds = intervalSeconds.coerceIn(10, 3_600),
        idleMinutes = idleMinutes.coerceIn(1, 180),
    )
}

fun parseWallpaperPref(text: String): WallpaperPref {
    val hidden = LinkedHashSet<String>()
    val quarters = LinkedHashMap<String, Int>()
    var shuffle = false
    var interval = 30
    var idle = 5
    for (raw in text.lineSequence()) {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) continue
        val parts = line.split("=", limit = 2)
        if (parts.size != 2) continue
        val key = parts[0].trim()
        val value = parts[1].trim()
        when {
            key == "hidden" -> hidden += value.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            key == "shuffle" -> shuffle = value == "1" || value.equals("true", ignoreCase = true)
            key == "interval" -> value.toIntOrNull()?.let { interval = it }
            key == "idle" -> value.toIntOrNull()?.let { idle = it }
            key.startsWith("quarter.") -> {
                val name = key.removePrefix("quarter.").trim()
                val turns = value.toIntOrNull()
                if (name.isNotEmpty() && turns != null) quarters[name] = turns
            }
        }
    }
    return WallpaperPref(hidden, quarters, shuffle, interval, idle).normalized()
}

fun formatWallpaperPref(pref: WallpaperPref): String {
    val ready = pref.normalized()
    return buildString {
        append("hidden=")
        append(ready.hidden.sorted().joinToString("|"))
        append('\n')
        ready.quarters.toSortedMap().forEach { (name, turns) ->
            append("quarter.")
            append(name)
            append("=")
            append(turns)
            append('\n')
        }
        append("shuffle=")
        append(if (ready.shuffle) "1" else "0")
        append('\n')
        append("interval=")
        append(ready.intervalSeconds)
        append('\n')
        append("idle=")
        append(ready.idleMinutes)
        append('\n')
    }
}

fun photoFiles(dir: File): List<File> {
    if (!dir.isDirectory) return emptyList()
    return dir.listFiles()
        ?.filter { file ->
            file.isFile && file.extension.lowercase() in PHOTO_EXTENSIONS && !file.name.contains("..")
        }
        ?.sortedBy { it.name.lowercase() }
        .orEmpty()
}

fun collectPhotos(dirs: List<File>): List<File> {
    val seen = HashSet<String>()
    val found = ArrayList<File>()
    for (dir in dirs) {
        for (file in photoFiles(dir)) {
            if (seen.add(file.name.lowercase())) found += file
        }
    }
    return found.sortedBy { it.name.lowercase() }
}

fun playlist(names: List<String>, pref: WallpaperPref, seed: Long): List<String> {
    val visible = names.filter { it.isNotBlank() && it !in pref.hidden }
    if (!pref.shuffle || visible.size < 2) return visible
    return visible.shuffled(Random(seed))
}

fun nextInterval(current: Int): Int = when (current) {
    15 -> 30
    30 -> 60
    60 -> 120
    else -> 15
}

fun nextIdle(current: Int): Int = when (current) {
    1 -> 5
    5 -> 10
    10 -> 30
    else -> 1
}

private val PHOTO_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

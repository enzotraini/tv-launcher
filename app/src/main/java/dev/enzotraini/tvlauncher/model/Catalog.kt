package dev.enzotraini.tvlauncher.model

import java.util.Locale

private val SKIP_WORDS = setOf(
    "de", "da", "do", "das", "dos", "e", "o", "a", "os", "as",
    "the", "of", "um", "uma",
)

/** Iniciais para um pôster real que chegou sem imagem. Não inventa título. */
fun monogram(name: String): String {
    val words = name.split(Regex("[^\\p{L}\\p{N}]+"))
        .map { it.trim() }
        .filter { token ->
            token.isNotEmpty() && token.lowercase(Locale("pt", "BR")) !in SKIP_WORDS
        }
    return when {
        words.size >= 2 ->
            "${words[0].first()}${words[1].first()}".uppercase(Locale("pt", "BR"))
        words.size == 1 -> words[0].take(2).uppercase(Locale("pt", "BR"))
        else -> "•"
    }
}

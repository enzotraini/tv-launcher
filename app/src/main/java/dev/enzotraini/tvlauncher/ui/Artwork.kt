package dev.enzotraini.tvlauncher.ui

import dev.enzotraini.tvlauncher.R

/** Fotos originais empacotadas no app. Não há pôster de estúdio. */
object Artwork {
    fun resId(key: String): Int {
        return when (key) {
            "noite_estacao" -> R.drawable.poster_noite_estacao
            "sombras_chuva" -> R.drawable.poster_sombras_chuva
            "chamado_floresta" -> R.drawable.poster_chamado_floresta
            "casa_sem_numero" -> R.drawable.poster_casa_sem_numero
            "mare_vidro" -> R.drawable.poster_mare_vidro
            "arquivo_mares" -> R.drawable.poster_arquivo_mares
            "luz_inverno" -> R.drawable.poster_luz_inverno
            "estrada_sal" -> R.drawable.poster_estrada_sal
            "porto_quieto" -> R.drawable.poster_porto_quieto
            else -> 0
        }
    }
}

package dev.enzotraini.tvlauncher.model

/**
 * Resultado de uma leitura que pode vir da rede ou do cache.
 * Sem chave e sem cache não existe um terceiro estado com título inventado.
 */
sealed class Load<out T> {
    data class Ready<T>(
        val value: T,
        val fromCache: Boolean,
        val stale: Boolean,
    ) : Load<T>()

    data object MissingKey : Load<Nothing>()

    data object Unavailable : Load<Nothing>()
}

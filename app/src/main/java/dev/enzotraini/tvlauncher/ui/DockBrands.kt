package dev.enzotraini.tvlauncher.ui

import dev.enzotraini.tvlauncher.R

internal data class DockBrand(
    val backgroundRes: Int,
    val word: String,
    val wordColorRes: Int,
    val widthDp: Int,
    val textSizeSp: Float,
    val letterSpacing: Float = 0f,
    val fontRes: Int = R.font.inter_bold,
    val markRes: Int = 0,
)

/**
 * Tiles do dock no visual do mockup. O pacote instalado continua sendo o que abre.
 * Pacote fora desta lista ganha um tile neutro com o nome configurado.
 */
internal object DockBrands {
    private val all = mapOf(
        "com.netflix.ninja" to DockBrand(
            backgroundRes = R.drawable.dock_netflix,
            word = "NETFLIX",
            wordColorRes = R.color.netflix_red,
            widthDp = 124,
            textSizeSp = 13f,
            letterSpacing = 0.06f,
        ),
        "com.google.android.youtube.tv" to DockBrand(
            backgroundRes = R.drawable.dock_youtube,
            word = "YouTube",
            wordColorRes = R.color.ink,
            widthDp = 142,
            textSizeSp = 14f,
            markRes = R.drawable.ic_play,
        ),
        "com.globo.globotv" to DockBrand(
            backgroundRes = R.drawable.dock_globoplay,
            word = "globoplay",
            wordColorRes = R.color.white,
            widthDp = 132,
            textSizeSp = 14f,
            fontRes = R.font.inter_semibold,
        ),
        "com.amazon.amazonvideo.livingroom" to DockBrand(
            backgroundRes = R.drawable.dock_prime,
            word = "prime video",
            wordColorRes = R.color.white,
            widthDp = 136,
            textSizeSp = 13f,
            fontRes = R.font.inter_semibold,
        ),
        "com.disney.disneyplus" to DockBrand(
            backgroundRes = R.drawable.dock_disney,
            word = "Disney+",
            wordColorRes = R.color.white,
            widthDp = 116,
            textSizeSp = 15f,
            fontRes = R.font.inter_semibold,
        ),
        "com.stremio.one" to DockBrand(
            backgroundRes = R.drawable.dock_stremio,
            word = "Stremio",
            wordColorRes = R.color.white,
            widthDp = 118,
            textSizeSp = 15f,
            fontRes = R.font.inter_semibold,
        ),
    )

    fun forPackage(packageName: String, label: String): DockBrand {
        return all[packageName] ?: DockBrand(
            backgroundRes = R.drawable.dock_neutral,
            word = label,
            wordColorRes = R.color.ink,
            widthDp = 120,
            textSizeSp = 13f,
            fontRes = R.font.inter_semibold,
        )
    }
}

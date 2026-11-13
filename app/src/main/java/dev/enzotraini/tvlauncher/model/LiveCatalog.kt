package dev.enzotraini.tvlauncher.model

data class BroadcastChannel(
    val id: String,
    val name: String,
    val appLabel: String,
    val packages: List<String>,
)

data class FreeService(
    val id: String,
    val name: String,
    val packages: List<String>,
)

sealed class ChannelTarget {
    data class App(val packageName: String) : ChannelTarget()
    data class Tuner(val packageName: String) : ChannelTarget()
    data object Unavailable : ChannelTarget()
}

/**
 * Canais abertos e serviços oficiais gratuitos.
 * Sem grade de programação: não há fonte gratuita confiável para o que está no ar.
 * Cultura não tem pacote oficial confirmado aqui, então só o sintonizador.
 */
object LiveCatalog {
    val channels: List<BroadcastChannel> = listOf(
        BroadcastChannel("globo", "Globo", "Globoplay", listOf("com.globo.globotv")),
        BroadcastChannel("band", "Band", "Bandplay", listOf("br.com.band.guiatv")),
        BroadcastChannel("sbt", "SBT", "+SBT", listOf("br.com.sbt.mais")),
        BroadcastChannel(
            "record",
            "Record",
            "PlayPlus",
            listOf("com.mobilus.recordplay.tv", "com.mobilus.recordplay"),
        ),
        BroadcastChannel("cultura", "Cultura", "", emptyList()),
    )

    val free: List<FreeService> = listOf(
        FreeService("pluto", "Pluto TV", listOf("tv.pluto.android")),
        FreeService("plex", "Plex", listOf("com.plexapp.android")),
        FreeService("libreflix", "Libreflix", listOf("org.libreflix.app")),
        FreeService("spcine", "Spcine Play", listOf("com.eitv.spcineplay")),
        FreeService(
            "youtube",
            "YouTube",
            listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
        ),
    )

    fun installedPackages(): List<String> {
        return (channels.flatMap { it.packages } + free.flatMap { it.packages }).distinct()
    }
}

fun resolveChannel(
    channel: BroadcastChannel,
    installed: Set<String>,
    tunerPackage: String,
): ChannelTarget {
    val app = channel.packages.firstOrNull { it in installed }
    if (app != null) return ChannelTarget.App(app)
    val tuner = tunerPackage.trim()
    if (tuner.isNotEmpty()) return ChannelTarget.Tuner(tuner)
    return ChannelTarget.Unavailable
}

fun resolveService(service: FreeService, installed: Set<String>): String? {
    return service.packages.firstOrNull { it in installed }
}

fun channelHint(target: ChannelTarget, appLabel: String): String = when (target) {
    is ChannelTarget.App -> appLabel.ifBlank { "App" }
    is ChannelTarget.Tuner -> "Sintonizador"
    ChannelTarget.Unavailable -> "Indisponível"
}

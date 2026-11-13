package dev.enzotraini.tvlauncher.model

data class ServiceMatch(
    val label: String,
    val packages: List<String>,
    val searchUrl: String?,
)

sealed class PlayAction {
    data class View(val uri: String, val packageName: String) : PlayAction()
    data class OpenApp(val packageName: String) : PlayAction()
    data object NotInstalled : PlayAction()
}

private data class KnownService(
    val ids: Set<Int>,
    val names: Set<String>,
    val label: String,
    val packages: List<String>,
    val searchUrl: String?,
)

fun matchProvider(providerId: Int, providerName: String): ServiceMatch? {
    val normalized = normalizeProvider(providerName)
    val known = KNOWN.firstOrNull { providerId in it.ids || normalized in it.names || it.names.any { name -> normalized.contains(name) } }
        ?: return null
    return ServiceMatch(known.label, known.packages, known.searchUrl)
}

fun watchButton(offer: WatchOffer): String {
    val label = matchProvider(offer.providerId, offer.providerName)?.label ?: offer.providerName
    return when (offer.kind) {
        OfferKind.RENT -> "Alugar na $label"
        OfferKind.BUY -> "Comprar na $label"
        else -> "Assistir na $label"
    }
}

fun planPlay(
    offer: WatchOffer,
    title: String,
    installed: Set<String>,
    uriResolves: (uri: String, packageName: String) -> Boolean,
): PlayAction {
    val match = matchProvider(offer.providerId, offer.providerName) ?: return PlayAction.NotInstalled
    val packageName = match.packages.firstOrNull { it in installed } ?: return PlayAction.NotInstalled
    val search = match.searchUrl?.replace("{q}", formEncode(title))
    if (!search.isNullOrBlank() && uriResolves(search, packageName)) {
        return PlayAction.View(search, packageName)
    }
    return PlayAction.OpenApp(packageName)
}

private fun normalizeProvider(name: String): String {
    return name.lowercase()
        .replace("+", " plus ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex(" +"), " ")
}

private val KNOWN = listOf(
    KnownService(setOf(8), setOf("netflix"), "Netflix", listOf("com.netflix.ninja"), null),
    KnownService(
        setOf(9, 10, 119),
        setOf("amazon prime video", "prime video", "amazon video"),
        "Prime Video",
        listOf("com.amazon.amazonvideo.livingroom"),
        "https://app.primevideo.com/search?phrase={q}",
    ),
    KnownService(
        setOf(337),
        setOf("disney plus"),
        "Disney+",
        listOf("com.disney.disneyplus"),
        null,
    ),
    KnownService(
        setOf(301, 307),
        setOf("globoplay"),
        "Globoplay",
        listOf("com.globo.globotv"),
        "https://globoplay.globo.com/busca/?q={q}",
    ),
    KnownService(
        setOf(384, 1899),
        setOf("max", "hbo max"),
        "Max",
        listOf("com.wbd.stream"),
        null,
    ),
    KnownService(
        setOf(531),
        setOf("paramount plus"),
        "Paramount+",
        listOf("com.cbs.ott"),
        null,
    ),
    KnownService(
        setOf(283),
        setOf("crunchyroll"),
        "Crunchyroll",
        listOf("com.crunchyroll.crunchyroid"),
        null,
    ),
    KnownService(
        setOf(2, 350),
        setOf("apple tv", "apple tv plus"),
        "Apple TV",
        listOf("com.apple.atve.androidtv.appletv"),
        null,
    ),
    KnownService(
        setOf(3),
        setOf("google play movies", "google play filmes"),
        "Google Play Filmes",
        listOf("com.google.android.videos"),
        "https://play.google.com/store/search?q={q}&c=movies",
    ),
    KnownService(
        setOf(192),
        setOf("youtube"),
        "YouTube",
        listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
        "https://www.youtube.com/results?search_query={q}",
    ),
    KnownService(setOf(300), setOf("pluto tv"), "Pluto TV", listOf("tv.pluto.android"), null),
    KnownService(setOf(538), setOf("plex"), "Plex", listOf("com.plexapp.android"), null),
)

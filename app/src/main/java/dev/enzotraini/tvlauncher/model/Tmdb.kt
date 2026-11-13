package dev.enzotraini.tvlauncher.model

import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.time.LocalDate
import java.util.Locale

enum class MediaKind {
    MOVIE,
    SERIES,
}

enum class OfferKind {
    FLATRATE,
    FREE,
    ADS,
    RENT,
    BUY,
}

data class MediaItem(
    val id: Int,
    val kind: MediaKind,
    val title: String,
    val year: Int?,
    val rating: Double?,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val popularity: Double,
)

data class WatchOffer(
    val providerId: Int,
    val providerName: String,
    val kind: OfferKind,
    val logoPath: String?,
)

data class MediaDetails(
    val item: MediaItem,
    val offers: List<WatchOffer>,
)

fun interface HttpGet {
    fun get(url: String): String
}

fun formEncode(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}

fun tmdbImage(path: String?, size: String): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val normalized = if (path.startsWith("/")) path else "/$path"
    return "https://image.tmdb.org/t/p/$size$normalized"
}

class TmdbUrls(private val apiKey: String) {
    fun trending(): String = get("/trending/all/week", mapOf("region" to "BR"))

    fun movies(): String = get("/movie/popular", mapOf("region" to "BR", "page" to "1"))

    fun series(): String = get("/tv/popular", mapOf("page" to "1"))

    fun discoverMovies(start: LocalDate, end: LocalDate): String = get(
        "/discover/movie",
        mapOf(
            "region" to "BR",
            "sort_by" to "popularity.desc",
            "include_adult" to "false",
            "include_video" to "false",
            "watch_region" to "BR",
            "primary_release_date.gte" to start.toString(),
            "primary_release_date.lte" to end.toString(),
            "with_release_type" to "2|3",
            "page" to "1",
        ),
    )

    fun discoverSeries(start: LocalDate, end: LocalDate): String = get(
        "/discover/tv",
        mapOf(
            "sort_by" to "popularity.desc",
            "include_adult" to "false",
            "watch_region" to "BR",
            "first_air_date.gte" to start.toString(),
            "first_air_date.lte" to end.toString(),
            "page" to "1",
        ),
    )

    fun movie(id: Int): String = get("/movie/$id", mapOf("append_to_response" to "watch/providers"))

    fun series(id: Int): String = get("/tv/$id", mapOf("append_to_response" to "watch/providers"))

    private fun get(path: String, query: Map<String, String>): String {
        val all = LinkedHashMap<String, String>()
        all["api_key"] = apiKey
        all["language"] = "pt-BR"
        all.putAll(query)
        val encoded = all.entries.joinToString("&") { (key, value) ->
            "${formEncode(key)}=${formEncode(value)}"
        }
        return "https://api.themoviedb.org/3$path?$encoded"
    }
}

fun parseTmdbList(json: String, forcedKind: MediaKind? = null): List<MediaItem> {
    val root = parseObject(json) ?: return emptyList()
    val results = root.optJSONArray("results") ?: return emptyList()
    val items = ArrayList<MediaItem>(results.length())
    for (index in 0 until results.length()) {
        val obj = results.optJSONObject(index) ?: continue
        parseItem(obj, forcedKind)?.let { items += it }
    }
    return items
}

fun parseTmdbDetails(json: String, fallbackKind: MediaKind): MediaDetails? {
    val root = parseObject(json) ?: return null
    val item = parseItem(root, fallbackKind) ?: return null
    return MediaDetails(item, parseWatchOffers(root))
}

fun parseWatchOffers(root: JSONObject): List<WatchOffer> {
    val providers = root.optJSONObject("watch/providers") ?: return emptyList()
    val brazil = providers.optJSONObject("results")?.optJSONObject("BR") ?: return emptyList()
    val groups = listOf(
        "flatrate" to OfferKind.FLATRATE,
        "free" to OfferKind.FREE,
        "ads" to OfferKind.ADS,
        "rent" to OfferKind.RENT,
        "buy" to OfferKind.BUY,
    )
    val seen = HashSet<Int>()
    val offers = ArrayList<WatchOffer>()
    for ((key, kind) in groups) {
        val array = brazil.optJSONArray(key) ?: continue
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val id = obj.optInt("provider_id", -1)
            val name = obj.optString("provider_name").trim()
            if (id <= 0 || name.isEmpty() || !seen.add(id)) continue
            offers += WatchOffer(
                providerId = id,
                providerName = name,
                kind = kind,
                logoPath = blankToNull(obj.optString("logo_path")),
            )
        }
    }
    return offers
}

class TmdbRepository(
    private val apiKey: String,
    private val http: HttpGet,
    private val cache: TextCache,
    private val now: () -> Long,
    private val listTtlMillis: Long = SIX_HOURS,
    private val detailTtlMillis: Long = DAY,
) {
    private val urls = TmdbUrls(apiKey)

    fun trending(): Load<List<MediaItem>> = loadList(KEY_TRENDING, urls.trending(), null)

    fun movies(): Load<List<MediaItem>> = loadList(KEY_MOVIES, urls.movies(), MediaKind.MOVIE)

    fun series(): Load<List<MediaItem>> = loadList(KEY_SERIES, urls.series(), MediaKind.SERIES)

    fun weekly(start: LocalDate, end: LocalDate): Load<List<MediaItem>> {
        if (apiKey.isBlank()) return Load.MissingKey
        val movies = loadList(
            "weekly-movie-$start-$end",
            urls.discoverMovies(start, end),
            MediaKind.MOVIE,
        )
        val shows = loadList(
            "weekly-tv-$start-$end",
            urls.discoverSeries(start, end),
            MediaKind.SERIES,
        )
        return merge(movies, shows)
    }

    fun details(kind: MediaKind, id: Int): Load<MediaDetails> {
        val key = "detail-${kind.name.lowercase(Locale.US)}-$id"
        val url = if (kind == MediaKind.MOVIE) urls.movie(id) else urls.series(id)
        return when (val text = loadText(key, url, detailTtlMillis)) {
            is Load.Ready -> {
                val parsed = parseTmdbDetails(text.value, kind)
                if (parsed == null) Load.Unavailable else Load.Ready(parsed, text.fromCache, text.stale)
            }
            Load.MissingKey -> Load.MissingKey
            Load.Unavailable -> Load.Unavailable
        }
    }

    private fun loadList(cacheKey: String, url: String, kind: MediaKind?): Load<List<MediaItem>> {
        return when (val text = loadText(cacheKey, url, listTtlMillis)) {
            is Load.Ready -> Load.Ready(parseTmdbList(text.value, kind).take(SHELF_LIMIT), text.fromCache, text.stale)
            Load.MissingKey -> Load.MissingKey
            Load.Unavailable -> Load.Unavailable
        }
    }

    private fun loadText(cacheKey: String, url: String, ttl: Long): Load<String> {
        if (apiKey.isBlank()) return Load.MissingKey
        val cached = cache.read(cacheKey)
        val age = cached?.let { now() - it.savedAtMillis }
        if (cached != null && age != null && age in 0..ttl) {
            return Load.Ready(cached.body, fromCache = true, stale = false)
        }
        return try {
            val body = http.get(url)
            if (body.isBlank()) throw IOException("resposta vazia")
            cache.write(cacheKey, body, now())
            Load.Ready(body, fromCache = false, stale = false)
        } catch (_: Exception) {
            if (cached != null) {
                Load.Ready(cached.body, fromCache = true, stale = true)
            } else {
                Load.Unavailable
            }
        }
    }

    companion object {
        const val KEY_TRENDING = "trending-all-week"
        const val KEY_MOVIES = "movies-popular-br"
        const val KEY_SERIES = "series-popular-br"
        const val SHELF_LIMIT = 20
        const val SIX_HOURS = 6L * 60L * 60L * 1000L
        const val DAY = 24L * 60L * 60L * 1000L
    }
}

private fun merge(first: Load<List<MediaItem>>, second: Load<List<MediaItem>>): Load<List<MediaItem>> {
    val ready = listOf(first, second).filterIsInstance<Load.Ready<List<MediaItem>>>()
    if (ready.isEmpty()) {
        return if (first is Load.MissingKey || second is Load.MissingKey) Load.MissingKey else Load.Unavailable
    }
    val items = ready.flatMap { it.value }
        .sortedByDescending { it.popularity }
        .distinctBy { it.kind to it.id }
        .take(TmdbRepository.SHELF_LIMIT)
    return Load.Ready(
        value = items,
        fromCache = ready.all { it.fromCache },
        stale = ready.any { it.stale },
    )
}

private fun parseObject(json: String): JSONObject? {
    return try {
        JSONObject(json)
    } catch (_: Exception) {
        null
    }
}

private fun parseItem(obj: JSONObject, forcedKind: MediaKind?): MediaItem? {
    val mediaType = obj.optString("media_type")
    if (mediaType == "person") return null
    val kind = forcedKind ?: when (mediaType) {
        "movie" -> MediaKind.MOVIE
        "tv" -> MediaKind.SERIES
        else -> when {
            obj.has("title") || obj.has("release_date") -> MediaKind.MOVIE
            obj.has("name") || obj.has("first_air_date") -> MediaKind.SERIES
            else -> return null
        }
    }
    val title = when (kind) {
        MediaKind.MOVIE -> obj.optString("title").ifBlank { obj.optString("name") }
        MediaKind.SERIES -> obj.optString("name").ifBlank { obj.optString("title") }
    }.trim()
    val id = obj.optInt("id", -1)
    if (title.isEmpty() || id <= 0) return null
    val date = when (kind) {
        MediaKind.MOVIE -> obj.optString("release_date")
        MediaKind.SERIES -> obj.optString("first_air_date")
    }
    val vote = if (obj.has("vote_average") && !obj.isNull("vote_average")) obj.optDouble("vote_average") else 0.0
    return MediaItem(
        id = id,
        kind = kind,
        title = title,
        year = date.take(4).toIntOrNull(),
        rating = if (vote > 0.05) vote else null,
        overview = obj.optString("overview").trim(),
        posterPath = blankToNull(obj.optString("poster_path")),
        backdropPath = blankToNull(obj.optString("backdrop_path")),
        popularity = if (obj.has("popularity") && !obj.isNull("popularity")) obj.optDouble("popularity") else 0.0,
    )
}

private fun blankToNull(value: String): String? = value.trim().takeIf { it.isNotEmpty() && it != "null" }

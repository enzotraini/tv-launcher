package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.time.LocalDate

class TmdbRepositoryTest {
    @Test
    fun missingKeyDoesNotCallTheNetwork() {
        val http = FakeHttp(mapOf("trending" to LIST))
        val cache = MemoryTextCache()
        val repo = repo(apiKey = "  ", http = http, cache = cache)
        assertEquals(Load.MissingKey, repo.trending())
        assertEquals(Load.MissingKey, repo.movies())
        assertEquals(Load.MissingKey, repo.series())
        assertEquals(Load.MissingKey, repo.weekly(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-15")))
        assertEquals(Load.MissingKey, repo.details(MediaKind.MOVIE, 101))
        assertTrue(http.calls.isEmpty())
        assertTrue(cache.entries.isEmpty())
    }

    @Test
    fun freshCacheSkipsTheNetworkAndDiskRoundTripKeepsTheBody() {
        val cache = MemoryTextCache()
        cache.write(TmdbRepository.KEY_TRENDING, LIST, 0L)
        val http = FakeHttp(emptyMap())
        val repo = repo(http = http, cache = cache, now = 1_000L, ttl = 5_000L)
        val loaded = repo.trending() as Load.Ready
        assertEquals(listOf("Ainda Estou Aqui"), loaded.value.map { it.title })
        assertTrue(loaded.fromCache)
        assertTrue(!loaded.stale)
        assertTrue(http.calls.isEmpty())

        val dir = File(System.getProperty("java.io.tmpdir"), "tmdb-cache-${System.nanoTime()}")
        val disk = DiskTextCache(dir)
        disk.write("movies-popular-br", LIST, 40L)
        assertEquals(LIST, disk.read("movies-popular-br")?.body)
        assertEquals(40L, disk.read("movies-popular-br")?.savedAtMillis)
        assertTrue(dir.walkTopDown().none { it.name.contains("sekret") })
    }

    @Test
    fun staleCacheRefreshesAndOfflineKeepsTheOldBody() {
        var now = 0L
        val cache = MemoryTextCache()
        val http = FakeHttp(mapOf("/movie/popular" to LIST))
        val repo = repo(http = http, cache = cache, now = { now }, ttl = 100L)
        val first = repo.movies() as Load.Ready
        assertEquals(1, http.calls.size)
        assertTrue(!first.fromCache)
        now = 50L
        repo.movies()
        assertEquals(1, http.calls.size)
        now = 250L
        http.fail = true
        val stale = repo.movies() as Load.Ready
        assertTrue(stale.stale)
        assertEquals("Ainda Estou Aqui", stale.value.single().title)
        assertTrue(cache.entries.keys.none { it.contains("sekret-key") })
    }

    @Test
    fun networkFailureWithoutCacheIsEmptyNotInvented() {
        val http = FakeHttp(emptyMap(), fail = true)
        val repo = repo(http = http, cache = MemoryTextCache())
        assertEquals(Load.Unavailable, repo.series())
        assertEquals(Load.Unavailable, repo.details(MediaKind.SERIES, 9))
    }

    @Test
    fun weeklyMergesBothFeedsAndSurvivesOneFailure() {
        val http = FakeHttp(
            mapOf(
                "/discover/movie" to LIST,
                "/discover/tv" to """{"results":[{"id":9,"name":"Senna","first_air_date":"2024-11-29","vote_average":6.5,"popularity":80}]}""",
            ),
        )
        val repo = repo(http = http, cache = MemoryTextCache())
        val week = repo.weekly(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-15")) as Load.Ready
        assertEquals(listOf("Senna", "Ainda Estou Aqui"), week.value.map { it.title })
        http.failPaths = setOf("/discover/tv")
        val onlyMovies = TmdbRepository(
            apiKey = "sekret-key",
            http = http,
            cache = MemoryTextCache(),
            now = { 0L },
            listTtlMillis = 10L,
            detailTtlMillis = 10L,
        ).weekly(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-15")) as Load.Ready
        assertEquals(listOf("Ainda Estou Aqui"), onlyMovies.value.map { it.title })
        assertTrue(onlyMovies.stale || !onlyMovies.fromCache)
    }

    private fun repo(
        apiKey: String = "sekret-key",
        http: HttpGet,
        cache: TextCache,
        now: Long = 0L,
        ttl: Long = 10L,
    ) = repo(apiKey, http, cache, { now }, ttl)

    private fun repo(
        apiKey: String = "sekret-key",
        http: HttpGet,
        cache: TextCache,
        now: () -> Long,
        ttl: Long,
    ) = TmdbRepository(apiKey, http, cache, now, ttl, ttl)

    private class FakeHttp(
        private val bodies: Map<String, String>,
        var fail: Boolean = false,
        var failPaths: Set<String> = emptySet(),
    ) : HttpGet {
        val calls = mutableListOf<String>()

        override fun get(url: String): String {
            calls += url
            if (fail || failPaths.any { url.contains(it) }) throw IOException("offline")
            val body = bodies.entries.firstOrNull { url.contains(it.key) }?.value
                ?: throw IOException("sem corpo")
            return body
        }
    }

    private companion object {
        const val LIST = """
            {"results":[{"id":101,"title":"Ainda Estou Aqui","release_date":"2024-09-19","vote_average":8.1,"poster_path":"/a.jpg","popularity":3}]}
        """
    }
}

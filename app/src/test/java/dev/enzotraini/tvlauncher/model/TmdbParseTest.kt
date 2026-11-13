package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbParseTest {
    @Test
    fun parsesMoviesSeriesAndSkipsPeople() {
        val items = parseTmdbList(LIST_JSON)
        assertEquals(listOf(101 to MediaKind.MOVIE, 202 to MediaKind.SERIES), items.map { it.id to it.kind })
        val movie = items.first()
        assertEquals("Ainda Estou Aqui", movie.title)
        assertEquals(2024, movie.year)
        assertEquals(8.14, movie.rating!!, 0.001)
        assertEquals("/a.jpg", movie.posterPath)
        assertEquals("/b.jpg", movie.backdropPath)
        val series = items[1]
        assertEquals("Senna", series.title)
        assertNull(series.rating)
        assertNull(series.posterPath)
        assertEquals("", series.overview)
    }

    @Test
    fun forcedKindWinsAndBlankJsonIsEmpty() {
        val movies = parseTmdbList(MOVIE_JSON, MediaKind.MOVIE)
        assertEquals(MediaKind.MOVIE, movies.single().kind)
        assertEquals("O Agente", movies.single().title)
        assertTrue(parseTmdbList("").isEmpty())
        assertTrue(parseTmdbList("{").isEmpty())
        assertTrue(parseTmdbList("""{"results":[]}""").isEmpty())
    }

    @Test
    fun watchProvidersStayInBrazilAndDoNotRepeat() {
        val details = parseTmdbDetails(DETAIL_JSON, MediaKind.MOVIE)
        requireNotNull(details)
        assertEquals("Ainda Estou Aqui", details.item.title)
        assertEquals(
            listOf(
                8 to OfferKind.FLATRATE,
                307 to OfferKind.FLATRATE,
                300 to OfferKind.FREE,
                119 to OfferKind.RENT,
            ),
            details.offers.map { it.providerId to it.kind },
        )
        assertTrue(details.offers.none { it.providerName == "Hulu" })
    }

    @Test
    fun imageUrlAndRequestDoNotHideTheKeyInTheCacheName() {
        assertEquals("https://image.tmdb.org/t/p/w780/a.jpg", tmdbImage("/a.jpg", "w780"))
        assertNull(tmdbImage(null, "w780"))
        val urls = TmdbUrls("sekret-key")
        val trending = urls.trending()
        assertTrue(trending.startsWith("https://api.themoviedb.org/3/trending/all/week?"))
        assertTrue(trending.contains("api_key=sekret-key"))
        assertTrue(trending.contains("language=pt-BR"))
        assertTrue(trending.contains("region=BR"))
        assertEquals("trending-all-week", TmdbRepository.KEY_TRENDING)
        assertTrue(!TmdbRepository.KEY_TRENDING.contains("sekret-key"))
    }

    private companion object {
        val LIST_JSON = """
            {
              "results": [
                {
                  "id": 101,
                  "media_type": "movie",
                  "title": "Ainda Estou Aqui",
                  "overview": "Uma família.",
                  "poster_path": "/a.jpg",
                  "backdrop_path": "/b.jpg",
                  "release_date": "2024-09-19",
                  "vote_average": 8.14,
                  "popularity": 40.2
                },
                {
                  "id": 202,
                  "media_type": "tv",
                  "name": "Senna",
                  "overview": "",
                  "poster_path": null,
                  "backdrop_path": "/s.jpg",
                  "first_air_date": "2024-11-29",
                  "vote_average": 0,
                  "popularity": 12
                },
                {"id": 303, "media_type": "person", "name": "Alguém", "popularity": 99}
              ]
            }
        """.trimIndent()

        val MOVIE_JSON = """
            {"results":[{"id":7,"title":"O Agente","release_date":"2021-01-02","vote_average":7.2}]}
        """.trimIndent()

        val DETAIL_JSON = """
            {
              "id": 101,
              "title": "Ainda Estou Aqui",
              "overview": "Uma família.",
              "poster_path": "/a.jpg",
              "backdrop_path": "/b.jpg",
              "release_date": "2024-09-19",
              "vote_average": 8.1,
              "watch/providers": {
                "results": {
                  "US": {"flatrate": [{"provider_id": 15, "provider_name": "Hulu", "logo_path": "/h.png"}]},
                  "BR": {
                    "flatrate": [
                      {"provider_id": 8, "provider_name": "Netflix", "logo_path": "/n.png"},
                      {"provider_id": 307, "provider_name": "Globoplay", "logo_path": "/g.png"}
                    ],
                    "rent": [
                      {"provider_id": 8, "provider_name": "Netflix", "logo_path": "/n.png"},
                      {"provider_id": 119, "provider_name": "Amazon Prime Video", "logo_path": "/p.png"}
                    ],
                    "free": [
                      {"provider_id": 300, "provider_name": "Pluto TV", "logo_path": "/pl.png"}
                    ]
                  }
                }
              }
            }
        """.trimIndent()
    }
}

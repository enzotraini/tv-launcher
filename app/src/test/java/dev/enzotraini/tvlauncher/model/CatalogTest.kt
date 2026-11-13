package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun shelvesMatchTheHomeTabs() {
        val shelves = RecommendationCatalog.current().shelves()
        assertEquals(listOf("explorar", "filmes", "series"), shelves.map { it.id })
        assertEquals(listOf("Explorar", "Filmes", "Séries"), shelves.map { it.title })
        assertTrue(shelves.all { it.items.size >= 3 && it.items.any { title -> title.hero } })
        shelves.forEach { shelf ->
            val ids = shelf.items.map { it.id }
            assertEquals(ids.size, ids.toSet().size)
        }
        val allIds = shelves.flatMap { it.items }.map { it.id }
        assertEquals(allIds.size, allIds.toSet().size)
        assertTrue(shelves.flatMap { it.items }.all { it.artwork.isNotBlank() })
    }

    @Test
    fun filmesTabMatchesTheApprovedRow() {
        val row = RecommendationCatalog.current().shelves().first { it.id == "filmes" }.featured()
        assertEquals(
            listOf("Noite na Estação", "Sombras da Chuva", "O Chamado da Floresta"),
            row.map { it.name },
        )
        assertTrue(row.first().hero)
        assertEquals(Kind.SERIES, row.first().kind)
        assertEquals(
            listOf(
                "Série · Mistério · 1 temporada",
                "Filme · Drama · 2h 15 min",
                "Filme · Aventura · 1h 50 min",
            ),
            row.map { it.metadata(if (it.kind == Kind.MOVIE) "Filme" else "Série") },
        )
    }

    @Test
    fun monogramsSkipArticles() {
        assertEquals("AE", monogram("Ainda Estou Aqui"))
        assertEquals("PC", monogram("O Poderoso Chefão"))
        assertEquals("R6", monogram("Round 6"))
        assertEquals("SE", monogram("Senna"))
        assertEquals("LU", monogram("The Last of Us"))
        assertEquals("AC", monogram("O Auto da Compadecida"))
        assertEquals("MM", monogram("Mad Max: Estrada da Fúria"))
    }
}

package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun shelvesArePortugueseAndUnique() {
        val shelves = RecommendationCatalog.current().shelves()
        assertEquals(
            listOf(
                "Continuar assistindo",
                "Filmes para você",
                "Séries para você",
                "Feito no Brasil",
            ),
            shelves.map { it.title },
        )
        assertTrue(shelves.all { it.items.isNotEmpty() })
        val ids = shelves.flatMap { it.items }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun onlyContinueShelfCarriesProgress() {
        val shelves = RecommendationCatalog.current().shelves()
        val current = shelves.first { it.id == "continuar" }
        assertTrue(current.items.all { it.progress != null && it.progress in 0f..1f })
        shelves.filter { it.id != "continuar" }.forEach { shelf ->
            assertTrue(shelf.items.all { it.progress == null })
        }
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

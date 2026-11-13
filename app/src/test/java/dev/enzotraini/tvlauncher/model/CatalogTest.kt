package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogTest {
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

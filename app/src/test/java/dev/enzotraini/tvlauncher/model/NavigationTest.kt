package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {
    @Test
    fun backMovesUpOneBandAndNeverLeavesHome() {
        assertEquals(HomeBand.ROW, bandAfterBack(HomeBand.DOCK, hasHero = true, hasRow = true))
        assertEquals(HomeBand.HERO, bandAfterBack(HomeBand.ROW, hasHero = true, hasRow = true))
        assertEquals(HomeBand.TABS, bandAfterBack(HomeBand.HERO, hasHero = true, hasRow = true))
        assertEquals(HomeBand.TABS, bandAfterBack(HomeBand.TABS, hasHero = true, hasRow = true))
        assertEquals(HomeBand.ROW, bandAfterBack(HomeBand.DOCK, hasHero = false, hasRow = true))
        assertEquals(HomeBand.TABS, bandAfterBack(HomeBand.ROW, hasHero = false, hasRow = true))
        assertEquals(HomeBand.TABS, bandAfterBack(HomeBand.DOCK, hasHero = false, hasRow = false))
    }

    @Test
    fun shelfMemoryIsPerShelfAndHeroLoops() {
        val memory = ShelfMemory()
        assertEquals(0, memory.recall("filmes", 6))
        memory.remember("filmes", 4)
        memory.remember("series", 1)
        memory.remember("filmes", -1)
        assertEquals(4, memory.recall("filmes", 6))
        assertEquals(1, memory.recall("series", 6))
        assertEquals(2, memory.recall("filmes", 3))
        assertEquals(0, nextHeroIndex(0, 0))
        assertEquals(0, nextHeroIndex(4, 1))
        assertEquals(3, nextHeroIndex(2, 5))
        assertEquals(0, nextHeroIndex(4, 5))
    }

    @Test
    fun homeOpensOnExplorarAndKeepsFourTabs() {
        assertEquals(HomeTab.EXPLORAR, HomeTab.fromId(null))
        assertEquals(HomeTab.AO_VIVO, HomeTab.fromId("ao-vivo"))
        assertEquals(
            listOf("explorar", "filmes", "series", "ao-vivo"),
            HomeTab.entries.map { it.shelfId },
        )
    }
}

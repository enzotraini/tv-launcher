package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusGraphTest {
    @Test
    fun rowEdgesStayAndDoNotWrap() {
        val row = nodes(10 to 100, 11 to 300, 12 to 500)
        val links = linkFocus(listOf(row))
        assertEquals(FOCUS_STAY, links.getValue(10).left)
        assertEquals(11, links.getValue(10).right)
        assertEquals(10, links.getValue(11).left)
        assertEquals(12, links.getValue(11).right)
        assertEquals(FOCUS_STAY, links.getValue(12).right)
        assertEquals(FOCUS_NONE, links.getValue(11).up)
        assertEquals(FOCUS_NONE, links.getValue(11).down)
    }

    @Test
    fun upAndDownUseTheNearestHorizontalCenter() {
        val posters = nodes(1 to 100, 2 to 300, 3 to 500)
        val dock = nodes(8 to 80, 9 to 420)
        val links = linkFocus(listOf(posters, dock))
        assertEquals(8, links.getValue(1).down)
        assertEquals(9, links.getValue(2).down)
        assertEquals(9, links.getValue(3).down)
        assertEquals(1, links.getValue(8).up)
        assertEquals(3, links.getValue(9).up)
        assertEquals(FOCUS_NONE, links.getValue(1).up)
        assertEquals(FOCUS_NONE, links.getValue(8).down)
    }

    @Test
    fun emptyBandIsSkippedAndAWideHeroLinksToItsNeighbors() {
        val tabs = nodes(20 to 40, 21 to 180)
        val hero = nodes(30 to 400)
        val posters = nodes(1 to 120, 2 to 360)
        val links = linkFocus(listOf(tabs, emptyList(), hero, posters))
        assertEquals(FOCUS_STAY, links.getValue(30).left)
        assertEquals(FOCUS_STAY, links.getValue(30).right)
        assertEquals(21, links.getValue(30).up)
        assertEquals(2, links.getValue(30).down)
        assertEquals(30, links.getValue(20).down)
        assertEquals(30, links.getValue(1).up)
        assertEquals(FOCUS_NONE, links.getValue(20).up)
    }

    @Test
    fun equalDistanceKeepsTheLeftItem() {
        val from = FocusNode(1, 200)
        val row = nodes(5 to 100, 6 to 300)
        assertEquals(5, nearest(from, row).id)
    }

    @Test
    fun sectionKeySurvivesAnotherShelfAndTheHomeButton() {
        val memory = ShelfMemory()
        memory.rememberKey("filmes", "poster:filmes:42")
        memory.rememberKey("series", "poster:series:7")
        memory.remember("filmes", 3)
        memory.rememberKey("tab", "tab:explorar")
        assertEquals("poster:filmes:42", memory.recallKey("filmes"))
        assertEquals("poster:series:7", memory.recallKey("series"))
        assertEquals(3, memory.recall("filmes", 8))
        assertNull(memory.recallKey("dock"))
        assertEquals(3, previousHeroIndex(0, 4))
        assertEquals(2, previousHeroIndex(3, 4))
        assertEquals(0, previousHeroIndex(1, 0))
    }

    private fun nodes(vararg pairs: Pair<Int, Int>): List<FocusNode> {
        return pairs.map { (id, center) -> FocusNode(id, center) }
    }
}

package dev.enzotraini.tvlauncher.ui

import dev.enzotraini.tvlauncher.model.RecommendationCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkTest {
    @Test
    fun everyTitleShipsAStill() {
        val titles = RecommendationCatalog.current().shelves().flatMap { it.items }
        assertTrue(titles.isNotEmpty())
        for (title in titles) {
            assertTrue(title.artwork, Artwork.resId(title.artwork) != 0)
        }
    }
}

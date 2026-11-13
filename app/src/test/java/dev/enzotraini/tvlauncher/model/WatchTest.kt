package dev.enzotraini.tvlauncher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchTest {
    @Test
    fun buttonNamesAndLaunchPlan() {
        val netflix = WatchOffer(8, "Netflix", OfferKind.FLATRATE, null)
        val prime = WatchOffer(119, "Amazon Prime Video", OfferKind.RENT, null)
        val youtube = WatchOffer(192, "YouTube", OfferKind.ADS, null)
        val unknown = WatchOffer(999, "Canal Pirata", OfferKind.FREE, null)
        assertEquals("Assistir na Netflix", watchButton(netflix))
        assertEquals("Alugar na Prime Video", watchButton(prime))
        assertEquals(
            PlayAction.OpenApp("com.netflix.ninja"),
            planPlay(netflix, "Senna", setOf("com.netflix.ninja")) { _, _ -> true },
        )
        val view = planPlay(youtube, "Senna", setOf("com.google.android.youtube.tv")) { uri, pkg ->
            uri.contains("search_query=Senna") && pkg == "com.google.android.youtube.tv"
        }
        assertTrue(view is PlayAction.View)
        assertEquals(
            PlayAction.OpenApp("com.google.android.youtube.tv"),
            planPlay(youtube, "Senna", setOf("com.google.android.youtube.tv")) { _, _ -> false },
        )
        assertEquals(PlayAction.NotInstalled, planPlay(netflix, "Senna", emptySet()) { _, _ -> true })
        assertEquals(PlayAction.NotInstalled, planPlay(unknown, "Senna", setOf("com.netflix.ninja")) { _, _ -> true })
        assertEquals("Globoplay", matchProvider(301, "Globo Play")?.label)
        assertEquals("Globoplay", matchProvider(307, "Outro")?.label)
    }
}

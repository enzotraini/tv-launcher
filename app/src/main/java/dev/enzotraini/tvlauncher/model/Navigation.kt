package dev.enzotraini.tvlauncher.model

enum class HomeBand {
    TABS,
    HERO,
    ROW,
    DOCK,
}

enum class HomeTab(val shelfId: String) {
    EXPLORAR("explorar"),
    FILMES("filmes"),
    SERIES("series"),
    AO_VIVO("ao-vivo"),
    ;

    companion object {
        fun fromId(id: String?): HomeTab = entries.firstOrNull { it.shelfId == id } ?: EXPLORAR
    }
}

/** Voltar sobe um nível. Na aba de cima, continua na home. */
fun bandAfterBack(current: HomeBand, hasHero: Boolean, hasRow: Boolean): HomeBand {
    return when (current) {
        HomeBand.DOCK -> when {
            hasRow -> HomeBand.ROW
            hasHero -> HomeBand.HERO
            else -> HomeBand.TABS
        }
        HomeBand.ROW -> if (hasHero) HomeBand.HERO else HomeBand.TABS
        HomeBand.HERO -> HomeBand.TABS
        HomeBand.TABS -> HomeBand.TABS
    }
}

fun nextHeroIndex(current: Int, count: Int): Int {
    if (count <= 1) return 0
    val safe = if (current < 0) 0 else current
    return (safe + 1) % count
}

class ShelfMemory {
    private val spots = HashMap<String, Int>()
    private val keys = HashMap<String, String>()

    fun remember(shelfId: String, index: Int) {
        if (shelfId.isBlank() || index < 0) return
        spots[shelfId] = index
    }

    fun recall(shelfId: String, count: Int): Int {
        if (count <= 0) return 0
        val saved = spots[shelfId] ?: return 0
        return saved.coerceIn(0, count - 1)
    }

    fun rememberKey(section: String, key: String) {
        if (section.isBlank() || key.isBlank()) return
        keys[section] = key
    }

    fun recallKey(section: String): String? = keys[section]
}

const val HERO_INTERVAL_MS = 8_000L
const val MOTION_MS = 200L

package dev.enzotraini.tvlauncher.model

import java.util.Locale

enum class Kind {
    MOVIE,
    SERIES,
}

enum class HomeTab(val shelfId: String) {
    EXPLORAR("explorar"),
    FILMES("filmes"),
    SERIES("series"),
    ;

    companion object {
        fun fromId(id: String?): HomeTab = entries.firstOrNull { it.shelfId == id } ?: FILMES
    }
}

data class Title(
    val id: String,
    val name: String,
    val kind: Kind,
    val year: Int,
    val genre: String,
    val detail: String,
    val blurb: String,
    val artwork: String,
    val hero: Boolean = false,
)

fun Title.metadata(kindLabel: String): String = "$kindLabel · $genre · $detail"

data class Shelf(
    val id: String,
    val title: String,
    val items: List<Title>,
)

fun Shelf.featured(): List<Title> {
    if (items.isEmpty()) return emptyList()
    val hero = items.firstOrNull { it.hero } ?: items.first()
    return listOf(hero) + items.filter { it.id != hero.id }
}

/**
 * Fonte das fileiras da home.
 *
 * A implementação atual é [StaticRecommendations]: fotos originais no APK,
 * títulos fictícios, sem rede. Para trocar por TMDB ou outra API, implemente
 * esta interface e devolva a fonte nova em [RecommendationCatalog.current].
 * A chave fica em recommendations.properties (veja o .example), que está
 * no .gitignore. Não coloque a chave no git.
 */
fun interface RecommendationsSource {
    fun shelves(): List<Shelf>
}

object RecommendationCatalog {
    fun current(): RecommendationsSource = StaticRecommendations

    fun find(id: String?): Title? {
        if (id.isNullOrBlank()) return null
        return current().shelves().asSequence().flatMap { it.items.asSequence() }.find { it.id == id }
    }
}

private val SKIP_WORDS = setOf(
    "de", "da", "do", "das", "dos", "e", "o", "a", "os", "as",
    "the", "of", "um", "uma",
)

fun monogram(name: String): String {
    val words = name.split(Regex("[^\\p{L}\\p{N}]+"))
        .map { it.trim() }
        .filter { token ->
            token.isNotEmpty() && token.lowercase(Locale("pt", "BR")) !in SKIP_WORDS
        }
    return when {
        words.size >= 2 ->
            "${words[0].first()}${words[1].first()}".uppercase(Locale("pt", "BR"))
        words.size == 1 -> words[0].take(2).uppercase(Locale("pt", "BR"))
        else -> "•"
    }
}

private fun movie(
    id: String,
    name: String,
    year: Int,
    genre: String,
    detail: String,
    blurb: String,
    artwork: String,
    hero: Boolean = false,
) = Title(id, name, Kind.MOVIE, year, genre, detail, blurb, artwork, hero)

private fun series(
    id: String,
    name: String,
    year: Int,
    genre: String,
    detail: String,
    blurb: String,
    artwork: String,
    hero: Boolean = false,
) = Title(id, name, Kind.SERIES, year, genre, detail, blurb, artwork, hero)

object StaticRecommendations : RecommendationsSource {
    override fun shelves(): List<Shelf> = listOf(
        Shelf(
            id = "explorar",
            title = "Explorar",
            items = listOf(
                movie(
                    "luz-inverno",
                    "Luz de Inverno",
                    2018,
                    "Drama",
                    "1h 58 min",
                    "Um pintor volta à cidade costeira no inverno para terminar um quadro que a luz não deixa.",
                    "luz_inverno",
                    hero = true,
                ),
                movie(
                    "estrada-sal",
                    "Estrada de Sal",
                    2017,
                    "Aventura",
                    "2h 05 min",
                    "Uma mecânica cruza o salar num carro que só liga quando o horizonte está reto.",
                    "estrada_sal",
                ),
                movie(
                    "porto-quieto",
                    "Porto Quieto",
                    2016,
                    "Suspense",
                    "1h 42 min",
                    "Um barco volta ao porto sem a tripulação e com a cabine ainda acesa.",
                    "porto_quieto",
                ),
            ),
        ),
        Shelf(
            id = "filmes",
            title = "Filmes",
            items = listOf(
                series(
                    "noite-estacao",
                    "Noite na Estação",
                    2024,
                    "Mistério",
                    "1 temporada",
                    "Uma arquivista volta à estação fechada e encontra o corredor aceso, com fogo no chão.",
                    "noite_estacao",
                    hero = true,
                ),
                movie(
                    "sombras-chuva",
                    "Sombras da Chuva",
                    2023,
                    "Drama",
                    "2h 15 min",
                    "Um investigador atravessa a cidade molhada atrás de um nome que só aparece nas poças.",
                    "sombras_chuva",
                ),
                movie(
                    "chamado-floresta",
                    "O Chamado da Floresta",
                    2022,
                    "Aventura",
                    "1h 50 min",
                    "Um homem entra na mata fechada seguindo um chamado que não vem de voz nenhuma.",
                    "chamado_floresta",
                ),
            ),
        ),
        Shelf(
            id = "series",
            title = "Séries",
            items = listOf(
                series(
                    "casa-sem-numero",
                    "A Casa sem Número",
                    2021,
                    "Suspense",
                    "1 temporada",
                    "A família muda para uma casa que a rua insiste em não numerar.",
                    "casa_sem_numero",
                    hero = true,
                ),
                series(
                    "mare-vidro",
                    "Maré de Vidro",
                    2020,
                    "Drama",
                    "2 temporadas",
                    "Duas irmãs herdam um farol e um vidro que só embaça quando a maré mente.",
                    "mare_vidro",
                ),
                series(
                    "arquivo-mares",
                    "O Arquivo das Marés",
                    2019,
                    "Mistério",
                    "1 temporada",
                    "Num porão de mapas molhados, cada gaveta guarda uma cidade que a água já levou.",
                    "arquivo_mares",
                ),
            ),
        ),
    )
}

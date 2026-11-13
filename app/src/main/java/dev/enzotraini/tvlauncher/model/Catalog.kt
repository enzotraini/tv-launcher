package dev.enzotraini.tvlauncher.model

import java.util.Locale

enum class Kind {
    MOVIE,
    SERIES,
}

data class Title(
    val id: String,
    val name: String,
    val kind: Kind,
    val year: Int,
    val blurb: String,
    val accent: Int,
    val progress: Float? = null,
)

data class Shelf(
    val id: String,
    val title: String,
    val items: List<Title>,
)

/**
 * Fonte das prateleiras da home.
 *
 * A implementação atual é [StaticRecommendations]: cartazes desenhados no
 * aparelho, sem rede. Para trocar por TMDB ou outra API, implemente esta
 * interface e devolva a fonte nova em [RecommendationCatalog.current].
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
    blurb: String,
    accent: Long,
    progress: Float? = null,
) = Title(id, name, Kind.MOVIE, year, blurb, accent.toInt(), progress)

private fun series(
    id: String,
    name: String,
    year: Int,
    blurb: String,
    accent: Long,
    progress: Float? = null,
) = Title(id, name, Kind.SERIES, year, blurb, accent.toInt(), progress)

object StaticRecommendations : RecommendationsSource {
    override fun shelves(): List<Shelf> = listOf(
        Shelf(
            id = "continuar",
            title = "Continuar assistindo",
            items = listOf(
                movie(
                    "cont-ainda",
                    "Ainda Estou Aqui",
                    2024,
                    "Uma família no Rio espera um pai que a ditadura não devolve.",
                    0xFF1F4E5F,
                    0.35f,
                ),
                series(
                    "cont-shogun",
                    "Shōgun",
                    2024,
                    "Um navio inglês aporta no Japão e cada tradução muda o destino.",
                    0xFF3E3A55,
                    0.62f,
                ),
                series(
                    "cont-bear",
                    "The Bear",
                    2022,
                    "Uma cozinha pequena tenta servir bem sem se queimar no caminho.",
                    0xFF6B3A2A,
                    0.18f,
                ),
                movie(
                    "cont-senna",
                    "Senna",
                    2010,
                    "A carreira de Ayrton contada com o que a pista já tinha filmado.",
                    0xFF1E4D45,
                    0.80f,
                ),
            ),
        ),
        Shelf(
            id = "filmes",
            title = "Filmes para você",
            items = listOf(
                movie(
                    "filme-duna",
                    "Duna: Parte Dois",
                    2024,
                    "Areia, profecia e uma guerra que a primeira parte só prometeu.",
                    0xFF8C4A2F,
                ),
                movie(
                    "filme-ainda",
                    "Ainda Estou Aqui",
                    2024,
                    "Uma família no Rio espera um pai que a ditadura não devolve.",
                    0xFF1F4E5F,
                ),
                movie(
                    "filme-interestelar",
                    "Interestelar",
                    2014,
                    "Um pai atravessa o tempo procurando um lugar para os filhos.",
                    0xFF1B2A4A,
                ),
                movie(
                    "filme-parasita",
                    "Parasita",
                    2019,
                    "Duas famílias dividem a mesma casa e nenhum dos planos cabe nela.",
                    0xFF2F4F3E,
                ),
                movie(
                    "filme-chefao",
                    "O Poderoso Chefão",
                    1972,
                    "Um casamento abre a história de uma família que não passa o bastão em paz.",
                    0xFF4A2C2C,
                ),
                movie(
                    "filme-madmax",
                    "Mad Max: Estrada da Fúria",
                    2015,
                    "Uma perseguição no deserto que quase não troca de marcha.",
                    0xFF8A3E1D,
                ),
            ),
        ),
        Shelf(
            id = "series",
            title = "Séries para você",
            items = listOf(
                series(
                    "serie-shogun",
                    "Shōgun",
                    2024,
                    "Um navio inglês aporta no Japão e cada tradução muda o destino.",
                    0xFF3E3A55,
                ),
                series(
                    "serie-bear",
                    "The Bear",
                    2022,
                    "Uma cozinha pequena tenta servir bem sem se queimar no caminho.",
                    0xFF6B3A2A,
                ),
                series(
                    "serie-succession",
                    "Succession",
                    2018,
                    "Os filhos de um magnata disputam a empresa como quem disputa o almoço.",
                    0xFF243028,
                ),
                series(
                    "serie-round6",
                    "Round 6",
                    2021,
                    "Brincadeiras de infância com uma dívida que não aceita empate.",
                    0xFF8C3A4A,
                ),
                series(
                    "serie-dark",
                    "Dark",
                    2017,
                    "Uma cidade, uma caverna e um relógio fora do calendário.",
                    0xFF2C3D4F,
                ),
                series(
                    "serie-lastofus",
                    "The Last of Us",
                    2023,
                    "Uma viagem longa por um país vazio, com pouca gente para confiar.",
                    0xFF3F4A32,
                ),
            ),
        ),
        Shelf(
            id = "brasil",
            title = "Feito no Brasil",
            items = listOf(
                movie(
                    "br-cidade",
                    "Cidade de Deus",
                    2002,
                    "A favela cresce no quadro de quem preferiu a câmera.",
                    0xFF7A3E2E,
                ),
                movie(
                    "br-central",
                    "Central do Brasil",
                    1998,
                    "Uma carta atrasada põe um menino e uma mulher na estrada.",
                    0xFF3D4F6B,
                ),
                movie(
                    "br-senna",
                    "Senna",
                    2010,
                    "A carreira de Ayrton contada com o que a pista já tinha filmado.",
                    0xFF1E4D45,
                ),
                movie(
                    "br-auto",
                    "O Auto da Compadecida",
                    2000,
                    "João Grilo argumenta até na hora em que ninguém costuma ter a última palavra.",
                    0xFF6B5428,
                ),
                movie(
                    "br-tropa",
                    "Tropa de Elite",
                    2007,
                    "O BOPE sobe o morro e a cidade assiste sem escolher um lado.",
                    0xFF2A2E33,
                ),
                movie(
                    "br-marte",
                    "Marte Um",
                    2022,
                    "Um menino em Contagem treina para o espaço enquanto a casa pede outro plano.",
                    0xFF1F3F5B,
                ),
            ),
        ),
    )
}

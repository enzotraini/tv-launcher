package dev.enzotraini.tvlauncher.model

import kotlin.math.abs

/** O foco fica neste item. Não usa o mesmo valor para cima e para baixo. */
const val FOCUS_STAY = Int.MIN_VALUE

/** Não há alvo nessa direção. */
const val FOCUS_NONE = -1

data class FocusNode(
    val id: Int,
    val centerX: Int,
)

data class FocusLink(
    val left: Int,
    val right: Int,
    val up: Int,
    val down: Int,
)

/**
 * Liga faixas pela posição horizontal real.
 * Esquerda e direita ficam na faixa. Cima e baixo escolhem o item da faixa
 * vizinha cujo centro está mais perto. A primeira faixa não aponta para cima
 * e a última não aponta para baixo.
 */
fun linkFocus(rows: List<List<FocusNode>>): Map<Int, FocusLink> {
    val present = rows.filter { it.isNotEmpty() }
    val links = LinkedHashMap<Int, FocusLink>()
    present.forEachIndexed { rowIndex, row ->
        val above = present.getOrNull(rowIndex - 1)
        val below = present.getOrNull(rowIndex + 1)
        row.forEachIndexed { index, node ->
            links[node.id] = FocusLink(
                left = if (index == 0) FOCUS_STAY else row[index - 1].id,
                right = if (index == row.lastIndex) FOCUS_STAY else row[index + 1].id,
                up = if (above == null) FOCUS_NONE else nearest(node, above).id,
                down = if (below == null) FOCUS_NONE else nearest(node, below).id,
            )
        }
    }
    return links
}

fun nearest(from: FocusNode, row: List<FocusNode>): FocusNode {
    var best = row.first()
    var bestDistance = abs(best.centerX - from.centerX)
    for (index in 1 until row.size) {
        val candidate = row[index]
        val distance = abs(candidate.centerX - from.centerX)
        if (distance < bestDistance) {
            best = candidate
            bestDistance = distance
        }
    }
    return best
}

fun previousHeroIndex(current: Int, count: Int): Int {
    if (count <= 1) return 0
    val safe = if (current < 0) 0 else current
    return (safe - 1 + count) % count
}

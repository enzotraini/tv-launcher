package dev.enzotraini.tvlauncher.ui

import android.view.View
import kotlin.math.min

/** Foco do controle: uma linha por vez, sem voltar para o outro lado da fileira. */
object Dpad {
    fun stack(rows: List<List<View>>) {
        val present = rows.filter { it.isNotEmpty() }
        if (present.isEmpty()) return
        present.forEach { row ->
            row.forEachIndexed { index, view ->
                view.nextFocusLeftId = if (index == 0) view.id else row[index - 1].id
                view.nextFocusRightId = if (index == row.lastIndex) view.id else row[index + 1].id
            }
        }
        present.forEachIndexed { rowIndex, row ->
            val above = if (rowIndex == 0) row else present[rowIndex - 1]
            val below = if (rowIndex == present.lastIndex) row else present[rowIndex + 1]
            row.forEachIndexed { index, view ->
                view.nextFocusUpId = above[min(index, above.lastIndex)].id
                view.nextFocusDownId = below[min(index, below.lastIndex)].id
            }
        }
    }
}

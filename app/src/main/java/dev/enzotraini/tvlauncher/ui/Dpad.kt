package dev.enzotraini.tvlauncher.ui

import android.graphics.Rect
import android.view.View
import dev.enzotraini.tvlauncher.model.FOCUS_NONE
import dev.enzotraini.tvlauncher.model.FOCUS_STAY
import dev.enzotraini.tvlauncher.model.FocusNode
import dev.enzotraini.tvlauncher.model.linkFocus

/** Foco do controle: uma faixa por vez, cima e baixo pelo centro horizontal. */
object Dpad {
    fun stack(rows: List<List<View>>) {
        val views = rows.flatten()
        views.forEach { view ->
            if (view.id == View.NO_ID) view.id = View.generateViewId()
        }
        val unmeasured = views.all { centerOf(it) == 0 }
        val nodes = rows.map { row ->
            row.mapIndexed { index, view ->
                val center = centerOf(view).let { value ->
                    if (unmeasured) index * 1_000 else value
                }
                FocusNode(view.id, center)
            }
        }
        val links = linkFocus(nodes)
        views.forEach { view ->
            val link = links[view.id] ?: return@forEach
            view.nextFocusLeftId = resolve(link.left, view.id)
            view.nextFocusRightId = resolve(link.right, view.id)
            view.nextFocusUpId = resolve(link.up, view.id)
            view.nextFocusDownId = resolve(link.down, view.id)
        }
    }

    private fun centerOf(view: View): Int {
        val rect = Rect()
        if (view.getGlobalVisibleRect(rect) && rect.width() > 0) return rect.centerX()
        if (view.width > 0) return view.left + view.width / 2
        return 0
    }

    private fun resolve(target: Int, self: Int): Int {
        return when (target) {
            FOCUS_STAY -> self
            FOCUS_NONE -> View.NO_ID
            else -> target
        }
    }
}

package dev.enzotraini.tvlauncher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.ScrollView

/** Rolagem vertical que não toma o foco nem consome as setas. */
class QuietScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ScrollView(context, attrs, defStyleAttr) {
    init {
        isFocusable = false
        isFocusableInTouchMode = false
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        isVerticalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
    }

    override fun executeKeyEvent(event: KeyEvent): Boolean = false
}

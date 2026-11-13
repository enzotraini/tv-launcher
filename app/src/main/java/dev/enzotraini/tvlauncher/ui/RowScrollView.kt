package dev.enzotraini.tvlauncher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.HorizontalScrollView

/** Fileira horizontal que rola com o filho focado e nunca fica com o foco. */
class RowScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : HorizontalScrollView(context, attrs, defStyleAttr) {
    init {
        isFocusable = false
        isFocusableInTouchMode = false
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
    }

    override fun executeKeyEvent(event: KeyEvent): Boolean = false
}

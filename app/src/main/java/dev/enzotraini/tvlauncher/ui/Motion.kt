package dev.enzotraini.tvlauncher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.animation.PathInterpolator
import dev.enzotraini.tvlauncher.R
import dev.enzotraini.tvlauncher.model.MOTION_MS
import kotlin.math.roundToInt

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

private val EASE = PathInterpolator(0.2f, 0f, 0f, 1f)

fun View.tvFocus(scale: Float = 1.08f, onChange: (Boolean) -> Unit = {}) {
    isFocusable = true
    isFocusableInTouchMode = true
    setOnFocusChangeListener { target, hasFocus ->
        target.animate().cancel()
        target.animate()
            .scaleX(if (hasFocus) scale else 1f)
            .scaleY(if (hasFocus) scale else 1f)
            .setDuration(MOTION_MS)
            .setInterpolator(EASE)
            .start()
        target.translationZ = if (hasFocus) resources.getDimension(R.dimen.elevation_focus) else 0f
        if (hasFocus) {
            val width = target.width.coerceAtLeast(1)
            val height = target.height.coerceAtLeast(1)
            target.requestRectangleOnScreen(Rect(0, 0, width, height), false)
        }
        onChange(hasFocus)
    }
}

fun View.animatePageColor(to: Int) {
    val from = (getTag(R.id.tag_tint) as? Int) ?: to
    setTag(R.id.tag_tint, to)
    if (from == to) {
        setBackgroundColor(to)
        return
    }
    ValueAnimator.ofArgb(from, to).apply {
        duration = MOTION_MS
        interpolator = EASE
        addUpdateListener { setBackgroundColor(it.animatedValue as Int) }
        start()
    }
}

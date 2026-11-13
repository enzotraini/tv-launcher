package dev.enzotraini.tvlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import dev.enzotraini.tvlauncher.R

/**
 * Recorte em canvas, não só em outline. O outline arredonda no render de hardware
 * da TV; este recorte também vale no desenho em software.
 */
class RoundedImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageView(context, attrs, defStyleAttr) {
    private val clipPath = Path()
    private val clipRect = RectF()
    private val radius = context.resources.getDimension(R.dimen.poster_radius)

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return
        clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(clipRect, radius, radius, Path.Direction.CW)
        val checkpoint = canvas.save()
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
        canvas.restoreToCount(checkpoint)
    }
}

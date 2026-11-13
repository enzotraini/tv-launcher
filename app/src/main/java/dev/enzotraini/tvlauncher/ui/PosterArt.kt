package dev.enzotraini.tvlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import dev.enzotraini.tvlauncher.R

/** Cartaz desenhado no aparelho. Não baixa imagem e não usa pôster protegido por direito autoral. */
class PosterArt @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var accent: Int = 0xFF0E6B5C.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var monogram: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var kindLabel: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var progress: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val kindPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 220
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
    private val corner = resources.getDimension(R.dimen.corner)

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, corner)
            }
        }
        clipToOutline = true
    }

    override fun onDraw(canvas: Canvas) {
        val widthPx = width.toFloat()
        val heightPx = height.toFloat()
        if (widthPx <= 0f || heightPx <= 0f) return
        fill.shader = LinearGradient(
            0f,
            0f,
            0f,
            heightPx,
            mix(accent, Color.WHITE, 0.22f),
            mix(accent, Color.BLACK, 0.45f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, widthPx, heightPx, fill)
        canvas.drawCircle(widthPx * 0.84f, heightPx * 0.16f, widthPx * 0.42f, disc)

        val pad = widthPx * 0.08f
        if (kindLabel.isNotEmpty()) {
            kindPaint.textSize = heightPx * 0.055f
            canvas.drawText(kindLabel, pad, pad + kindPaint.textSize, kindPaint)
        }
        if (monogram.isNotEmpty()) {
            glyph.textSize = heightPx * 0.15f
            canvas.drawText(monogram, pad, heightPx * 0.78f, glyph)
        }
        val fraction = progress
        if (fraction != null) {
            val barHeight = heightPx * 0.028f
            val top = heightPx - barHeight
            canvas.drawRect(0f, top, widthPx, heightPx, track)
            canvas.drawRect(0f, top, widthPx * fraction.coerceIn(0f, 1f), heightPx, bar)
        }
    }

    private fun mix(color: Int, target: Int, amount: Float): Int {
        val ratio = amount.coerceIn(0f, 1f)
        val red = Color.red(color) + ((Color.red(target) - Color.red(color)) * ratio)
        val green = Color.green(color) + ((Color.green(target) - Color.green(color)) * ratio)
        val blue = Color.blue(color) + ((Color.blue(target) - Color.blue(color)) * ratio)
        return Color.rgb(red.toInt(), green.toInt(), blue.toInt())
    }
}

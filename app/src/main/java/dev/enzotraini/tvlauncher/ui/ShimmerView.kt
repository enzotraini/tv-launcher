package dev.enzotraini.tvlauncher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class ShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shift = 0f
    private val tick = object : Runnable {
        override fun run() {
            shift = (shift + 0.03f) % 1.5f
            invalidate()
            postDelayed(this, 32L)
        }
    }

    fun setRunning(running: Boolean) {
        visibility = if (running) VISIBLE else GONE
        removeCallbacks(tick)
        if (running) post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return
        val start = (shift - 0.4f) * width
        paint.shader = LinearGradient(
            start,
            0f,
            start + width * 0.45f,
            height,
            intArrayOf(0xFFE3DDD4.toInt(), 0xFFF7F4EE.toInt(), 0xFFE3DDD4.toInt()),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        val radius = 20f * resources.displayMetrics.density
        canvas.drawRoundRect(0f, 0f, width, height, radius, radius, paint)
    }
}

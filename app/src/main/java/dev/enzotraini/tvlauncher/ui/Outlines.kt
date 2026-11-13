package dev.enzotraini.tvlauncher.ui

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

fun View.applyRoundOutline(radiusPx: Float, clip: Boolean) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
        }
    }
    clipToOutline = clip
}

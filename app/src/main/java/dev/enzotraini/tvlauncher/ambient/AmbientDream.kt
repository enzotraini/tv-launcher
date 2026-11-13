package dev.enzotraini.tvlauncher.ambient

import android.service.dreams.DreamService
import android.view.LayoutInflater
import dev.enzotraini.tvlauncher.databinding.ActivityAmbientBinding

class AmbientDream : DreamService() {
    private var screen: AmbientScreen? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        val binding = ActivityAmbientBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        screen = AmbientScreen(binding) { }
        screen?.start()
    }

    override fun onDetachedFromWindow() {
        screen?.stop()
        screen = null
        super.onDetachedFromWindow()
    }
}

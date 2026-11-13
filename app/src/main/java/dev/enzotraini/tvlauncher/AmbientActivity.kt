package dev.enzotraini.tvlauncher

import android.os.Bundle
import android.view.KeyEvent
import dev.enzotraini.tvlauncher.ambient.AmbientScreen
import dev.enzotraini.tvlauncher.databinding.ActivityAmbientBinding

class AmbientActivity : TvActivity() {
    private var screen: AmbientScreen? = null

    override fun idleEnabled(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAmbientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        screen = AmbientScreen(binding) { }
    }

    override fun onResume() {
        super.onResume()
        screen?.start()
    }

    override fun onPause() {
        screen?.stop()
        super.onPause()
    }

    override fun onMenuKey() {
        finish()
    }

    @Deprecated("Sair do descanso volta para a tela de baixo.")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        finish()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val volume = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        if (volume) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) {
            finish()
            return true
        }
        return true
    }
}

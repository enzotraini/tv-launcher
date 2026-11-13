package dev.enzotraini.tvlauncher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import dev.enzotraini.tvlauncher.store.DeviceStores

open class TvActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val idle = Runnable {
        if (!isFinishing) onIdle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        immersive()
    }

    override fun onResume() {
        super.onResume()
        scheduleIdle()
    }

    override fun onPause() {
        handler.removeCallbacks(idle)
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        scheduleIdle()
        val opensInputs = event.keyCode == KeyEvent.KEYCODE_MENU ||
            event.keyCode == KeyEvent.KEYCODE_TV_INPUT
        if (opensInputs && event.action == KeyEvent.ACTION_UP) {
            onMenuKey()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    protected open fun idleEnabled(): Boolean = true

    protected open fun onIdle() {
        startActivity(Intent(this, AmbientActivity::class.java))
    }

    protected open fun onMenuKey() {
        openInputs()
    }

    protected fun openInputs() {
        startActivity(Intent(this, InputsActivity::class.java))
    }

    private fun scheduleIdle() {
        handler.removeCallbacks(idle)
        if (!idleEnabled()) return
        val minutes = try {
            val config = LauncherConfig.from(this)
            DeviceStores.readWallpaper(this, config.idleMinutes).idleMinutes
        } catch (_: RuntimeException) {
            5
        }
        handler.postDelayed(idle, minutes.coerceIn(1, 180) * 60_000L)
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}

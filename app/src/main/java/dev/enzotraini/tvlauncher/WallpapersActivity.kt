package dev.enzotraini.tvlauncher

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import dev.enzotraini.tvlauncher.databinding.ActivityWallpapersBinding
import dev.enzotraini.tvlauncher.image.PhotoBitmaps
import dev.enzotraini.tvlauncher.model.WallpaperPref
import dev.enzotraini.tvlauncher.model.collectPhotos
import dev.enzotraini.tvlauncher.model.nextIdle
import dev.enzotraini.tvlauncher.model.nextInterval
import dev.enzotraini.tvlauncher.store.DeviceStores
import dev.enzotraini.tvlauncher.ui.Dpad
import dev.enzotraini.tvlauncher.ui.dp
import dev.enzotraini.tvlauncher.ui.tvFocus
import java.io.File

class WallpapersActivity : TvActivity() {
    private lateinit var binding: ActivityWallpapersBinding
    private var pref = WallpaperPref()
    private var photos: List<File> = emptyList()
    private var focused: File? = null
    private val photoViews = mutableListOf<View>()
    private val actionViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpapersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val config = LauncherConfig.from(this)
        pref = DeviceStores.readWallpaper(this, config.idleMinutes)
        reload()
    }

    private fun reload() {
        photos = collectPhotos(DeviceStores.wallpaperDirs(this))
        binding.wallpaperRow.removeAllViews()
        binding.wallpaperActions.removeAllViews()
        photoViews.clear()
        actionViews.clear()
        binding.wallpaperEmpty.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        photos.forEach { file ->
            val card = photoCard(file)
            val params = LinearLayout.LayoutParams(dp(188), LinearLayout.LayoutParams.MATCH_PARENT)
            params.marginEnd = dp(16)
            binding.wallpaperRow.addView(card, params)
            photoViews += card
        }
        action(getString(R.string.action_rotate)) { rotateFocused() }
        val focusedHidden = focused?.name?.let { it in pref.hidden } == true
        action(getString(if (focusedHidden) R.string.action_restore else R.string.action_use)) { toggleFocused() }
        action(shuffleLabel()) { update(pref.copy(shuffle = !pref.shuffle)) }
        action(getString(R.string.interval_value, pref.intervalSeconds)) {
            update(pref.copy(intervalSeconds = nextInterval(pref.intervalSeconds)))
        }
        action(getString(R.string.idle_value, pref.idleMinutes)) {
            update(pref.copy(idleMinutes = nextIdle(pref.idleMinutes)))
        }
        action(getString(R.string.action_preview)) {
            startActivity(Intent(this, AmbientActivity::class.java))
        }
        val rows = buildList {
            if (photoViews.isNotEmpty()) add(photoViews.toList())
            if (actionViews.isNotEmpty()) add(actionViews.toList())
        }
        Dpad.stack(rows)
        (photoViews.firstOrNull() ?: actionViews.firstOrNull())?.requestFocus()
    }

    private fun photoCard(file: File): View {
        val hidden = file.name in pref.hidden
        val quarters = pref.quarters[file.name] ?: 0
        val root = FrameLayout(this)
        root.id = View.generateViewId()
        root.background = getDrawable(R.drawable.bg_card)
        root.foreground = getDrawable(R.drawable.focus_card)
        root.tvFocus(1.04f) { focusedNow ->
            if (focusedNow) focused = file
        }
        root.setOnClickListener {
            focused = file
            rotateFocused()
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(124),
            )
        }
        val caption = TextView(this).apply {
            text = file.name + "\n" + (if (hidden) getString(R.string.wallpaper_off) else getString(R.string.wallpaper_on)) +
                " · ${quarters * 90}°"
            setTextColor(getColor(R.color.ink))
            textSize = 14f
            typeface = resources.getFont(R.font.inter_semibold)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            )
        }
        root.addView(image)
        root.addView(caption)
        root.contentDescription = caption.text
        app().io.execute {
            val bitmap = PhotoBitmaps.decode(file, quarters, 640)
            image.post { if (bitmap != null) image.setImageBitmap(bitmap) }
        }
        return root
    }

    private fun action(label: String, onClick: () -> Unit) {
        val view = TextView(this).apply {
            id = View.generateViewId()
            text = label
            setTextColor(getColor(R.color.ink))
            textSize = 16f
            typeface = resources.getFont(R.font.inter_semibold)
            background = getDrawable(R.drawable.bg_card)
            foreground = getDrawable(R.drawable.focus_card)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            isClickable = true
            isFocusable = true
            tvFocus(1.04f)
            setOnClickListener { onClick() }
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        params.marginEnd = dp(12)
        binding.wallpaperActions.addView(view, params)
        actionViews += view
    }

    private fun rotateFocused() {
        val file = focused ?: photos.firstOrNull() ?: return
        val next = ((pref.quarters[file.name] ?: 0) + 1) % 4
        update(pref.copy(quarters = pref.quarters + (file.name to next)))
    }

    private fun toggleFocused() {
        val file = focused ?: photos.firstOrNull() ?: return
        val hidden = pref.hidden.toMutableSet()
        if (!hidden.add(file.name)) hidden.remove(file.name)
        update(pref.copy(hidden = hidden))
    }

    private fun update(next: WallpaperPref) {
        pref = next.normalized()
        DeviceStores.writeWallpaper(this, pref)
        reload()
    }

    private fun shuffleLabel(): String {
        return getString(if (pref.shuffle) R.string.shuffle_on else R.string.shuffle_off)
    }
}

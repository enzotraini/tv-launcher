package dev.enzotraini.tvlauncher.ambient

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import dev.enzotraini.tvlauncher.LauncherConfig
import dev.enzotraini.tvlauncher.app
import dev.enzotraini.tvlauncher.databinding.ActivityAmbientBinding
import dev.enzotraini.tvlauncher.image.PhotoBitmaps
import dev.enzotraini.tvlauncher.model.Load
import dev.enzotraini.tvlauncher.model.MediaItem
import dev.enzotraini.tvlauncher.model.WallpaperPref
import dev.enzotraini.tvlauncher.model.ambientDate
import dev.enzotraini.tvlauncher.model.clockLabel
import dev.enzotraini.tvlauncher.model.collectPhotos
import dev.enzotraini.tvlauncher.model.playlist
import dev.enzotraini.tvlauncher.model.temperatureLabel
import dev.enzotraini.tvlauncher.model.tmdbImage
import dev.enzotraini.tvlauncher.store.DeviceStores
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class AmbientScreen(
    private val binding: ActivityAmbientBinding,
    private val onStop: () -> Unit,
) {
    private val context = binding.root.context
    private val handler = Handler(Looper.getMainLooper())
    private val slides = ArrayList<Slide>()
    private var index = 0
    private var showingFront = false
    private var intervalMs = 30_000L
    private var started = false

    private val clock = object : Runnable {
        override fun run() {
            val now = LocalTime.now()
            binding.ambientClock.text = clockLabel(now)
            binding.ambientDate.text = ambientDate(LocalDate.now())
            handler.postDelayed(this, 1_000L)
        }
    }

    private val advance = object : Runnable {
        override fun run() {
            if (slides.size > 1) {
                index = (index + 1) % slides.size
                show(slides[index])
            }
            handler.postDelayed(this, intervalMs)
        }
    }

    fun start() {
        if (started) return
        started = true
        clock.run()
        val config = LauncherConfig.from(context)
        val pref = DeviceStores.readWallpaper(context, config.idleMinutes)
        intervalMs = pref.intervalSeconds.coerceIn(10, 3_600) * 1_000L
        loadWeather(config)
        context.app().io.execute {
            val built = buildSlides(pref)
            binding.root.post {
                if (!started) return@post
                slides.clear()
                slides += built
                if (slides.isNotEmpty()) show(slides.first())
                handler.postDelayed(advance, intervalMs)
            }
        }
    }

    fun stop() {
        started = false
        handler.removeCallbacks(clock)
        handler.removeCallbacks(advance)
        binding.ambientFront.animate().cancel()
        binding.ambientBack.animate().cancel()
        onStop()
    }

    private fun buildSlides(pref: WallpaperPref): List<Slide> {
        val files = collectPhotos(DeviceStores.wallpaperDirs(context))
        val names = playlist(files.map { it.name }, pref, System.currentTimeMillis() / 86_400_000L)
        val photos = names.mapNotNull { name ->
            val file = files.firstOrNull { it.name == name } ?: return@mapNotNull null
            Slide.Photo(file, pref.quarters[name] ?: 0)
        }
        if (photos.isNotEmpty()) return photos
        val trending = context.app().tmdb().trending() as? Load.Ready ?: return emptyList()
        return trending.value.mapNotNull { item -> remote(item) }
    }

    private fun remote(item: MediaItem): Slide.Remote? {
        val url = tmdbImage(item.backdropPath ?: item.posterPath, "w1280") ?: return null
        return Slide.Remote(url, item.title)
    }

    private fun show(slide: Slide) {
        when (slide) {
            is Slide.Photo -> {
                binding.ambientCaption.text = ""
                context.app().io.execute {
                    val bitmap = PhotoBitmaps.decode(slide.file, slide.quarters, 1920)
                    if (bitmap != null) binding.root.post { if (started) place(bitmap) }
                }
            }
            is Slide.Remote -> {
                binding.ambientCaption.text = slide.caption
                val target = if (showingFront) binding.ambientBack else binding.ambientFront
                context.app().images.load(slide.url, target) { bitmap ->
                    place(bitmap)
                }
            }
        }
    }

    private fun place(bitmap: Bitmap) {
        val incoming = if (showingFront) binding.ambientBack else binding.ambientFront
        val outgoing = if (showingFront) binding.ambientFront else binding.ambientBack
        showingFront = !showingFront
        incoming.animate().cancel()
        outgoing.animate().cancel()
        incoming.setImageBitmap(bitmap)
        incoming.scaleX = 1f
        incoming.scaleY = 1f
        incoming.translationX = 0f
        incoming.alpha = 0f
        val fadeIn = ObjectAnimator.ofFloat(incoming, View.ALPHA, 0f, 1f).setDuration(250L)
        val fadeOut = ObjectAnimator.ofFloat(outgoing, View.ALPHA, outgoing.alpha, 0f).setDuration(250L)
        val scaleX = ObjectAnimator.ofFloat(incoming, View.SCALE_X, 1f, 1.06f).setDuration(intervalMs)
        val scaleY = ObjectAnimator.ofFloat(incoming, View.SCALE_Y, 1f, 1.06f).setDuration(intervalMs)
        val drift = ObjectAnimator.ofFloat(incoming, View.TRANSLATION_X, 0f, -28f).setDuration(intervalMs)
        val motion = AnimatorSet()
        motion.interpolator = LinearInterpolator()
        motion.playTogether(fadeIn, fadeOut, scaleX, scaleY, drift)
        motion.start()
    }

    private fun loadWeather(config: LauncherConfig) {
        context.app().io.execute {
            val weather = context.app().weather().current(config.weatherLatitude, config.weatherLongitude)
            val label = (weather as? Load.Ready)?.let { temperatureLabel(it.value.celsius, config.weatherCity) }.orEmpty()
            binding.root.post { binding.ambientWeather.text = label }
        }
    }

    private sealed class Slide {
        data class Photo(val file: File, val quarters: Int) : Slide()
        data class Remote(val url: String, val caption: String) : Slide()
    }
}

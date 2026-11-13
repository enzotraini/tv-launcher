package dev.enzotraini.tvlauncher

import android.app.Application
import android.content.Context
import dev.enzotraini.tvlauncher.image.ImageLoader
import dev.enzotraini.tvlauncher.model.DiskTextCache
import dev.enzotraini.tvlauncher.model.TmdbRepository
import dev.enzotraini.tvlauncher.model.WeatherRepository
import dev.enzotraini.tvlauncher.net.UrlHttp
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class App : Application() {
    lateinit var images: ImageLoader
        private set

    val io: ExecutorService = Executors.newFixedThreadPool(3)

    override fun onCreate() {
        super.onCreate()
        images = ImageLoader(this, io)
    }

    fun tmdb(): TmdbRepository {
        return TmdbRepository(
            apiKey = BuildConfig.TMDB_API_KEY,
            http = UrlHttp,
            cache = DiskTextCache(File(cacheDir, "tmdb-json")),
            now = System::currentTimeMillis,
        )
    }

    fun weather(): WeatherRepository {
        return WeatherRepository(
            http = UrlHttp,
            cache = DiskTextCache(File(cacheDir, "weather-json")),
            now = System::currentTimeMillis,
        )
    }
}

fun Context.app(): App = applicationContext as App

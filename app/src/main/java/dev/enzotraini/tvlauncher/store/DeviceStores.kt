package dev.enzotraini.tvlauncher.store

import android.content.Context
import android.content.SharedPreferences
import dev.enzotraini.tvlauncher.model.WallpaperPref
import dev.enzotraini.tvlauncher.model.formatWallpaperPref
import dev.enzotraini.tvlauncher.model.parseWallpaperPref
import java.io.File

object DeviceStores {
    fun wallpaperFile(context: Context): File = File(context.filesDir, "wallpaper-pref.txt")

    fun readWallpaper(context: Context, defaultIdle: Int): WallpaperPref {
        val file = wallpaperFile(context)
        if (!file.isFile) return WallpaperPref(idleMinutes = defaultIdle)
        return parseWallpaperPref(file.readText())
    }

    fun writeWallpaper(context: Context, pref: WallpaperPref) {
        val file = wallpaperFile(context)
        file.parentFile?.mkdirs()
        file.writeText(formatWallpaperPref(pref))
    }

    fun inputNames(context: Context): SharedPreferences {
        return context.getSharedPreferences("input-names", Context.MODE_PRIVATE)
    }

    fun wallpaperDirs(context: Context): List<File> {
        val dirs = ArrayList<File>()
        context.getExternalFilesDir(null)?.let { dirs += File(it, "wallpapers") }
        dirs += File(context.filesDir, "wallpapers")
        dirs.forEach { it.mkdirs() }
        return dirs
    }
}

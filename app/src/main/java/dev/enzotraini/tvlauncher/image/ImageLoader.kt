package dev.enzotraini.tvlauncher.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import dev.enzotraini.tvlauncher.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executor

class ImageLoader(
    context: Context,
    private val executor: Executor,
) {
    private val dir = File(context.cacheDir, "images").apply { mkdirs() }
    private val memory = LruCache<String, Bitmap>(24)

    fun load(url: String?, into: ImageView, onReady: (Bitmap) -> Unit = {}) {
        val token = Any()
        into.setTag(R.id.tag_request, token)
        if (url.isNullOrBlank() || !url.startsWith("https://")) {
            into.setImageDrawable(null)
            return
        }
        memory.get(url)?.let { cached ->
            crossfade(into, cached)
            onReady(cached)
            return
        }
        executor.execute {
            val bitmap = try {
                decode(url)
            } catch (_: Exception) {
                null
            } ?: return@execute
            memory.put(url, bitmap)
            into.post {
                if (into.getTag(R.id.tag_request) !== token) return@post
                crossfade(into, bitmap)
                onReady(bitmap)
            }
        }
    }

    private fun decode(url: String): Bitmap? {
        val file = File(dir, fileName(url))
        if (!file.isFile || file.length() == 0L) {
            download(url, file)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0) {
            file.delete()
            return null
        }
        val sample = sampleSize(bounds.outWidth.coerceAtLeast(bounds.outHeight), 1280)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun download(url: String, file: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ParaVoce/0.2")
        }
        try {
            if (connection.responseCode !in 200..299) throw java.io.IOException("HTTP")
            file.outputStream().use { output -> connection.inputStream.use { it.copyTo(output) } }
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun crossfade(into: ImageView, bitmap: Bitmap) {
        into.alpha = 0f
        into.setImageBitmap(bitmap)
        into.animate().alpha(1f).setDuration(200L).start()
    }

    companion object {
        fun fileName(url: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(40) + ".img"
        }

        fun sampleSize(width: Int, req: Int): Int {
            var sample = 1
            var current = width
            while (current / 2 >= req && sample < 16) {
                current /= 2
                sample *= 2
            }
            return sample
        }
    }
}

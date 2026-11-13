package dev.enzotraini.tvlauncher.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import dev.enzotraini.tvlauncher.model.PhotoFit
import java.io.File

object PhotoBitmaps {
    fun decode(file: File, quarterTurns: Int, reqWidth: Int): Bitmap? {
        if (!file.isFile) return null
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val spec = PhotoFit.fromExif(orientation)
            val degrees = PhotoFit.totalClockwise(spec, quarterTurns)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val oriented = PhotoFit.orientedSize(bounds.outWidth, bounds.outHeight, degrees)
            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageLoader.sampleSize(oriented.width, reqWidth.coerceAtLeast(1))
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            val matrix = Matrix()
            if (spec.flipHorizontal) matrix.postScale(-1f, 1f)
            if (degrees % 360 != 0) matrix.postRotate(degrees.toFloat())
            val turned = if (matrix.isIdentity) {
                decoded
            } else {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
            if (turned !== decoded) decoded.recycle()
            val crop = PhotoFit.centerFill(turned.width, turned.height)
            val cropped = Bitmap.createBitmap(turned, crop.x, crop.y, crop.width, crop.height)
            if (cropped !== turned) turned.recycle()
            cropped
        } catch (_: Exception) {
            null
        }
    }
}

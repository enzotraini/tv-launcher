package dev.enzotraini.tvlauncher.model

data class PhotoOrientation(
    val clockwiseDegrees: Int,
    val flipHorizontal: Boolean,
)

data class PixelSize(val width: Int, val height: Int)

data class CropWindow(val x: Int, val y: Int, val width: Int, val height: Int)

object PhotoFit {
    /** Orientation EXIF 1–8. Valor desconhecido fica em pé, sem espelho. */
    fun fromExif(orientation: Int): PhotoOrientation = when (orientation) {
        2 -> PhotoOrientation(0, true)
        3 -> PhotoOrientation(180, false)
        4 -> PhotoOrientation(180, true)
        5 -> PhotoOrientation(90, true)
        6 -> PhotoOrientation(90, false)
        7 -> PhotoOrientation(270, true)
        8 -> PhotoOrientation(270, false)
        else -> PhotoOrientation(0, false)
    }

    fun userDegrees(quarterTurns: Int): Int = normalizedQuarters(quarterTurns) * 90

    fun totalClockwise(exif: PhotoOrientation, quarterTurns: Int): Int {
        return (exif.clockwiseDegrees + userDegrees(quarterTurns)) % 360
    }

    fun normalizedQuarters(quarterTurns: Int): Int = ((quarterTurns % 4) + 4) % 4

    fun orientedSize(width: Int, height: Int, clockwiseDegrees: Int): PixelSize {
        val degrees = ((clockwiseDegrees % 360) + 360) % 360
        return if (degrees == 90 || degrees == 270) PixelSize(height, width) else PixelSize(width, height)
    }

    fun isPortrait(width: Int, height: Int): Boolean = height > width

    /**
     * Recorte central que preenche 16:9.
     * Foto mais larga perde as laterais. Foto mais alta perde topo e base.
     */
    fun centerFill(srcWidth: Int, srcHeight: Int, aspectW: Int = 16, aspectH: Int = 9): CropWindow {
        require(srcWidth > 0 && srcHeight > 0)
        val target = aspectW.toDouble() / aspectH.toDouble()
        val source = srcWidth.toDouble() / srcHeight.toDouble()
        return if (source > target) {
            val width = (srcHeight * target).toInt().coerceIn(1, srcWidth)
            CropWindow((srcWidth - width) / 2, 0, width, srcHeight)
        } else {
            val height = (srcWidth / target).toInt().coerceIn(1, srcHeight)
            CropWindow(0, (srcHeight - height) / 2, srcWidth, height)
        }
    }
}

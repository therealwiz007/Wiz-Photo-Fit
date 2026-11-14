package com.wizlabs.photofit.engine

import android.graphics.Bitmap
import android.os.Build
import com.wizlabs.photofit.data.ImgFormat
import java.io.ByteArrayOutputStream

/** [Codec] over Bitmap.compress. [target] is already at the exact output pixel size. */
class BitmapCodec(private val target: Bitmap, private val format: ImgFormat) : Codec {
    override val lossy: Boolean = format != ImgFormat.PNG
    private val cache = HashMap<Pair<Float, Boolean>, Bitmap>()

    private fun frame(scale: Float, keep: Boolean): Bitmap {
        if (scale >= 0.999f) return target
        return cache.getOrPut(scale to keep) {
            val w = (target.width * scale).toInt().coerceAtLeast(8); val h = (target.height * scale).toInt().coerceAtLeast(8)
            val small = Bitmap.createScaledBitmap(target, w, h, true)
            if (!keep) small else Bitmap.createScaledBitmap(small, target.width, target.height, true).also { small.recycle() }
        }
    }

    override fun encode(scale: Float, quality: Int, keepSize: Boolean): ByteArray {
        val bmp = frame(scale, keepSize)
        val out = ByteArrayOutputStream(64 * 1024)
        val fmt = when (format) {
            ImgFormat.PNG -> Bitmap.CompressFormat.PNG
            ImgFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        bmp.compress(fmt, quality.coerceIn(1, 100), out)
        return out.toByteArray()
    }

    /** Pixel size of the bitmap that produced the last result at [scale]. */
    fun sizeAt(scale: Float, keep: Boolean): Pair<Int, Int> = if (keep || scale >= 0.999f) target.width to target.height
    else (target.width * scale).toInt().coerceAtLeast(8) to (target.height * scale).toInt().coerceAtLeast(8)

    fun release() { cache.values.forEach { if (it !== target) it.recycle() }; cache.clear() }
}

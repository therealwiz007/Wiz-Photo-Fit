package com.wizlabs.photofit.engine

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** Memory-safe decode: bounds pass, power-of-two sampling to a working size, EXIF rotation applied once. */
object Decode {
    /** Long edge of the working bitmap (enough for any form photo, small enough for low-RAM phones). */
    const val WORK_EDGE = 2048

    fun size(cr: ContentResolver, uri: Uri): Pair<Int, Int>? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // a bounds-only decode always returns null; the size comes back in the options
        val stream = cr.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, o) }
        return if (o.outWidth > 0 && o.outHeight > 0) o.outWidth to o.outHeight else null
    }

    fun sampleFor(w: Int, h: Int, maxEdge: Int): Int {
        var s = 1
        while (maxOf(w, h) / (s * 2) >= maxEdge) s *= 2
        return s
    }

    /** Decodes [uri] with its long edge at most ~[maxEdge] px, upright. Null if unreadable. */
    fun load(cr: ContentResolver, uri: Uri, maxEdge: Int = WORK_EDGE): Bitmap? {
        val (w, h) = size(cr, uri) ?: return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleFor(w, h, maxEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bmp = try {
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: OutOfMemoryError) {
            opts.inSampleSize *= 2
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } ?: return null
        // scale down further if sampling left it bigger than the working size
        val long = maxOf(bmp.width, bmp.height)
        if (long > maxEdge * 1.25f) {
            val f = maxEdge.toFloat() / long
            val s = Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true)
            if (s !== bmp) bmp.recycle(); bmp = s
        }
        val exif = runCatching { cr.openInputStream(uri)?.use { ExifInterface(it) } }.getOrNull()
        val rot = exif?.rotationDegrees ?: 0
        val flip = exif?.isFlipped ?: false
        if (rot != 0 || flip) {
            val m = Matrix().apply { if (rot != 0) postRotate(rot.toFloat()); if (flip) postScale(-1f, 1f) }
            val r = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (r !== bmp) bmp.recycle(); bmp = r
        }
        return bmp
    }
}

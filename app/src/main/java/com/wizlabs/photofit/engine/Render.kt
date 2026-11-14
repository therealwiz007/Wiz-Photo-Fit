package com.wizlabs.photofit.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.wizlabs.photofit.data.ImgFormat
import com.wizlabs.photofit.data.Part
import kotlin.math.roundToInt

/** Turns the working bitmap + crop into the final file bytes for a [Part]. */
object Render {
    data class Output(
        val bytes: ByteArray, val widthPx: Int, val heightPx: Int, val dpi: Int, val format: ImgFormat,
        val quality: Int, val padded: Boolean, val ok: Boolean, val message: String?,
    ) { val kb: Float get() = bytes.size / 1024f }

    /** Crop + resize to the exact target pixels on a white base (JPEG has no transparency). */
    fun crop(src: Bitmap, r: RectF4, w: Int, h: Int, base: Int = Color.WHITE): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(base)
        val p = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val s = Rect(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt().coerceAtMost(src.width), r.bottom.roundToInt().coerceAtMost(src.height))
        // two-step downscale for big reductions keeps edges smooth (Paint filtering is bilinear only)
        if (s.width() > w * 2) {
            val mid = Bitmap.createScaledBitmap(Bitmap.createBitmap(src, s.left, s.top, s.width(), s.height()), w * 2, h * 2, true)
            c.drawBitmap(mid, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), p); mid.recycle()
        } else c.drawBitmap(src, s, RectF(0f, 0f, w.toFloat(), h.toFloat()), p)
        return out
    }

    /**
     * Encode [img] (already exact size) for [part]. [maxOverrideBytes] lets "Make smaller" aim lower.
     */
    fun encode(img: Bitmap, part: Part, maxOverrideBytes: Int? = null, minOverrideBytes: Int? = null): Output {
        val dpi = part.size.dpi
        val fmt = part.format
        val encFmt = if (fmt == ImgFormat.PDF) ImgFormat.JPG else fmt
        val codec = BitmapCodec(img, encFmt)
        val pdfOverhead = if (fmt == ImgFormat.PDF) 900 else 0
        val maxB = (maxOverrideBytes ?: part.kbMax?.let { it * 1000 })?.minus(pdfOverhead)
        val minB = (minOverrideBytes ?: part.kbMin?.let { it * 1024 })?.minus(pdfOverhead)?.coerceAtLeast(0)
        val allowFewer = part.size.px == null && part.size.phys == null || part.size.pxMin != null
        val res = TargetEncoder.run(codec, minB, maxB, allowFewer, pad = if (encFmt == ImgFormat.JPG) Padding::padJpeg else null)
        val out = when (res) {
            is EncodeResult.Ok -> {
                val (w, h) = codec.sizeAt(res.scale, res.keepSize)
                val bytes = if (fmt == ImgFormat.PDF) PdfWriter.wrapJpeg(res.bytes, w, h, dpi) else JpegDensity.set(res.bytes, dpi, encFmt)
                Output(bytes, w, h, dpi, fmt, res.quality, res.padded, true, null)
            }
            is EncodeResult.Fail -> {
                val bytes = if (fmt == ImgFormat.PDF) PdfWriter.wrapJpeg(res.bestBytes, img.width, img.height, dpi) else res.bestBytes
                Output(bytes, img.width, img.height, dpi, fmt, 20, false, false, res.reason)
            }
        }
        codec.release()
        return out
    }
}

/** Writes the DPI into the JFIF header so the file reports the right physical size. */
object JpegDensity {
    fun set(b: ByteArray, dpi: Int, fmt: ImgFormat): ByteArray {
        if (fmt != ImgFormat.JPG || b.size < 20) return b
        // FF D8 FF E0 len(2) 'J''F''I''F' 0 ver(2) units(1) xd(2) yd(2)
        if (b[2] == 0xFF.toByte() && b[3] == 0xE0.toByte() && b[6] == 'J'.code.toByte() && b[7] == 'F'.code.toByte()) {
            val c = b.copyOf()
            c[13] = 1; c[14] = (dpi shr 8).toByte(); c[15] = dpi.toByte(); c[16] = (dpi shr 8).toByte(); c[17] = dpi.toByte()
            return c
        }
        return b
    }
}

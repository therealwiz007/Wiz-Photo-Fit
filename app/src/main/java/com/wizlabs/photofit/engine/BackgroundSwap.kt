package com.wizlabs.photofit.engine

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await

/**
 * Replaces the background behind the person with a flat colour. ML Kit subject segmentation (on-device, via Play
 * services) gives a confidence mask; we soften it (hair-safe smoothstep + two small box passes) and blend.
 */
object BackgroundSwap {
    class NotReady : Exception("Background change is getting ready. Google Play services is fetching it once; try again in a minute.")

    private val client by lazy {
        SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build())
    }

    /** Asks Play services to install the segmentation (and face) models now, so they're ready when needed. */
    fun prepare(ctx: android.content.Context) {
        runCatching {
            com.google.android.gms.common.moduleinstall.ModuleInstall.getClient(ctx).installModules(
                com.google.android.gms.common.moduleinstall.ModuleInstallRequest.newBuilder().addApi(client).build())
        }
    }

    /** Returns a new bitmap; [src] is unchanged. Throws [NotReady] if the model isn't installed yet. */
    suspend fun apply(src: Bitmap, color: Int): Bitmap {
        val res = try { client.process(InputImage.fromBitmap(src, 0)).await() }
        catch (e: Exception) { if (e.message?.contains("download", true) == true || e.message?.contains("module", true) == true) throw NotReady() else throw e }
        val fb = res.foregroundConfidenceMask ?: throw IllegalStateException("No mask")
        val w = src.width; val h = src.height
        val a = FloatArray(w * h); fb.rewind(); fb.get(a)
        for (i in a.indices) a[i] = smooth(a[i])
        boxPass(a, w, h, 3); boxPass(a, w, h, 3)
        val px = IntArray(w * h); src.getPixels(px, 0, w, 0, 0, w, h)
        val cr = (color shr 16) and 0xFF; val cg = (color shr 8) and 0xFF; val cb = color and 0xFF
        for (i in px.indices) {
            val t = a[i]; val c = px[i]
            val r = (((c shr 16) and 0xFF) * t + cr * (1 - t)).toInt()
            val g = (((c shr 8) and 0xFF) * t + cg * (1 - t)).toInt()
            val b = ((c and 0xFF) * t + cb * (1 - t)).toInt()
            px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun smooth(x: Float): Float { val t = ((x - 0.35f) / 0.3f).coerceIn(0f, 1f); return t * t * (3 - 2 * t) }

    /** Separable box filter of radius [r], in place. */
    fun boxPass(a: FloatArray, w: Int, h: Int, r: Int) {
        val tmp = FloatArray(maxOf(w, h))
        for (y in 0 until h) {
            var s = 0f; val o = y * w
            for (x in -r..r) s += a[o + x.coerceIn(0, w - 1)]
            for (x in 0 until w) { tmp[x] = s / (2 * r + 1); s += a[o + (x + r + 1).coerceAtMost(w - 1)] - a[o + (x - r).coerceAtLeast(0)] }
            System.arraycopy(tmp, 0, a, o, w)
        }
        for (x in 0 until w) {
            var s = 0f
            for (y in -r..r) s += a[y.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) { tmp[y] = s / (2 * r + 1); s += a[(y + r + 1).coerceAtMost(h - 1) * w + x] - a[(y - r).coerceAtLeast(0) * w + x] }
            for (y in 0 until h) a[y * w + x] = tmp[y]
        }
    }

    fun colorFor(bg: com.wizlabs.photofit.data.Bg, custom: Int?): Int? = when (bg) {
        com.wizlabs.photofit.data.Bg.WHITE -> 0xFFFFFFFF.toInt()
        com.wizlabs.photofit.data.Bg.LIGHT -> 0xFFDCE9F7.toInt()
        com.wizlabs.photofit.data.Bg.CUSTOM -> custom
        com.wizlabs.photofit.data.Bg.KEEP -> null
    }
}

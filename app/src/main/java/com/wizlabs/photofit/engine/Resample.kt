package com.wizlabs.photofit.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sin

/**
 * High-quality resampling (pure Kotlin on ARGB IntArrays, unit-tested):
 * Lanczos-3 for scaling (sharper and alias-free compared with bilinear), plus a light unsharp mask.
 */
object Resample {
    private fun sinc(x: Float): Float = if (abs(x) < 1e-6f) 1f else (sin(PI * x) / (PI * x)).toFloat()
    private fun lanczos(x: Float): Float = if (abs(x) >= 3f) 0f else sinc(x) * sinc(x / 3f)

    private class Taps(val first: IntArray, val weights: Array<FloatArray>)

    private fun taps(srcLen: Int, dstLen: Int): Taps {
        val scale = srcLen.toFloat() / dstLen
        val filterScale = maxOf(1f, scale)            // widen the kernel when shrinking: this is the anti-alias
        val support = 3f * filterScale
        val first = IntArray(dstLen); val w = Array(dstLen) { FloatArray(0) }
        for (d in 0 until dstLen) {
            val centre = (d + 0.5f) * scale
            val lo = floor(centre - support + 0.5f).toInt().coerceAtLeast(0)
            val hi = ceil(centre + support - 0.5f).toInt().coerceAtMost(srcLen - 1)
            val k = FloatArray(maxOf(1, hi - lo + 1)); var sum = 0f
            for (i in k.indices) { k[i] = lanczos((lo + i + 0.5f - centre) / filterScale); sum += k[i] }
            if (sum != 0f) for (i in k.indices) k[i] /= sum else k[0] = 1f
            first[d] = lo; w[d] = k
        }
        return Taps(first, w)
    }

    /** Scales [src] (sw×sh) to dw×dh with Lanczos-3. Opaque output. */
    fun scale(src: IntArray, sw: Int, sh: Int, dw: Int, dh: Int): IntArray {
        if (sw == dw && sh == dh) return src.copyOf()
        val tx = taps(sw, dw); val ty = taps(sh, dh)
        // horizontal pass into 3 float planes (dw × sh)
        val r = FloatArray(dw * sh); val g = FloatArray(dw * sh); val b = FloatArray(dw * sh)
        for (y in 0 until sh) for (x in 0 until dw) {
            val k = tx.weights[x]; val f = tx.first[x]
            var sr = 0f; var sg = 0f; var sb = 0f
            for (i in k.indices) { val c = src[y * sw + f + i]; sr += k[i] * (c shr 16 and 0xFF); sg += k[i] * (c shr 8 and 0xFF); sb += k[i] * (c and 0xFF) }
            val o = y * dw + x; r[o] = sr; g[o] = sg; b[o] = sb
        }
        val out = IntArray(dw * dh)
        for (y in 0 until dh) { val k = ty.weights[y]; val f = ty.first[y]
            for (x in 0 until dw) {
                var sr = 0f; var sg = 0f; var sb = 0f
                for (i in k.indices) { val o = (f + i) * dw + x; sr += k[i] * r[o]; sg += k[i] * g[o]; sb += k[i] * b[o] }
                out[y * dw + x] = (0xFF shl 24) or (c8(sr) shl 16) or (c8(sg) shl 8) or c8(sb)
            }
        }
        return out
    }

    private fun c8(v: Float): Int = (v + 0.5f).toInt().coerceIn(0, 255)

    /** Unsharp mask with a 3×3 blur. [amount] 0 = off; ~0.3–0.6 recovers the softness left by resizing. */
    fun unsharp(px: IntArray, w: Int, h: Int, amount: Float): IntArray {
        if (amount <= 0f || w < 3 || h < 3) return px.copyOf()
        val out = px.copyOf()
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            var r = 0; var g = 0; var b = 0
            for (dy in -1..1) for (dx in -1..1) {
                val c = px[(y + dy) * w + x + dx]
                val wt = if (dx == 0 && dy == 0) 4 else if (dx == 0 || dy == 0) 2 else 1
                r += wt * (c shr 16 and 0xFF); g += wt * (c shr 8 and 0xFF); b += wt * (c and 0xFF)
            }
            val c = px[y * w + x]
            val or = c shr 16 and 0xFF; val og = c shr 8 and 0xFF; val ob = c and 0xFF
            out[y * w + x] = (0xFF shl 24) or (c8(or + amount * (or - r / 16f)) shl 16) or (c8(og + amount * (og - g / 16f)) shl 8) or c8(ob + amount * (ob - b / 16f))
        }
        return out
    }
}

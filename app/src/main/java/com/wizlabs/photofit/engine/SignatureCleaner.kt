package com.wizlabs.photofit.engine

/**
 * Turns a phone photo of a signature on paper into clean ink on pure white.
 * Adaptive threshold against a local mean (integral image), so shadows and uneven light don't turn grey.
 * Works on ARGB IntArray (pure Kotlin, unit-testable).
 */
object SignatureCleaner {
    enum class Ink(val argb: Int?) { BLACK(0xFF000000.toInt()), BLUE(0xFF0B2A8A.toInt()), KEEP(null) }

    /** @param strength 0..1 (higher = only darker strokes count as ink). */
    fun clean(px: IntArray, w: Int, h: Int, strength: Float = 0.5f, ink: Ink = Ink.BLACK): IntArray {
        val n = w * h
        val gray = IntArray(n)
        for (i in 0 until n) {
            val c = px[i]
            gray[i] = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
        }
        // integral image for O(1) box means
        val ii = LongArray((w + 1) * (h + 1))
        for (y in 1..h) {
            var row = 0L
            for (x in 1..w) { row += gray[(y - 1) * w + x - 1]; ii[y * (w + 1) + x] = ii[(y - 1) * (w + 1) + x] + row }
        }
        val r = maxOf(4, minOf(w, h) / 8)
        val k = 0.10f + 0.15f * strength.coerceIn(0f, 1f)
        val out = IntArray(n)
        for (y in 0 until h) {
            val y0 = maxOf(0, y - r); val y1 = minOf(h, y + r + 1)
            for (x in 0 until w) {
                val x0 = maxOf(0, x - r); val x1 = minOf(w, x + r + 1)
                val sum = ii[y1 * (w + 1) + x1] - ii[y0 * (w + 1) + x1] - ii[y1 * (w + 1) + x0] + ii[y0 * (w + 1) + x0]
                val mean = sum.toFloat() / ((x1 - x0) * (y1 - y0))
                val i = y * w + x
                out[i] = if (gray[i] < mean * (1f - k)) (ink.argb ?: (px[i] or 0xFF000000.toInt())) else 0xFFFFFFFF.toInt()
            }
        }
        return out
    }

    /** Bounding box of ink (non-white) pixels plus [marginFrac] margin; null if no ink. */
    fun inkBounds(px: IntArray, w: Int, h: Int, marginFrac: Float = 0.06f): IntArray? {
        var l = w; var t = h; var rr = -1; var b = -1
        for (y in 0 until h) for (x in 0 until w) if (px[y * w + x] != 0xFFFFFFFF.toInt()) {
            if (x < l) l = x; if (x > rr) rr = x; if (y < t) t = y; if (y > b) b = y
        }
        if (rr < 0) return null
        val mx = ((rr - l + 1) * marginFrac).toInt() + 2; val my = ((b - t + 1) * marginFrac).toInt() + 2
        return intArrayOf(maxOf(0, l - mx), maxOf(0, t - my), minOf(w - 1, rr + mx), minOf(h - 1, b + my))
    }
}

package com.wizlabs.photofit.engine

/**
 * Encodes an image so the file lands inside a KB range. Pure logic over [Codec] so it is unit-tested with a fake.
 *
 * Order: exact pixel size first, then the highest JPEG quality that fits, then (only if quality 20 is still too big)
 * shrink: either fewer pixels (when the spec allows a range) or "soften" (downscale and scale back, keeping exact px).
 * Below the minimum: raise quality to 100, then pad with a harmless JPEG comment block.
 */
interface Codec {
    /** [scale] < 1 shrinks the image detail; [keepSize] keeps the exact output pixel size (soften). */
    fun encode(scale: Float, quality: Int, keepSize: Boolean): ByteArray
    val lossy: Boolean
}

sealed interface EncodeResult {
    data class Ok(val bytes: ByteArray, val quality: Int, val scale: Float, val keepSize: Boolean, val padded: Boolean) : EncodeResult
    data class Fail(val bestBytes: ByteArray, val reason: String) : EncodeResult
}

object TargetEncoder {
    private val SCALES = floatArrayOf(0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)

    /**
     * @param minBytes lower bound in bytes or null; @param maxBytes upper bound or null.
     * @param allowFewerPixels true when the spec accepts smaller pixel sizes (pxMin, or no exact px).
     * @param pad pads a JPEG to an exact byte length; null = padding unavailable (PNG handled by caller).
     */
    fun run(
        codec: Codec,
        minBytes: Int?,
        maxBytes: Int?,
        allowFewerPixels: Boolean,
        pad: ((ByteArray, Int) -> ByteArray)? = Padding::padJpeg,
        startQuality: Int = 95,
    ): EncodeResult {
        if (!codec.lossy) {
            val b = codec.encode(1f, 100, true)
            return check(b, minBytes, maxBytes, 100, 1f, true, pad, codec)
        }
        if (maxBytes == null) {
            val b = codec.encode(1f, startQuality, true)
            return check(b, minBytes, null, startQuality, 1f, true, pad, codec)
        }
        // aim a little under the max. Callers pass max as KB x 1000 and min as KB x 1024, so a file passes
        // whether the portal counts a KB as 1000 or 1024 bytes.
        val aim = maxOf(minBytes ?: 0, (maxBytes * 0.98).toInt())
        search(codec, 1f, true, aim, startQuality)?.let { (q, b) -> return check(b, minBytes, maxBytes, q, 1f, true, pad, codec) }

        var smallest = codec.encode(1f, 20, true)
        for (s in SCALES) {
            val keep = !allowFewerPixels
            search(codec, s, keep, aim, startQuality)?.let { (q, b) -> return check(b, minBytes, maxBytes, q, s, keep, pad, codec) }
            val b = codec.encode(s, 20, keep); if (b.size < smallest.size) smallest = b
        }
        return EncodeResult.Fail(smallest, "Cannot get below ${kb(smallest.size)} KB at this size. Try a smaller pixel size or a higher KB limit.")
    }

    /** Largest quality in [20, start] whose output is <= aim; null if even 20 is too big. ≤ 8 encodes. */
    private fun search(codec: Codec, scale: Float, keep: Boolean, aim: Int, start: Int): Pair<Int, ByteArray>? {
        val top = codec.encode(scale, start, keep)
        if (top.size <= aim) return start to top
        var lo = 20; var hi = start - 1
        val low = codec.encode(scale, lo, keep)
        if (low.size > aim) return null
        var best = lo to low
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val b = codec.encode(scale, mid, keep)
            if (b.size <= aim) { best = mid to b; lo = mid + 1 } else hi = mid - 1
        }
        return best
    }

    private fun check(b: ByteArray, minB: Int?, maxB: Int?, q: Int, s: Float, keep: Boolean, pad: ((ByteArray, Int) -> ByteArray)?, codec: Codec): EncodeResult {
        if (maxB != null && b.size > maxB) return EncodeResult.Fail(b, "Smallest file is ${kb(b.size)} KB, above the ${kb(maxB)} KB limit.")
        if (minB == null || b.size >= minB) return EncodeResult.Ok(b, q, s, keep, false)
        // too small: best quality first, then pad
        var out = b; var outQ = q
        if (codec.lossy && q < 100) { val hq = codec.encode(s, 100, keep); if (maxB == null || hq.size <= maxB) { out = hq; outQ = 100 } }
        if (out.size >= minB) return EncodeResult.Ok(out, outQ, s, keep, false)
        if (pad == null) return EncodeResult.Fail(out, "File is only ${kb(out.size)} KB; the form needs at least ${kb(minB)} KB.")
        val target = if (maxB != null) minOf(maxB, minB + 1024) else minB + 1024
        return EncodeResult.Ok(pad(out, target), outQ, s, keep, true)
    }

    fun kb(bytes: Int): String = String.format(java.util.Locale.US, if (bytes < 10 * 1024) "%.1f" else "%.0f", bytes / 1024f)
}

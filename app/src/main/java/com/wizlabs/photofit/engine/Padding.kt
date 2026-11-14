package com.wizlabs.photofit.engine

/** Grows a JPEG to an exact byte length with COM (comment) segments right after SOI. Image data is untouched. */
object Padding {
    private const val MAX_SEG = 65537 // FF FE + 2 length bytes + 65533 data bytes

    fun padJpeg(src: ByteArray, targetBytes: Int): ByteArray {
        if (src.size >= targetBytes) return src
        require(src.size >= 4 && src[0] == 0xFF.toByte() && src[1] == 0xD8.toByte()) { "not a JPEG" }
        val need = maxOf(4, targetBytes - src.size) // a segment can't be shorter than 4 bytes
        val count = (need + MAX_SEG - 1) / MAX_SEG
        val out = java.io.ByteArrayOutputStream(src.size + need)
        out.write(src, 0, 2)
        // spread evenly so every segment stays within [4, MAX_SEG] bytes
        val base = need / count; val extra = need % count
        for (i in 0 until count) {
            val seg = base + if (i < extra) 1 else 0
            val len = seg - 2 // length field counts itself + data
            out.write(0xFF); out.write(0xFE); out.write(len shr 8 and 0xFF); out.write(len and 0xFF)
            repeat(seg - 4) { out.write(0x20) }
        }
        out.write(src, 2, src.size - 2)
        return out.toByteArray()
    }
}

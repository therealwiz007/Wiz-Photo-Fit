package com.wizlabs.photofit.engine

import java.io.ByteArrayOutputStream

/** Minimal one-page PDF that wraps JPEG bytes as-is (DCTDecode), so the PDF is the JPEG + ~600 bytes. */
object PdfWriter {
    fun wrapJpeg(jpeg: ByteArray, widthPx: Int, heightPx: Int, dpi: Int): ByteArray {
        val wPt = widthPx * 72f / dpi; val hPt = heightPx * 72f / dpi
        val out = ByteArrayOutputStream(jpeg.size + 1024)
        val offsets = IntArray(6)
        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        val f = { v: Float -> String.format(java.util.Locale.US, "%.2f", v) }
        w("%PDF-1.4\n%âãÏÓ\n")
        offsets[1] = out.size(); w("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        offsets[2] = out.size(); w("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")
        offsets[3] = out.size(); w("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${f(wPt)} ${f(hPt)}] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\nendobj\n")
        offsets[4] = out.size(); w("4 0 obj\n<< /Type /XObject /Subtype /Image /Width $widthPx /Height $heightPx /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n")
        out.write(jpeg); w("\nendstream\nendobj\n")
        val content = "q ${f(wPt)} 0 0 ${f(hPt)} 0 0 cm /Im0 Do Q"
        offsets[5] = out.size(); w("5 0 obj\n<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n")
        val xref = out.size()
        w("xref\n0 6\n0000000000 65535 f \n")
        for (i in 1..5) w(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets[i]))
        w("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }
}

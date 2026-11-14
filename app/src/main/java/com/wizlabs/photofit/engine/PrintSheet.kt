package com.wizlabs.photofit.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/** Lays out many copies of a photo on 4×6 in or A4 paper at 300 dpi with cut lines. */
object PrintSheet {
    enum class Paper(val label: String, val wMm: Float, val hMm: Float) { P4X6("4 × 6 in", 101.6f, 152.4f), A4("A4", 210f, 297f) }

    data class Layout(val cols: Int, val rows: Int, val tileW: Int, val tileH: Int, val pageW: Int, val pageH: Int, val x0: Int, val y0: Int, val gap: Int) {
        val count get() = cols * rows
    }

    private fun mmPx(mm: Float, dpi: Int) = (mm / 25.4f * dpi).roundToInt()

    fun layout(paper: Paper, photoWmm: Float, photoHmm: Float, dpi: Int = 300): Layout {
        val pw = mmPx(paper.wMm, dpi); val ph = mmPx(paper.hMm, dpi)
        val m = mmPx(5f, dpi); val g = mmPx(2f, dpi)
        val tw = mmPx(photoWmm, dpi); val th = mmPx(photoHmm, dpi)
        val cols = ((pw - 2 * m + g) / (tw + g)).coerceAtLeast(1)
        val rows = ((ph - 2 * m + g) / (th + g)).coerceAtLeast(1)
        val usedW = cols * tw + (cols - 1) * g; val usedH = rows * th + (rows - 1) * g
        return Layout(cols, rows, tw, th, pw, ph, (pw - usedW) / 2, (ph - usedH) / 2, g)
    }

    private fun drawSheet(c: Canvas, l: Layout, tile: Bitmap, copies: Int, cutLines: Boolean) {
        c.drawColor(Color.WHITE)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        val line = Paint().apply { color = 0xFFAAAAAA.toInt(); strokeWidth = 1f }
        var n = 0
        loop@ for (r in 0 until l.rows) for (cIdx in 0 until l.cols) {
            if (n++ >= copies) break@loop
            val x = l.x0 + cIdx * (l.tileW + l.gap); val y = l.y0 + r * (l.tileH + l.gap)
            c.drawBitmap(tile, null, RectF(x.toFloat(), y.toFloat(), (x + l.tileW).toFloat(), (y + l.tileH).toFloat()), p)
        }
        if (cutLines) {
            for (cIdx in 0..l.cols) { val x = (l.x0 + cIdx * (l.tileW + l.gap) - l.gap / 2f); c.drawLine(x, 0f, x, l.pageH.toFloat(), line) }
            for (r in 0..l.rows) { val y = (l.y0 + r * (l.tileH + l.gap) - l.gap / 2f); c.drawLine(0f, y, l.pageW.toFloat(), y, line) }
        }
    }

    fun renderJpeg(l: Layout, tile: Bitmap, copies: Int, cutLines: Boolean): ByteArray {
        val bmp = Bitmap.createBitmap(l.pageW, l.pageH, Bitmap.Config.RGB_565)
        drawSheet(Canvas(bmp), l, tile, copies, cutLines)
        val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 95, out); bmp.recycle()
        return JpegDensity.set(out.toByteArray(), 300, com.wizlabs.photofit.data.ImgFormat.JPG)
    }

    /** PDF at true paper size (points); the tile is drawn scaled so it prints at the exact mm size. */
    fun renderPdf(paper: Paper, l: Layout, tile: Bitmap, copies: Int, cutLines: Boolean): ByteArray {
        val doc = PdfDocument()
        val wPt = (paper.wMm / 25.4f * 72).roundToInt(); val hPt = (paper.hMm / 25.4f * 72).roundToInt()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(wPt, hPt, 1).create())
        val c = page.canvas
        c.scale(wPt.toFloat() / l.pageW, hPt.toFloat() / l.pageH)
        drawSheet(c, l, tile, copies, cutLines)
        doc.finishPage(page)
        val out = ByteArrayOutputStream(); doc.writeTo(out); doc.close()
        return out.toByteArray()
    }
}

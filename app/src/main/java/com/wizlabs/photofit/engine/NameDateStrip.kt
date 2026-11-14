package com.wizlabs.photofit.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/** White band at the bottom with NAME and date (dd-MM-yyyy), the font sized to fit 92 % of the width. */
object NameDateStrip {
    const val BAND = 0.18f

    /** Draws onto [img] in place; the photo area must already be the top (1 - BAND) of the image. */
    fun draw(img: Bitmap, name: String, date: String) {
        val c = Canvas(img)
        val bandH = img.height * BAND
        val top = img.height - bandH
        c.drawRect(0f, top, img.width.toFloat(), img.height.toFloat(), Paint().apply { color = Color.WHITE })
        val lines = listOf(name.trim().uppercase(), date.trim()).filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val maxW = img.width * 0.92f
        val lineH = bandH / lines.size
        var lo = 4f; var hi = lineH * 0.9f
        repeat(14) { val mid = (lo + hi) / 2; p.textSize = mid; if (lines.all { p.measureText(it) <= maxW }) lo = mid else hi = mid }
        p.textSize = lo
        val fm = p.fontMetrics
        lines.forEachIndexed { i, s ->
            val cy = top + lineH * (i + 0.5f)
            c.drawText(s, img.width / 2f, cy - (fm.ascent + fm.descent) / 2, p)
        }
    }

    fun today(): String = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US).format(java.util.Date())
}

package com.wizlabs.photofit.engine

import com.wizlabs.photofit.data.IntSz
import com.wizlabs.photofit.data.LenUnit
import kotlin.math.roundToInt

/** Length conversions between px / cm / mm / inch at a given DPI. */
object Units {
    fun inches(v: Float, unit: LenUnit, dpi: Int): Float = when (unit) {
        LenUnit.PX -> v / dpi
        LenUnit.CM -> v / 2.54f
        LenUnit.MM -> v / 25.4f
        LenUnit.IN -> v
    }

    fun toPx(v: Float, unit: LenUnit, dpi: Int): Int = if (unit == LenUnit.PX) v.roundToInt() else (inches(v, unit, dpi) * dpi).roundToInt()

    fun toPx(w: Float, h: Float, unit: LenUnit, dpi: Int): IntSz = IntSz(toPx(w, unit, dpi).coerceAtLeast(1), toPx(h, unit, dpi).coerceAtLeast(1))

    fun pxToCm(px: Int, dpi: Int): Float = px * 2.54f / dpi

    fun fromPx(px: Int, unit: LenUnit, dpi: Int): Float = when (unit) {
        LenUnit.PX -> px.toFloat()
        LenUnit.CM -> px * 2.54f / dpi
        LenUnit.MM -> px * 25.4f / dpi
        LenUnit.IN -> px.toFloat() / dpi
    }

    /** "7:9"-style reduced aspect for display. */
    fun ratio(w: Int, h: Int): String {
        tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val g = gcd(w, h).coerceAtLeast(1)
        val a = w / g; val b = h / g
        return if (a <= 40 && b <= 40) "$a:$b" else String.format(java.util.Locale.US, "%.2f:1", w.toFloat() / h)
    }

    fun fmt(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
}

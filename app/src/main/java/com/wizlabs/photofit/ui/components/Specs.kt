package com.wizlabs.photofit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizlabs.photofit.data.Confidence
import com.wizlabs.photofit.data.Part
import com.wizlabs.photofit.engine.Units

object SpecText {
    fun px(p: Part): String { val t = p.size.targetPx(); return "${t.w} × ${t.h} px" }
    fun phys(p: Part): String? {
        val ph = p.size.phys ?: return null
        return "${Units.fmt(ph.first)} × ${Units.fmt(ph.second)} ${p.size.unit.label}"
    }
    fun kb(p: Part): String = when {
        p.kbMin != null && p.kbMax != null -> "${p.kbMin}–${p.kbMax} KB"
        p.kbMax != null -> "up to ${p.kbMax} KB"
        p.kbMin != null -> "at least ${p.kbMin} KB"
        else -> "any KB"
    }
    fun line(p: Part): String = listOfNotNull(px(p), phys(p), kb(p), p.format.ext.uppercase()).joinToString("  ·  ")
}

private val badgeColors = listOf(0xFF4338CA, 0xFF2BD97C, 0xFF4DA3FF, 0xFFFF8A65, 0xFFB388FF, 0xFF4DD0E1, 0xFFFF5A8A, 0xFFAED581)

/** Neutral coloured initials (no exam-body logos). */
@Composable
fun InitialBadge(name: String, modifier: Modifier = Modifier) {
    val initials = name.split(' ', '/', '(').filter { it.isNotBlank() && it[0].isLetterOrDigit() }.take(2).joinToString("") { it.take(if (it.all { c -> c.isUpperCase() }) 2 else 1) }.take(3).uppercase()
    val c = Color(badgeColors[(name.hashCode() and 0x7fffffff) % badgeColors.size])
    Box(modifier.size(46.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Text(initials, color = c, fontWeight = FontWeight.Black, fontSize = if (initials.length > 2) 13.sp else 16.sp)
    }
}

@Composable
fun ConfidenceDot(c: Confidence, modifier: Modifier = Modifier) {
    val col = when (c) { Confidence.HIGH -> Wiz.Good; Confidence.MEDIUM -> Wiz.Accent; Confidence.LOW -> Wiz.Warn; Confidence.USER -> Wiz.Muted }
    Box(modifier.size(8.dp).clip(CircleShape).background(col))
}

fun confidenceLabel(c: Confidence) = when (c) {
    Confidence.HIGH -> "Checked in several sources"
    Confidence.MEDIUM -> "Mostly confirmed"
    Confidence.LOW -> "Sources differ: double-check"
    Confidence.USER -> "Your own size"
}

package com.wizlabs.photofit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* Small code-drawn glyphs (no icon library): each draws in a square of `size`. */

@Composable
private fun Glyph(size: Dp, draw: DrawScope.(Float) -> Unit) = Canvas(Modifier.size(size)) { draw(this.size.minDimension) }

@Composable fun GlyphNoAds(c: Color, size: Dp = 14.dp) = Glyph(size) { s ->
    val st = Stroke(s * 0.12f, cap = StrokeCap.Round)
    drawRoundRect(c, Offset(s * 0.1f, s * 0.25f), Size(s * 0.8f, s * 0.5f), CornerRadius(s * 0.1f), style = st)
    drawLine(c, Offset(s * 0.12f, s * 0.12f), Offset(s * 0.88f, s * 0.88f), s * 0.12f, StrokeCap.Round)
}

@Composable fun GlyphNoWifi(c: Color, size: Dp = 14.dp) = Glyph(size) { s ->
    val st = Stroke(s * 0.11f, cap = StrokeCap.Round)
    for (i in 1..3) { val r = s * 0.16f * i; drawArc(c, 225f, 90f, false, Offset(s / 2 - r, s * 0.78f - r), Size(r * 2, r * 2), style = st) }
    drawCircle(c, s * 0.07f, Offset(s / 2, s * 0.78f))
    drawLine(c, Offset(s * 0.12f, s * 0.12f), Offset(s * 0.88f, s * 0.88f), s * 0.11f, StrokeCap.Round)
}

@Composable fun GlyphShield(c: Color, size: Dp = 14.dp) = Glyph(size) { s ->
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(s * 0.5f, s * 0.06f); lineTo(s * 0.88f, s * 0.2f); lineTo(s * 0.84f, s * 0.55f)
        quadraticTo(s * 0.75f, s * 0.82f, s * 0.5f, s * 0.95f); quadraticTo(s * 0.25f, s * 0.82f, s * 0.16f, s * 0.55f)
        lineTo(s * 0.12f, s * 0.2f); close()
    }
    drawPath(p, c, style = Stroke(s * 0.11f))
    drawLine(c, Offset(s * 0.34f, s * 0.5f), Offset(s * 0.46f, s * 0.62f), s * 0.11f, StrokeCap.Round)
    drawLine(c, Offset(s * 0.46f, s * 0.62f), Offset(s * 0.68f, s * 0.38f), s * 0.11f, StrokeCap.Round)
}

/** Person in a frame: exam / form photo. */
@Composable fun GlyphExam(c: Color, size: Dp = 30.dp) = Glyph(size) { s ->
    val st = Stroke(s * 0.08f, cap = StrokeCap.Round)
    drawRoundRect(c, Offset(s * 0.14f, s * 0.08f), Size(s * 0.72f, s * 0.84f), CornerRadius(s * 0.1f), style = st)
    drawCircle(c, s * 0.13f, Offset(s / 2, s * 0.38f))
    drawArc(c, 180f, 180f, true, Offset(s * 0.27f, s * 0.56f), Size(s * 0.46f, s * 0.36f))
    drawLine(c, Offset(s * 0.3f, s * 0.92f), Offset(s * 0.7f, s * 0.92f), s * 0.08f)
}

/** Passport booklet. */
@Composable fun GlyphPassport(c: Color, size: Dp = 30.dp) = Glyph(size) { s ->
    val st = Stroke(s * 0.08f, cap = StrokeCap.Round)
    drawRoundRect(c, Offset(s * 0.2f, s * 0.06f), Size(s * 0.6f, s * 0.88f), CornerRadius(s * 0.08f), style = st)
    drawCircle(c, s * 0.16f, Offset(s / 2, s * 0.4f), style = Stroke(s * 0.07f))
    drawLine(c, Offset(s * 0.34f, s * 0.4f), Offset(s * 0.66f, s * 0.4f), s * 0.05f)
    drawLine(c, Offset(s * 0.34f, s * 0.75f), Offset(s * 0.66f, s * 0.75f), s * 0.08f, StrokeCap.Round)
}

/** Arrows squeezing a file: compress. */
@Composable fun GlyphCompress(c: Color, size: Dp = 30.dp) = Glyph(size) { s ->
    val w = s * 0.09f
    drawRoundRect(c, Offset(s * 0.3f, s * 0.3f), Size(s * 0.4f, s * 0.4f), CornerRadius(s * 0.06f), style = Stroke(w))
    fun arrow(x0: Float, y0: Float, x1: Float, y1: Float) {
        drawLine(c, Offset(x0, y0), Offset(x1, y1), w, StrokeCap.Round)
        drawLine(c, Offset(x1, y1), Offset(x1 + (x0 - x1) * 0.45f, y1), w, StrokeCap.Round)
        drawLine(c, Offset(x1, y1), Offset(x1, y1 + (y0 - y1) * 0.45f), w, StrokeCap.Round)
    }
    arrow(s * 0.06f, s * 0.06f, s * 0.24f, s * 0.24f); arrow(s * 0.94f, s * 0.06f, s * 0.76f, s * 0.24f)
    arrow(s * 0.06f, s * 0.94f, s * 0.24f, s * 0.76f); arrow(s * 0.94f, s * 0.94f, s * 0.76f, s * 0.76f)
}

/** Pen stroke: signature. */
@Composable fun GlyphSign(c: Color, size: Dp = 26.dp) = Glyph(size) { s ->
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(s * 0.06f, s * 0.7f); cubicTo(s * 0.2f, s * 0.2f, s * 0.32f, s * 0.2f, s * 0.3f, s * 0.6f)
        cubicTo(s * 0.28f, s * 0.9f, s * 0.48f, s * 0.3f, s * 0.56f, s * 0.5f); cubicTo(s * 0.62f, s * 0.66f, s * 0.75f, s * 0.42f, s * 0.94f, s * 0.48f)
    }
    drawPath(p, c, style = Stroke(s * 0.08f, cap = StrokeCap.Round))
    drawLine(c, Offset(s * 0.06f, s * 0.86f), Offset(s * 0.94f, s * 0.86f), s * 0.05f)
}

/** Two boxes joined: joiner. */
@Composable fun GlyphJoin(c: Color, size: Dp = 26.dp) = Glyph(size) { s ->
    val st = Stroke(s * 0.08f)
    drawRoundRect(c, Offset(s * 0.06f, s * 0.2f), Size(s * 0.4f, s * 0.6f), CornerRadius(s * 0.06f), style = st)
    drawRoundRect(c, Offset(s * 0.54f, s * 0.36f), Size(s * 0.4f, s * 0.28f), CornerRadius(s * 0.06f), style = st)
    drawLine(c, Offset(s * 0.46f, s * 0.5f), Offset(s * 0.54f, s * 0.5f), s * 0.08f)
}

/** Stack of photos: saved. */
@Composable fun GlyphSaved(c: Color, size: Dp = 26.dp) = Glyph(size) { s ->
    val st = Stroke(s * 0.08f)
    drawRoundRect(c, Offset(s * 0.22f, s * 0.08f), Size(s * 0.7f, s * 0.62f), CornerRadius(s * 0.08f), style = st)
    drawRoundRect(c, Offset(s * 0.08f, s * 0.26f), Size(s * 0.7f, s * 0.62f), CornerRadius(s * 0.08f), style = st)
    drawLine(c, Offset(s * 0.18f, s * 0.78f), Offset(s * 0.38f, s * 0.56f), s * 0.07f, StrokeCap.Round)
    drawLine(c, Offset(s * 0.38f, s * 0.56f), Offset(s * 0.68f, s * 0.78f), s * 0.07f, StrokeCap.Round)
}

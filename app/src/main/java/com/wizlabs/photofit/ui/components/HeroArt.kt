package com.wizlabs.photofit.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.ui.motion.rememberMotionAllowed
import com.wizlabs.photofit.ui.motion.rememberThrottledClock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * "Snap to size" hero (original art, drawn in code): yellow corner brackets snap around a head-and-shoulders
 * photo while a half-dial KB gauge swings its needle into the green band. Paths are built once per size;
 * the shared clock is read only in the draw phase (24 fps, still frame when idle / Battery Saver).
 */
@Composable
fun HeroArt(modifier: Modifier = Modifier) {
    val motion = rememberMotionAllowed()
    val clock = rememberThrottledClock(running = motion, fps = 24)
    Box(modifier.size(width = 150.dp, height = 120.dp).drawWithCache {
        val w = size.width; val h = size.height
        // photo card area (left)
        val pw = w * 0.5f; val ph = h * 0.82f; val px = w * 0.02f; val py = (h - ph) / 2
        val head = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(px + pw * 0.5f, py + ph * 0.38f), pw * 0.17f)) }
        val body = Path().apply {
            moveTo(px + pw * 0.16f, py + ph * 0.92f)
            cubicTo(px + pw * 0.2f, py + ph * 0.6f, px + pw * 0.8f, py + ph * 0.6f, px + pw * 0.84f, py + ph * 0.92f); close()
        }
        val bracket = Stroke(w * 0.022f, cap = StrokeCap.Round)
        // gauge (right)
        val gc = Offset(w * 0.79f, h * 0.66f); val gr = w * 0.17f
        val arc = Stroke(w * 0.03f, cap = StrokeCap.Round)
        onDrawBehind {
            val t = clock.value
            // 0..1..0 every 4 s, eased: brackets snap in, needle swings into the band
            val cyc = (t % 4f) / 4f
            val p = if (cyc < 0.5f) easeOut(cyc * 2f) else 1f - easeIn((cyc - 0.5f) * 2f) * 0.85f
            drawRoundRect(Wiz.PanelHi, Offset(px, py), Size(pw, ph), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
            drawPath(head, Wiz.Muted)
            drawPath(body, Wiz.Muted)
            // corner brackets shrink 8 % toward the face as they lock
            val inset = pw * 0.06f * p
            val l = px - pw * 0.04f + inset; val r = px + pw * 1.04f - inset
            val tp = py - ph * 0.04f + inset; val b = py + ph * 1.04f - inset
            val arm = pw * 0.22f
            val col = Wiz.AccentInk
            drawLine(col, Offset(l, tp), Offset(l + arm, tp), bracket.width, StrokeCap.Round); drawLine(col, Offset(l, tp), Offset(l, tp + arm), bracket.width, StrokeCap.Round)
            drawLine(col, Offset(r, tp), Offset(r - arm, tp), bracket.width, StrokeCap.Round); drawLine(col, Offset(r, tp), Offset(r, tp + arm), bracket.width, StrokeCap.Round)
            drawLine(col, Offset(l, b), Offset(l + arm, b), bracket.width, StrokeCap.Round); drawLine(col, Offset(l, b), Offset(l, b - arm), bracket.width, StrokeCap.Round)
            drawLine(col, Offset(r, b), Offset(r - arm, b), bracket.width, StrokeCap.Round); drawLine(col, Offset(r, b), Offset(r, b - arm), bracket.width, StrokeCap.Round)
            // gauge: grey track, green target band, needle
            val tl = Offset(gc.x - gr, gc.y - gr); val sz = Size(gr * 2, gr * 2)
            drawArc(Wiz.PanelHi, 180f, 180f, false, tl, sz, style = arc)
            drawArc(Wiz.Good, 250f, 40f, false, tl, sz, style = arc)
            val ang = (180f + 20f + 70f * p) * (PI.toFloat() / 180f)
            val tip = Offset(gc.x + cos(ang) * gr * 0.86f, gc.y + sin(ang) * gr * 0.86f)
            drawLine(Wiz.Text, gc, tip, w * 0.018f, StrokeCap.Round)
            drawCircle(Wiz.AccentInk, w * 0.026f, gc)
            // "KB" tick lights up when the needle sits in the band
            val lit = (p - 0.85f).coerceIn(0f, 0.15f) / 0.15f
            drawRoundRect(Wiz.Good.copy(alpha = 0.2f + 0.8f * lit), Offset(gc.x - gr * 0.45f, gc.y + gr * 0.22f), Size(gr * 0.9f, gr * 0.34f),
                androidx.compose.ui.geometry.CornerRadius(gr * 0.17f))
        }
    })
}

private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)
private fun easeIn(x: Float) = x * x * x

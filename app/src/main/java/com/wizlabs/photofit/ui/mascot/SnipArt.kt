package com.wizlabs.photofit.ui.mascot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Snip geometry in a 100×100 unit box (same shapes as store-assets/mascot/snip.svg), Studio Indigo colours.
 * All Paths are scratch objects reused every frame, so drawing allocates nothing.
 */
class SnipArt {
    private val tmp = Path()
    private val tail = Path()
    private val stroke3 = Stroke(3.2f, cap = StrokeCap.Round)
    private val stroke2 = Stroke(2.4f, cap = StrokeCap.Round)
    private val handStroke = Stroke(4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    private val ringStroke = Stroke(1.6f, cap = StrokeCap.Round)

    private val tapeCol = Color(0xFF38BDF8)
    private val tickCol = Color(0xFF4338CA)
    private val photo = Color(0xFFBAE6FD)
    private val inkLight = Color(0xFF12142A)
    private val inkDark = Color(0xFFEEF0FF)
    private val drop = Color(0xFF7DD3FC)
    private val cheek = Color(0xFFFDA4AF)

    fun draw(d: DrawScope, p: SnipPose, dark: Boolean) = with(d) {
        val u = size.minDimension / 100f
        val frame = if (dark) inkDark else inkLight
        val ink = inkLight
        val c = p.cur
        scale(u, u, Offset.Zero) {
            val sy = 1f + p.breathe - p.squash
            val sx = 1f - p.breathe * 0.5f + p.squash * 0.6f
            translate(0f, p.bobY - c[F.BOUNCE] * abs(sin(p.t * 9f)) * 3f) {
                rotate(c[F.LEAN], Offset(50f, 95f)) {
                    scale(sx, sy * (1f - c[F.DROOP] * 0.03f), Offset(50f, 95f)) {
                        body(this, p, c, frame, ink, dark)
                    }
                }
            }
            extras(this, p, c, frame)
        }
    }

    private fun body(d: DrawScope, p: SnipPose, c: FloatArray, frame: Color, ink: Color, dark: Boolean) = with(d) {
        // feet
        drawRoundRect(frame, Offset(33f, 90f), Size(13f, 6f), CornerRadius(3f))
        drawRoundRect(frame, Offset(54f, 90f), Size(13f, 6f), CornerRadius(3f))
        // tape tail behind the body: trails right, sways, flourishes, spins
        val lift = -p.flourish * 14f
        val sw = p.tapeSway * 4f
        rotate(p.tapeSpin, Offset(74f, 72f)) {
            tail.reset()
            tail.moveTo(74f, 72f)
            tail.cubicTo(84f + sw, 76f + lift, 90f - sw, 84f + lift * 1.4f, 88f + sw * 2f, 94f + lift * 0.6f)
            tail.lineTo(94f + sw * 2f, 93f + lift * 0.6f)
            tail.cubicTo(96f - sw, 82f + lift * 1.4f, 90f + sw, 72f + lift, 80f, 68f)
            tail.close()
            drawPath(tail, tapeCol)
        }
        // frame, matte, photo
        if (dark) drawRoundRect(Color(0xFF171A2E), Offset(20.5f, 16.5f), Size(59f, 63f), CornerRadius(12f))
        drawRoundRect(frame, Offset(22f, 18f), Size(56f, 60f), CornerRadius(10f))
        drawRoundRect(Color.White, Offset(27f, 23f), Size(46f, 50f), CornerRadius(6f))
        drawRoundRect(photo, Offset(30f, 26f), Size(40f, 44f), CornerRadius(4f))
        // cheeks
        val bl = (c[F.BLUSH] * 0.55f).coerceIn(0f, 0.8f)
        drawCircle(cheek.copy(alpha = bl), 3.4f, Offset(36f, 55f)); drawCircle(cheek.copy(alpha = bl), 3.4f, Offset(64f, 55f))
        eyes(this, p, c, ink)
        brows(this, c, ink)
        mouth(this, c, ink)
        // tape scarf across the lower frame, with ticks
        drawRoundRect(tapeCol, Offset(19f, 66f), Size(62f, 8f), CornerRadius(4f))
        var x = 25f
        while (x < 78f) { drawLine(tickCol, Offset(x, 66f), Offset(x, if (((x - 25f) / 12f) % 1f < 0.01f) 71f else 69f), 1.1f); x += 6f }
        // crop-mark hands
        val arms = (c[F.ARMS] + p.armsExtra).coerceIn(0f, 1f)
        val tr = Offset(80f, 16f - arms * 7f)
        rotate(-p.waveDeg - arms * 22f, tr) { hand(this, tr.x, tr.y, -1f, 1f, frame) }
        val ch = c[F.CHIN]
        val hx = 20f + (37f - 20f) * ch + (12f - 20f) * arms * (1f - ch)
        val hy = 84f + (63f - 84f) * ch + (44f - 84f) * arms * (1f - ch) + c[F.DROOP] * 3f
        hand(this, hx, hy, 1f, -1f, frame)
    }

    private fun eyes(d: DrawScope, p: SnipPose, c: FloatArray, ink: Color) = with(d) {
        val arc = c[F.ARC].coerceIn(0f, 1f)
        for (i in 0..1) {
            val cx = 41f + 18f * i + p.pupilDx
            val cy = 46f + p.pupilDy
            val closing = if (i == 1) max(p.blink, p.wink) else p.blink
            val h = 6f * c[F.EYE_H] * (1f - 0.9f * closing)
            val w = 6f * (1f + 0.3f * (c[F.EYE_H] - 1f).coerceIn(-0.5f, 0.6f))
            if (arc < 0.99f) drawOval(ink.copy(alpha = 1f - arc), Offset(cx - w / 2, cy - h / 2), Size(w, h.coerceAtLeast(1.1f)))
            if (arc > 0.01f) {
                tmp.reset(); tmp.moveTo(cx - 3.6f, 48f); tmp.quadraticTo(cx, 41.5f, cx + 3.6f, 48f)
                drawPath(tmp, ink.copy(alpha = arc), style = stroke3)
            }
        }
    }

    private fun brows(d: DrawScope, c: FloatArray, ink: Color) = with(d) {
        val th = c[F.BROW_TILT] * (PI.toFloat() / 180f)
        val ca = cos(th); val sa = sin(th)
        for (i in 0..1) {
            val s = if (i == 0) 1f else -1f // + = toward the centre of the face
            val bx = 41f + 18f * i
            val by = 38.5f - (if (i == 0) c[F.BROW_Y] else c[F.BROW_YR])
            drawLine(ink, Offset(bx + s * 4.2f * ca, by + 4.2f * sa), Offset(bx - s * 4.2f * ca, by - 4.2f * sa), 2.4f, StrokeCap.Round)
        }
    }

    private fun mouth(d: DrawScope, c: FloatArray, ink: Color) = with(d) {
        val hw = 7f * c[F.MOUTH_W]
        val open = c[F.MOUTH_OPEN].coerceIn(0f, 1f)
        val curve = c[F.MOUTH_CURVE]
        if (open < 0.99f) {
            tmp.reset(); tmp.moveTo(50f - hw, 57f - curve * 1.6f); tmp.quadraticTo(50f, 57f + curve * 6f, 50f + hw, 57f - curve * 1.6f)
            drawPath(tmp, ink.copy(alpha = 1f - open), style = stroke3)
        }
        if (open > 0.01f) {
            val hgt = 3f + 7f * open * (0.65f + 0.35f * max(curve, 0f))
            drawOval(ink.copy(alpha = open), Offset(50f - hw, 55.5f), Size(hw * 2f, hgt))
        }
    }

    private fun extras(d: DrawScope, p: SnipPose, c: FloatArray, frame: Color) = with(d) {
        val t = p.t
        if (c[F.SPARKLE] > 0.02f) {
            val a = c[F.SPARKLE]
            val tw = 0.6f + 0.4f * sin(t * 6f)
            star(this, 88f, 30f, 5f * a * tw, tapeCol); star(this, 13f, 30f, 3.6f * a * (1.2f - tw * 0.5f), tapeCol)
        }
        if (c[F.SWEAT] > 0.02f) {
            val a = c[F.SWEAT]; val y = 33f + 2f * sin(t * 3f)
            tmp.reset(); tmp.moveTo(70f, y - 4f); tmp.quadraticTo(66.5f, y + 1f, 70f, y + 3f); tmp.quadraticTo(73.5f, y + 1f, 70f, y - 4f); tmp.close()
            drawPath(tmp, drop.copy(alpha = a))
        }
        if (c[F.TEAR] > 0.02f) {
            val a = c[F.TEAR]; val y = 52f + (t * 6f) % 8f
            drawCircle(drop.copy(alpha = a * (1f - ((y - 52f) / 8f) * 0.7f)), 1.9f, Offset(41f + p.pupilDx, y))
        }
        if (c[F.ZZ] > 0.02f) {
            val a = c[F.ZZ]
            for (k in 0..1) {
                val ph = ((t * 0.5f + k * 0.5f) % 1f)
                val zx = 82f + 4f * ph + k * 3f; val zy = 22f - 12f * ph - k * 3f; val s = 2.6f + 2.2f * ph + k
                tmp.reset(); tmp.moveTo(zx, zy); tmp.lineTo(zx + s, zy); tmp.lineTo(zx, zy + s); tmp.lineTo(zx + s, zy + s)
                drawPath(tmp, frame.copy(alpha = a * (1f - ph)), style = stroke2)
            }
        }
        if (c[F.BANG] > 0.02f) {
            val a = c[F.BANG]
            drawLine(frame.copy(alpha = a), Offset(89f, 6f), Offset(89f, 17f), 3.2f, StrokeCap.Round); drawCircle(frame.copy(alpha = a), 1.9f, Offset(89f, 22f))
        }
        if (c[F.QMARK] > 0.02f) {
            val a = c[F.QMARK]
            tmp.reset(); tmp.moveTo(83f, 14f); tmp.cubicTo(83f, 6f, 95f, 6f, 95f, 14f); tmp.cubicTo(95f, 19f, 89f, 19f, 89f, 25f)
            drawPath(tmp, frame.copy(alpha = a), style = stroke2); drawCircle(frame.copy(alpha = a), 1.6f, Offset(89f, 30f))
        }
    }

    private fun star(d: DrawScope, x: Float, y: Float, r: Float, col: Color) = with(d) {
        if (r < 0.3f) return@with
        tmp.reset(); tmp.moveTo(x, y - r); tmp.quadraticTo(x, y, x + r, y); tmp.quadraticTo(x, y, x, y + r); tmp.quadraticTo(x, y, x - r, y); tmp.quadraticTo(x, y, x, y - r); tmp.close()
        drawPath(tmp, col)
    }

    /** L-shaped crop mark: corner at ([x],[y]), arms 9 units long along ([sx],0) and (0,[sy]). */
    private fun hand(d: DrawScope, x: Float, y: Float, sx: Float, sy: Float, c: Color) = with(d) {
        tmp.reset(); tmp.moveTo(x + 9f * sx, y); tmp.lineTo(x, y); tmp.lineTo(x, y + 9f * sy)
        drawPath(tmp, c, style = handStroke)
    }
}

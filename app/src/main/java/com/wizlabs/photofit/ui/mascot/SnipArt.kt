package com.wizlabs.photofit.ui.mascot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Snip geometry in a 100×100 unit box (same numbers as store-assets/mascot/snip.svg). Built once per size. */
class SnipArt {
    private val smile = Path().apply { moveTo(43f, 55f); quadraticTo(50f, 61f, 57f, 55f) }
    private val bigSmile = Path().apply { moveTo(41f, 54f); quadraticTo(50f, 65f, 59f, 54f) }
    private val frown = Path().apply { moveTo(43f, 60f); quadraticTo(50f, 54f, 57f, 60f) }
    private val flat = Path().apply { moveTo(44f, 58f); lineTo(56f, 58f) }
    private val tapeTail = Path()

    /** Draws Snip. [u] scales 100 units to the canvas. */
    fun draw(d: DrawScope, p: SnipPose, dark: Boolean) = with(d) {
        val u = size.minDimension / 100f
        val frame = if (dark) Color(0xFFF3F3F6) else Color(0xFF15151A)
        val ink = Color(0xFF15151A)
        val tape = Color(0xFFFFC400)
        val tick = Color(0xFF8F6A00)
        val stroke3 = Stroke(3.2f, cap = StrokeCap.Round)
        scale(u, u, Offset.Zero) {
            val sq = p.squash
            // feet
            drawRoundRect(frame, Offset(33f, 90f), Size(13f, 6f), CornerRadius(3f))
            drawRoundRect(frame, Offset(54f, 90f), Size(13f, 6f), CornerRadius(3f))
            translate(0f, sq * 100f) {
                // tape tail behind the body (trails right, sways / flourishes)
                val lift = -p.flourish * 14f
                val sw = p.tapeSway * 4f
                tapeTail.reset()
                tapeTail.moveTo(74f, 72f)
                tapeTail.cubicTo(84f + sw, 76f + lift, 90f - sw, 84f + lift * 1.4f, 88f + sw * 2f, 94f + lift * 0.6f)
                tapeTail.lineTo(94f + sw * 2f, 93f + lift * 0.6f)
                tapeTail.cubicTo(96f - sw, 82f + lift * 1.4f, 90f + sw, 72f + lift, 80f, 68f)
                tapeTail.close()
                drawPath(tapeTail, tape)
                // body frame, matte, photo
                if (dark) drawRoundRect(Color(0xFF18181D), Offset(20.5f, 16.5f), Size(59f, 63f), CornerRadius(12f))
                drawRoundRect(frame, Offset(22f, 18f), Size(56f, 60f), CornerRadius(10f))
                drawRoundRect(Color.White, Offset(27f, 23f), Size(46f, 50f), CornerRadius(6f))
                drawRoundRect(Color(0xFFBAE6FD), Offset(30f, 26f), Size(40f, 44f), CornerRadius(4f))
                // cheeks
                drawCircle(Color(0x80FDA4AF), 3.4f, Offset(36f, 55f)); drawCircle(Color(0x80FDA4AF), 3.4f, Offset(64f, 55f))
                // eyes
                val ex = 41f + p.pupilDx; val ey = 46f + p.pupilDy
                when (p.eyes) {
                    Eyes.DOTS -> for (cx in floatArrayOf(ex, ex + 18f)) drawOval(ink, Offset(cx - 3f, ey - 3f * p.eyeOpen), Size(6f, 6f * p.eyeOpen))
                    Eyes.HAPPY_ARCS -> for (cx in floatArrayOf(41f, 59f)) {
                        val a = Path().apply { moveTo(cx - 3.5f, 48f); quadraticTo(cx, 42f, cx + 3.5f, 48f) }
                        drawPath(a, ink, style = stroke3)
                    }
                    Eyes.CLOSED -> for (cx in floatArrayOf(41f, 59f)) drawLine(ink, Offset(cx - 3.5f, 47f), Offset(cx + 3.5f, 47f), 2.6f, StrokeCap.Round)
                }
                when (p.mouth) {
                    Mouth.SMILE -> drawPath(smile, ink, style = stroke3)
                    Mouth.BIG_SMILE -> drawPath(bigSmile, ink, style = stroke3)
                    Mouth.FROWN -> drawPath(frown, ink, style = stroke3)
                    Mouth.FLAT -> drawPath(flat, ink, style = stroke3)
                    Mouth.O -> drawCircle(ink, 2.6f, Offset(50f, 58f), style = Stroke(2.4f))
                }
                // tape scarf across the lower frame, with ticks
                drawRoundRect(tape, Offset(19f, 66f), Size(62f, 8f), CornerRadius(4f))
                var x = 25f
                while (x < 78f) { drawLine(tick, Offset(x, 66f), Offset(x, if ((x - 25f) % 12f < 0.1f) 71f else 69f), 1.1f); x += 6f }
                // crop-mark hands: waving top-right, resting (or on chin) bottom-left
                rotate(-p.waveDeg, Offset(80f, 16f)) { hand(this, Offset(80f, 16f), -1f, 1f, frame) }
                if (p.handOnChin) hand(this, Offset(37f, 63f), 1f, -1f, frame) else hand(this, Offset(20f, 84f), 1f, -1f, frame)
            }
        }
    }

    /** L-shaped crop mark: corner at [o], arms 9 units long along ([sx],0) and (0,[sy]). */
    private fun hand(d: DrawScope, o: Offset, sx: Float, sy: Float, c: Color) = with(d) {
        val path = Path().apply { moveTo(o.x + 9f * sx, o.y); lineTo(o.x, o.y); lineTo(o.x, o.y + 9f * sy) }
        drawPath(path, c, style = Stroke(4f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

package com.wizlabs.photofit.ui.mascot

import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipBeat
import com.wizlabs.photofit.engine.SnipBrain
import com.wizlabs.photofit.engine.SnipExpression
import com.wizlabs.photofit.engine.SnipTap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** Indices into the morphing part of the pose. Every emotion is one row of floats; the pose eases toward it. */
object F {
    const val EYE_H = 0      // eye height scale (1 = round, 0.14 = closed line, 1.6 = wide)
    const val ARC = 1        // 0 = round eyes, 1 = happy "^" arcs
    const val BROW_TILT = 2  // degrees; + inner end down (furrowed), - inner end up (worried)
    const val BROW_Y = 3     // left brow lift in units
    const val BROW_YR = 4    // right brow lift in units
    const val MOUTH_CURVE = 5 // -1 frown .. +1 big smile
    const val MOUTH_OPEN = 6  // 0 line .. 1 open
    const val MOUTH_W = 7     // width scale
    const val LEAN = 8       // body tilt in degrees
    const val ARMS = 9       // 0..1 crop-mark hands raised
    const val CHIN = 10      // 0..1 hand on chin
    const val BLUSH = 11
    const val SPARKLE = 12
    const val SWEAT = 13
    const val ZZ = 14
    const val BANG = 15
    const val QMARK = 16
    const val TEAR = 17
    const val DROOP = 18     // frame sags (units)
    const val BOUNCE = 19    // bounce amplitude (units)
    const val N = 20
}

/** Group of features a field belongs to; an emotion must change at least three groups vs neutral. */
enum class FeatureGroup(val fields: IntArray) {
    EYES(intArrayOf(F.EYE_H, F.ARC)),
    BROWS(intArrayOf(F.BROW_TILT, F.BROW_Y, F.BROW_YR)),
    MOUTH(intArrayOf(F.MOUTH_CURVE, F.MOUTH_OPEN, F.MOUTH_W)),
    BODY(intArrayOf(F.LEAN, F.ARMS, F.CHIN, F.DROOP, F.BOUNCE)),
    EXTRAS(intArrayOf(F.BLUSH, F.SPARKLE, F.SWEAT, F.ZZ, F.BANG, F.QMARK, F.TEAR)),
}

object SnipLooks {
    private fun row(eh: Float, arc: Float, bt: Float, by: Float, byr: Float, mc: Float, mo: Float, mw: Float, lean: Float, arms: Float,
                    chin: Float = 0f, blush: Float = 0.3f, spark: Float = 0f, sweat: Float = 0f, zz: Float = 0f, bang: Float = 0f,
                    qm: Float = 0f, tear: Float = 0f, droop: Float = 0f, bounce: Float = 0f) =
        floatArrayOf(eh, arc, bt, by, byr, mc, mo, mw, lean, arms, chin, blush, spark, sweat, zz, bang, qm, tear, droop, bounce)

    private val table: Array<FloatArray> = Array(SnipExpression.entries.size) { i ->
        when (SnipExpression.entries[i]) {
            SnipExpression.NEUTRAL -> row(1f, 0f, 0f, 0f, 0f, 0.5f, 0f, 1f, 0f, 0f)
            SnipExpression.HAPPY -> row(1.05f, 0f, 0f, 1f, 1f, 0.9f, 0f, 1.1f, 2f, 0f, blush = 0.7f)
            SnipExpression.EXCITED -> row(1.25f, 0f, -6f, 3f, 3f, 1f, 1f, 1.3f, 0f, 1f, blush = 0.9f, spark = 1f, bounce = 1f)
            SnipExpression.PROUD -> row(1f, 1f, -4f, 2f, 2f, 0.9f, 0f, 1f, -2f, 0.2f, blush = 0.8f, spark = 0.6f)
            SnipExpression.CURIOUS -> row(1.1f, 0f, 0f, 0.5f, 4.5f, 0.2f, 0f, 0.7f, 5f, 0f, qm = 1f)
            SnipExpression.THINKING -> row(0.85f, 0f, 4f, 0f, 0f, 0f, 0f, 0.8f, -3f, 0f, chin = 1f)
            SnipExpression.FOCUSED -> row(0.55f, 0f, 10f, -1f, -1f, -0.1f, 0f, 0.9f, 0f, 0f, blush = 0f)
            SnipExpression.SURPRISED -> row(1.6f, 0f, -10f, 4f, 4f, 0f, 1f, 0.45f, 0f, 0.3f, bang = 1f)
            SnipExpression.WORRIED -> row(1.1f, 0f, -20f, 1.5f, 1.5f, -0.7f, 0f, 0.9f, -2f, 0f, blush = 0.1f, sweat = 1f)
            SnipExpression.SAD -> row(0.8f, 0f, -24f, 0f, 0f, -1f, 0f, 0.8f, -4f, 0f, blush = 0f, tear = 1f, droop = 1f)
            SnipExpression.SLEEPY -> row(0.14f, 0f, 0f, -1f, -1f, 0.2f, 0.55f, 0.4f, 3f, 0f, blush = 0.4f, zz = 1f, droop = 0.3f)
        }
    }
    fun of(e: SnipExpression): FloatArray = table[e.ordinal]
}

/** Mutable pose, updated in place every drawn frame (no allocation per frame). */
class SnipPose {
    /** Morphing fields, eased toward the current target over ~200 ms. */
    val cur = FloatArray(F.N).also { System.arraycopy(SnipLooks.of(SnipExpression.NEUTRAL), 0, it, 0, F.N) }
    private var tgt: FloatArray = SnipLooks.of(SnipExpression.NEUTRAL)
    private var fastUntil = 0f

    // overlays, set by the driver each frame
    var blink = 0f
    var wink = 0f
    var pupilDx = 0f
    var pupilDy = 0f
    var breathe = 0f
    var bobY = 0f
    var squash = 0f
    var armsExtra = 0f
    var waveDeg = 0f
    var tapeSway = 0f
    var flourish = 0f
    var tapeSpin = 0f
    var sparkPhase = 0f
    var t = 0f

    fun target(e: SnipExpression, nowT: Float) {
        val next = SnipLooks.of(e)
        if (next !== tgt) {
            // a "surprised" pop lands in ~120 ms, everything else morphs over ~200 ms
            if (e == SnipExpression.SURPRISED) fastUntil = nowT + 0.2f
            tgt = next
        }
    }

    fun step(dt: Float, nowT: Float) {
        val tau = if (nowT < fastUntil) 0.03f else 0.07f
        val k = 1f - exp(-dt / tau)
        for (i in 0 until F.N) cur[i] += (tgt[i] - cur[i]) * k
    }

    fun snap(e: SnipExpression) { tgt = SnipLooks.of(e); System.arraycopy(tgt, 0, cur, 0, F.N) }
    fun resetOverlays() { blink = 0f; wink = 0f; pupilDx = 0f; pupilDy = 0f; breathe = 0f; bobY = 0f; squash = 0f; armsExtra = 0f; waveDeg = 0f; tapeSway = 0f; flourish = 0f; tapeSpin = 0f }
}

/**
 * Plays Snip's life: idle loop (blink 3-5 s, breathe, glances, a beat every 8-15 s), one-shot event reactions
 * and tap reactions. Pure maths on [SnipPose]; no Android types, no allocation per frame.
 */
class SnipDriver(seed: Long = System.nanoTime()) {
    private val rnd = java.util.Random(seed)
    private var last = -1f
    private var nextBlink = 1.5f
    private var blinkAt = -10f
    private var nextBeat = 6f
    private var beat = SnipBeat.LOOK_AROUND
    private var beatAt = -10f
    private var beatN = 0
    private var glanceDx = 0f
    private var glanceDy = 0f
    private var nextGlance = 2f
    private var shotE: SnipExpression? = null
    private var shotEnd = -1f
    private var pendingShot: SnipExpression? = null
    private var pendingMs = 0
    private var tapAt = -10f
    private var tapKind = SnipTap.WINK
    private var tapN = 0
    private var pendingTap = false

    fun requestShot(e: SnipExpression?, ms: Int) { if (e != null) { pendingShot = e; pendingMs = ms.coerceAtMost(SnipBrain.MAX_SHOT_MS) } }
    /** Returns the tap reaction that will play (so the caller can add haptics). */
    fun tap(): SnipTap { tapKind = SnipBrain.tapReaction(tapN++); pendingTap = true; return tapKind }

    /** One animation frame at clock time [t] seconds. [lookDown] makes glances go toward the search field. */
    fun frame(p: SnipPose, t: Float, expression: SnipExpression, action: SnipAction, lookDown: Boolean) {
        val dt = if (last < 0f) 0.066f else (t - last).coerceIn(0f, 0.25f)
        last = t
        p.t = t
        pendingShot?.let { shotE = it; shotEnd = t + pendingMs / 1000f; pendingShot = null }
        if (pendingTap) { tapAt = t; pendingTap = false }
        var e = if (shotE != null && t < shotEnd) shotE!! else expression
        val tu = t - tapAt
        val tapping = tu in 0f..0.9f
        if (tapping) when (tapKind) { SnipTap.WINK, SnipTap.GIGGLE, SnipTap.TAPE_SPIN, SnipTap.WAVE -> if (e == SnipExpression.NEUTRAL || e == SnipExpression.SLEEPY) e = SnipExpression.HAPPY }
        p.target(e, t)
        p.step(dt, t)

        // blink: 130 ms every 3-5 s (not while eyes are already closed by an emotion)
        if (t >= nextBlink) { blinkAt = t; nextBlink = t + SnipBrain.blinkGap(rnd.nextFloat()) }
        val bu = (t - blinkAt) / 0.13f
        p.blink = if (bu in 0f..1f) sin(bu * PI.toFloat()) else 0f
        // breathe ±1.5 % and a gentle bob of ±1.5 units
        p.breathe = 0.015f * sin(t * 2f * PI.toFloat() / 3.4f)
        p.bobY = 1.5f * sin(t * 2f * PI.toFloat() / 4.1f)
        // glances: ease toward a random target; on Home some go toward the search field
        if (t >= nextGlance) {
            nextGlance = t + 2.2f + rnd.nextFloat() * 3f
            val r = rnd.nextFloat()
            if (lookDown && r < 0.5f) { glanceDx = -1.6f; glanceDy = 1.5f } else if (r < 0.75f) { glanceDx = 0f; glanceDy = 0f } else { glanceDx = if (rnd.nextBoolean()) 1.8f else -1.8f; glanceDy = 0f }
        }
        val gk = 1f - exp(-dt / 0.12f)
        p.pupilDx += (glanceDx - p.pupilDx) * gk
        p.pupilDy += (glanceDy - p.pupilDy) * gk
        // emotion-specific eye bias
        if (e == SnipExpression.THINKING) { p.pupilDy = -1.5f; p.pupilDx = 1.2f }
        if (e == SnipExpression.WORRIED || e == SnipExpression.SAD) p.pupilDy = 1f
        if (e == SnipExpression.SLEEPY || e == SnipExpression.FOCUSED) { p.pupilDx = 0f; if (e == SnipExpression.SLEEPY) p.pupilDy = 0f }
        p.sparkPhase = t * 6f

        // idle beat every 8-15 s
        if (t >= nextBeat) { beat = SnipBrain.beat(beatN++); beatAt = t; nextBeat = t + SnipBrain.beatGap(rnd.nextFloat()) }
        val su = t - beatAt
        p.armsExtra = 0f; p.tapeSway = 0f; p.squash = 0f; p.waveDeg = 0f; p.flourish = 0f; p.tapeSpin = 0f; p.wink = 0f
        when (beat) {
            SnipBeat.LOOK_AROUND -> if (su in 0f..1.6f) p.pupilDx = sin(su / 1.6f * 2f * PI.toFloat()) * 2.6f
            SnipBeat.STRETCH -> if (su in 0f..0.9f) { val b = sin(su / 0.9f * PI.toFloat()); p.armsExtra = b * 0.8f; p.squash = -0.02f * b }
            SnipBeat.TAPE_ADJUST -> if (su in 0f..1.2f) p.tapeSway = sin(su / 1.2f * 2f * PI.toFloat())
        }
        // actions from the app
        when (action) {
            SnipAction.WAVE -> p.waveDeg = 18f + sin(t * 7f) * 16f
            SnipAction.TAPE_SWAY -> p.tapeSway = sin(t * 3.2f)
            SnipAction.FLOURISH -> { val fu = t % 4f; if (fu < 0.42f) p.flourish = sin(fu / 0.42f * PI.toFloat()) }
            SnipAction.LOOK_SIDE -> p.pupilDx = if ((t % 3.2f) < 1.6f) sin((t % 3.2f) / 1.6f * 2f * PI.toFloat()) * 2.4f else 0f
            SnipAction.NONE -> {}
        }
        // tap reactions, ≤900 ms
        if (tapping) {
            val u = tu / 0.9f
            val bump = sin(u * PI.toFloat())
            when (tapKind) {
                SnipTap.WINK -> p.wink = bump
                SnipTap.GIGGLE -> { p.squash = 0.04f * sin(u * 6f * PI.toFloat()) * (1f - u); p.bobY -= 3f * bump }
                SnipTap.TAPE_SPIN -> { p.tapeSpin = u * 360f; p.flourish = bump * 0.7f }
                SnipTap.WAVE -> p.waveDeg = 18f + sin(tu * 14f) * 18f
            }
        }
    }

    /** Still frame for reduce-motion / Battery Saver / hidden: the current emotion, no loops. */
    fun still(p: SnipPose, expression: SnipExpression) { p.snap(expression); p.resetOverlays(); p.pupilDy = if (expression == SnipExpression.THINKING) -1.5f else 0f }

    companion object {
        /** Number of features groups in which [e] differs from NEUTRAL by at least [eps] (testable). */
        fun changedGroups(e: SnipExpression, eps: Float = 0.35f): Int {
            val a = SnipLooks.of(e); val b = SnipLooks.of(SnipExpression.NEUTRAL)
            return FeatureGroup.entries.count { g -> g.fields.any { abs(a[it] - b[it]) >= eps } }
        }
    }
}

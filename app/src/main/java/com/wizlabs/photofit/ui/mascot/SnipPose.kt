package com.wizlabs.photofit.ui.mascot

import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipExpression
import kotlin.math.PI
import kotlin.math.sin

enum class Mouth { SMILE, BIG_SMILE, FLAT, FROWN, O }
enum class Eyes { DOTS, HAPPY_ARCS, CLOSED }

/** Mutable pose, updated in place every drawn frame (no allocation per frame). */
class SnipPose {
    var eyes = Eyes.DOTS
    var mouth = Mouth.SMILE
    var eyeOpen = 1f
    var pupilDx = 0f
    var pupilDy = 0f
    /** Degrees the waving hand lifts (0 = rest). */
    var waveDeg = 0f
    var handOnChin = false
    var tapeSway = 0f
    /** 0..1 flourish lift of the tape tail. */
    var flourish = 0f
    var squash = 0f

    /** Sets every field for [expression] + [action] at animation time [t] seconds. [still] freezes loops. */
    fun update(expression: SnipExpression, action: SnipAction, t: Float, still: Boolean) {
        val tt = if (still) 0f else t
        eyes = when (expression) { SnipExpression.PROUD -> Eyes.HAPPY_ARCS; SnipExpression.SLEEPY -> Eyes.CLOSED; else -> Eyes.DOTS }
        mouth = when (expression) {
            SnipExpression.HAPPY, SnipExpression.PROUD -> Mouth.BIG_SMILE
            SnipExpression.NEUTRAL -> Mouth.SMILE
            SnipExpression.WORRIED -> Mouth.FROWN
            SnipExpression.SLEEPY -> Mouth.O
            else -> Mouth.FLAT
        }
        // blink for 120 ms every 3.4 s
        eyeOpen = if (still || expression == SnipExpression.SLEEPY) 1f else if (tt % 3.4f > 3.28f) 0.1f else 1f
        pupilDx = 0f; pupilDy = if (expression == SnipExpression.THINKING) -1.6f else 0f
        if (expression == SnipExpression.SEARCHING) pupilDx = if (still) -2f else (if (tt < 1.6f) sin(tt / 1.6f * 2 * PI).toFloat() * 2.4f else 0f)
        if (expression == SnipExpression.WORRIED) pupilDy = 1f
        handOnChin = expression == SnipExpression.THINKING
        waveDeg = if (action == SnipAction.WAVE) (if (still) 18f else 18f + sin(tt * 7f) * 16f) else 0f
        tapeSway = if (action == SnipAction.TAPE_SWAY) (if (still) 0f else sin(tt * 3.2f)) else 0f
        flourish = if (action == SnipAction.FLOURISH) (if (still) 0f else flourishAt(tt)) else 0f
        squash = if (expression == SnipExpression.SLEEPY) (if (still) 0f else sin(tt * 1.6f) * 0.015f) else 0f
    }

    /** One-shot ≤420 ms swing: up, then settle. */
    private fun flourishAt(t: Float): Float = if (t >= 0.42f) 0f else sin(t / 0.42f * PI.toFloat())
}

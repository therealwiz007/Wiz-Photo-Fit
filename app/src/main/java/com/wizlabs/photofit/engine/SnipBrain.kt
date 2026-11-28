package com.wizlabs.photofit.engine

/** Snip's emotions (10 + neutral). Pure Kotlin, no Android types, so the mapping below is unit-tested. */
enum class SnipExpression { NEUTRAL, HAPPY, EXCITED, PROUD, CURIOUS, THINKING, FOCUSED, SURPRISED, WORRIED, SAD, SLEEPY }

/** What Snip does while showing the face. */
enum class SnipAction { NONE, WAVE, LOOK_SIDE, TAPE_SWAY, FLOURISH }

/** Delight reactions to a tap, played in rotation so two taps in a row never match. */
enum class SnipTap { WINK, GIGGLE, TAPE_SPIN, WAVE }

/** Small idle beats, one every 8-15 s. */
enum class SnipBeat { LOOK_AROUND, STRETCH, TAPE_ADJUST }

/** App events Snip reacts to (handoff §13A table). */
sealed interface SnipEvent {
    data object FirstRun : SnipEvent
    data class SearchFound(val count: Int) : SnipEvent
    data object SearchNoResults : SnipEvent
    data class ModelDownloading(val percent: Int) : SnipEvent
    data object ModelUnavailable : SnipEvent
    data class AllChecksPass(val widthPx: Int, val heightPx: Int, val kb: Int) : SnipEvent
    data class ChecksFail(val failed: Int) : SnipEvent
    data object Saved : SnipEvent
    data object SavedEmpty : SnipEvent
    data class Clock(val hour: Int) : SnipEvent
}

/**
 * [expression] is the idle emotion Snip settles into. [shot], when set, plays first for [shotMs] (≤600 ms)
 * and then settles into [expression].
 */
data class SnipMood(
    val expression: SnipExpression, val action: SnipAction, val line: String,
    val shot: SnipExpression? = null, val shotMs: Int = 0,
)

object SnipBrain {
    /** Longest speech line Snip may use. */
    const val MAX_LINE = 50
    const val MAX_SHOT_MS = 600

    fun isLateNight(hour: Int) = hour >= 23 || hour < 5

    fun react(e: SnipEvent): SnipMood = when (e) {
        SnipEvent.FirstRun -> SnipMood(SnipExpression.HAPPY, SnipAction.WAVE, "Hi, I'm Snip. I make photos fit.")
        is SnipEvent.SearchFound -> SnipMood(SnipExpression.HAPPY, SnipAction.NONE,
            if (e.count == 1) "Found it." else "Found ${e.count.coerceAtMost(99)} matches.", SnipExpression.CURIOUS, 450)
        SnipEvent.SearchNoResults -> SnipMood(SnipExpression.THINKING, SnipAction.LOOK_SIDE, "Try a custom size.")
        is SnipEvent.ModelDownloading -> SnipMood(SnipExpression.FOCUSED, SnipAction.TAPE_SWAY,
            "Getting the background tool… ${e.percent.coerceIn(0, 100)}%")
        SnipEvent.ModelUnavailable -> SnipMood(SnipExpression.WORRIED, SnipAction.NONE, "Background tool needs Google Play services.")
        is SnipEvent.AllChecksPass -> SnipMood(SnipExpression.PROUD, SnipAction.FLOURISH, "Fits: ${e.widthPx}×${e.heightPx} px · ${e.kb} KB.")
        is SnipEvent.ChecksFail -> SnipMood(SnipExpression.WORRIED, SnipAction.NONE,
            if (e.failed == 1) "1 check needs a fix. Easy to sort." else "${e.failed} checks need a fix. Easy to sort.")
        SnipEvent.Saved -> SnipMood(SnipExpression.HAPPY, SnipAction.NONE, "Saved.", SnipExpression.EXCITED, 600)
        SnipEvent.SavedEmpty -> SnipMood(SnipExpression.SLEEPY, SnipAction.NONE, "Saved photos appear here.")
        is SnipEvent.Clock -> if (isLateNight(e.hour)) SnipMood(SnipExpression.SLEEPY, SnipAction.NONE, "Late night? Take your time.")
            else SnipMood(SnipExpression.HAPPY, SnipAction.NONE, "")
    }

    private val tapPool = SnipTap.entries
    private val beatPool = SnipBeat.entries

    /** Tap number [n] (0-based) → reaction; consecutive taps always differ. */
    fun tapReaction(n: Int): SnipTap = tapPool[Math.floorMod(n, tapPool.size)]
    fun beat(n: Int): SnipBeat = beatPool[Math.floorMod(n, beatPool.size)]

    /** Seconds until the next blink, 3-5 s, for a random [r] in 0..1. */
    fun blinkGap(r: Float): Float = 3f + 2f * r.coerceIn(0f, 1f)
    /** Seconds until the next idle beat, 8-15 s. */
    fun beatGap(r: Float): Float = 8f + 7f * r.coerceIn(0f, 1f)
}

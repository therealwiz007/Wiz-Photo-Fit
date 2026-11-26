package com.wizlabs.photofit.engine

/** Snip's faces. Pure Kotlin, no Android types, so the mapping below is unit-tested. */
enum class SnipExpression { NEUTRAL, HAPPY, PROUD, THINKING, SEARCHING, SLEEPY, WORRIED }

/** What Snip does while showing the face. */
enum class SnipAction { NONE, WAVE, LOOK_SIDE, TAPE_SWAY, FLOURISH }

/** App events Snip reacts to (handoff §14). */
sealed interface SnipEvent {
    data object FirstRun : SnipEvent
    data object SearchNoResults : SnipEvent
    data class ModelDownloading(val percent: Int) : SnipEvent
    data object ModelUnavailable : SnipEvent
    data class AllChecksPass(val widthPx: Int, val heightPx: Int, val kb: Int) : SnipEvent
    data class ChecksFail(val failed: Int) : SnipEvent
    data object SavedEmpty : SnipEvent
}

data class SnipMood(val expression: SnipExpression, val action: SnipAction, val line: String)

object SnipBrain {
    /** Longest speech line Snip may use. */
    const val MAX_LINE = 50

    fun react(e: SnipEvent): SnipMood = when (e) {
        SnipEvent.FirstRun -> SnipMood(SnipExpression.HAPPY, SnipAction.WAVE, "Hi, I'm Snip. I make photos fit.")
        SnipEvent.SearchNoResults -> SnipMood(SnipExpression.SEARCHING, SnipAction.LOOK_SIDE, "Try a custom size.")
        is SnipEvent.ModelDownloading -> SnipMood(SnipExpression.THINKING, SnipAction.TAPE_SWAY,
            "Getting the background tool… ${e.percent.coerceIn(0, 100)}%")
        SnipEvent.ModelUnavailable -> SnipMood(SnipExpression.WORRIED, SnipAction.NONE, "Background tool needs Google Play services.")
        is SnipEvent.AllChecksPass -> SnipMood(SnipExpression.PROUD, SnipAction.FLOURISH, "Fits: ${e.widthPx}×${e.heightPx} px · ${e.kb} KB.")
        is SnipEvent.ChecksFail -> SnipMood(SnipExpression.THINKING, SnipAction.NONE,
            if (e.failed == 1) "1 check needs a fix." else "${e.failed} checks need a fix.")
        SnipEvent.SavedEmpty -> SnipMood(SnipExpression.SLEEPY, SnipAction.NONE, "Saved photos appear here.")
    }
}

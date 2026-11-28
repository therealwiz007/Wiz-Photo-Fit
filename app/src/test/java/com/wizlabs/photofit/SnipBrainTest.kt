package com.wizlabs.photofit

import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipBrain
import com.wizlabs.photofit.engine.SnipEvent
import com.wizlabs.photofit.engine.SnipExpression
import com.wizlabs.photofit.engine.SnipTap
import com.wizlabs.photofit.ui.mascot.F
import com.wizlabs.photofit.ui.mascot.SnipDriver
import com.wizlabs.photofit.ui.mascot.SnipLooks
import com.wizlabs.photofit.ui.mascot.SnipPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Handoff §13A: every row of the event table, the 10 emotions, tap pool, idle timing and the pose morph. */
class SnipBrainTest {
    private fun r(e: SnipEvent) = SnipBrain.react(e)
    private val tenEmotions = listOf(SnipExpression.HAPPY, SnipExpression.EXCITED, SnipExpression.PROUD, SnipExpression.CURIOUS, SnipExpression.THINKING,
        SnipExpression.FOCUSED, SnipExpression.SURPRISED, SnipExpression.WORRIED, SnipExpression.SAD, SnipExpression.SLEEPY)

    @Test fun firstRun() = r(SnipEvent.FirstRun).let { assertEquals(SnipExpression.HAPPY, it.expression); assertEquals(SnipAction.WAVE, it.action) }
    @Test fun searchFoundIsCuriousThenHappy() = r(SnipEvent.SearchFound(3)).let {
        assertEquals(SnipExpression.CURIOUS, it.shot); assertEquals(SnipExpression.HAPPY, it.expression); assertTrue(it.shotMs in 1..SnipBrain.MAX_SHOT_MS); assertEquals("Found 3 matches.", it.line) }
    @Test fun noResultsIsThinking() = r(SnipEvent.SearchNoResults).let { assertEquals(SnipExpression.THINKING, it.expression); assertEquals("Try a custom size.", it.line) }
    @Test fun downloadingIsFocused() = r(SnipEvent.ModelDownloading(42)).let { assertEquals(SnipExpression.FOCUSED, it.expression); assertEquals(SnipAction.TAPE_SWAY, it.action); assertEquals("Getting the background tool… 42%", it.line) }
    @Test fun downloadingClamps() = assertTrue(r(SnipEvent.ModelDownloading(180)).line.endsWith("100%"))
    @Test fun unavailableIsWorried() = r(SnipEvent.ModelUnavailable).let { assertEquals(SnipExpression.WORRIED, it.expression); assertEquals("Background tool needs Google Play services.", it.line) }
    @Test fun passIsProudWithFlourish() = r(SnipEvent.AllChecksPass(200, 230, 42)).let { assertEquals(SnipExpression.PROUD, it.expression); assertEquals(SnipAction.FLOURISH, it.action); assertEquals("Fits: 200×230 px · 42 KB.", it.line) }
    @Test fun failIsWorriedAndKind() { assertEquals(SnipExpression.WORRIED, r(SnipEvent.ChecksFail(2)).expression); assertTrue(r(SnipEvent.ChecksFail(1)).line.startsWith("1 check needs a fix.")); assertTrue(r(SnipEvent.ChecksFail(3)).line.startsWith("3 checks need a fix.")) }
    @Test fun savedIsExcitedOneShot() = r(SnipEvent.Saved).let { assertEquals(SnipExpression.EXCITED, it.shot); assertEquals(SnipExpression.HAPPY, it.expression); assertTrue(it.shotMs <= SnipBrain.MAX_SHOT_MS) }
    @Test fun savedEmptyIsSleepy() = r(SnipEvent.SavedEmpty).let { assertEquals(SnipExpression.SLEEPY, it.expression); assertEquals("Saved photos appear here.", it.line) }
    @Test fun lateNightIsSleepy() {
        for (h in listOf(23, 0, 3, 4)) assertEquals("hour $h", SnipExpression.SLEEPY, r(SnipEvent.Clock(h)).expression)
        for (h in listOf(5, 12, 22)) assertEquals("hour $h", SnipExpression.HAPPY, r(SnipEvent.Clock(h)).expression)
    }
    @Test fun linesFitBudget() {
        listOf(SnipEvent.FirstRun, SnipEvent.SearchFound(120), SnipEvent.SearchNoResults, SnipEvent.ModelDownloading(100), SnipEvent.ModelUnavailable, SnipEvent.AllChecksPass(1000, 1000, 500),
            SnipEvent.ChecksFail(12), SnipEvent.Saved, SnipEvent.SavedEmpty, SnipEvent.Clock(2)).forEach { assertTrue(r(it).line, r(it).line.length <= SnipBrain.MAX_LINE) }
    }

    @Test fun tapPoolHasFourAndNeverRepeatsInARow() {
        assertEquals(setOf(SnipTap.WINK, SnipTap.GIGGLE, SnipTap.TAPE_SPIN, SnipTap.WAVE), (0 until 4).map { SnipBrain.tapReaction(it) }.toSet())
        for (n in 0 until 20) assertNotEquals(SnipBrain.tapReaction(n), SnipBrain.tapReaction(n + 1))
    }
    @Test fun idleTiming() {
        assertEquals(3f, SnipBrain.blinkGap(0f)); assertEquals(5f, SnipBrain.blinkGap(1f))
        assertEquals(8f, SnipBrain.beatGap(0f)); assertEquals(15f, SnipBrain.beatGap(1f))
    }

    @Test fun tenEmotionsEachChangeAtLeastThreeFeatureGroups() {
        assertEquals(10, tenEmotions.size)
        for (e in tenEmotions) assertTrue("$e changes only ${SnipDriver.changedGroups(e)} groups", SnipDriver.changedGroups(e) >= 3)
    }
    @Test fun emotionsAreAllDistinct() {
        val rows = SnipExpression.entries.map { SnipLooks.of(it).toList() }
        assertEquals(rows.size, rows.toSet().size)
    }
    @Test fun morphIsSmoothNotACut() {
        val p = SnipPose(); p.snap(SnipExpression.HAPPY)
        p.target(SnipExpression.SAD, 0f)
        p.step(0.033f, 0.033f)
        val a = SnipLooks.of(SnipExpression.HAPPY)[F.MOUTH_CURVE]; val b = SnipLooks.of(SnipExpression.SAD)[F.MOUTH_CURVE]
        assertTrue(p.cur[F.MOUTH_CURVE] < a && p.cur[F.MOUTH_CURVE] > b)       // part way after one 15-30 fps frame
        repeat(12) { p.step(0.033f, 0.033f * (it + 2)) }                          // ~400 ms later it has arrived
        assertEquals(b, p.cur[F.MOUTH_CURVE], 0.05f)
    }
    @Test fun surprisedPopsFast() {
        val p = SnipPose(); p.target(SnipExpression.SURPRISED, 0f)
        repeat(4) { p.step(0.033f, 0.033f * (it + 1)) }                           // 132 ms
        assertTrue(p.cur[F.EYE_H] > 1.5f)
    }
    @Test fun driverRunsForMinutesWithoutStoppingOrNaN() {
        val d = SnipDriver(1L); val p = SnipPose()
        var blinked = false; var t = 0f; var moved = false
        while (t < 120f) {
            d.frame(p, t, SnipExpression.HAPPY, SnipAction.NONE, true)
            if (p.blink > 0.5f) blinked = true
            if (p.bobY != 0f) moved = true
            for (v in p.cur) assertTrue(!v.isNaN())
            t += 1f / 15f
        }
        assertTrue(blinked && moved)
    }
    @Test fun oneShotPlaysThenSettles() {
        val d = SnipDriver(2L); val p = SnipPose()
        d.frame(p, 0f, SnipExpression.HAPPY, SnipAction.NONE, false)
        d.requestShot(SnipExpression.CURIOUS, 600)
        var t = 0.066f
        repeat(8) { d.frame(p, t, SnipExpression.HAPPY, SnipAction.NONE, false); t += 0.066f }   // ~530 ms: still curious
        assertTrue(p.cur[F.QMARK] > 0.5f)
        repeat(30) { d.frame(p, t, SnipExpression.HAPPY, SnipAction.NONE, false); t += 0.066f }  // 2 s later: settled
        assertTrue(p.cur[F.QMARK] < 0.05f)
    }
    @Test fun tapPlaysAndEndsWithin900ms() {
        val d = SnipDriver(3L); val p = SnipPose()
        d.frame(p, 0f, SnipExpression.HAPPY, SnipAction.NONE, false)
        assertEquals(SnipTap.WINK, d.tap())
        d.frame(p, 0.066f, SnipExpression.HAPPY, SnipAction.NONE, false)
        d.frame(p, 0.3f, SnipExpression.HAPPY, SnipAction.NONE, false)
        assertTrue(p.wink > 0.5f)
        d.frame(p, 1.2f, SnipExpression.HAPPY, SnipAction.NONE, false)
        assertEquals(0f, p.wink)
    }
    @Test fun stillFrameShowsTheCurrentEmotion() {
        val p = SnipPose(); SnipDriver(4L).still(p, SnipExpression.SLEEPY)
        assertEquals(SnipLooks.of(SnipExpression.SLEEPY)[F.EYE_H], p.cur[F.EYE_H], 0.001f)
        assertEquals(0f, p.blink)
    }
}

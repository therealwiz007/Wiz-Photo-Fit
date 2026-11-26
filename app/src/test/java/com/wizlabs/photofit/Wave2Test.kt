package com.wizlabs.photofit

import com.wizlabs.photofit.data.PresetJson
import com.wizlabs.photofit.data.SearchEntry
import com.wizlabs.photofit.data.SearchIndex
import com.wizlabs.photofit.engine.BgRule
import com.wizlabs.photofit.engine.Compliance
import com.wizlabs.photofit.engine.ComplianceSpec
import com.wizlabs.photofit.engine.Measured
import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipBrain
import com.wizlabs.photofit.engine.SnipEvent
import com.wizlabs.photofit.engine.SnipExpression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PresetCountTest {
    private fun bundle() = PresetJson.parseBundle(listOf("src/main/assets/presets.json", "app/src/main/assets/presets.json").map(::File).first { it.exists() }.readText())

    @Test fun thirtySixPresetsThirteenGroups() {
        val b = bundle()
        assertEquals(36, b.presets.size)
        assertEquals(13, b.groups.size)
    }

    /** Fingerprint of every preset id, part size and KB range: any accidental change to preset values fails this. */
    @Test fun dimensionChecksumUnchanged() {
        val s = bundle().presets.joinToString("|") { p ->
            p.id + ":" + p.parts.joinToString(",") { val t = it.size.targetPx(); "${t.w}x${t.h}/${it.kbMin}-${it.kbMax}" }
        }
        assertEquals(EXPECTED, s.hashCode().toString() + ":" + s.length)
    }

    companion object { const val EXPECTED = "1985948845:1513" }
}

class SearchIndexTest {
    private val entries = listOf(
        SearchEntry("jnvst", "Navodaya JNVST (Class 6 / 9)", "School"), SearchEntry("neet", "NEET UG", "Medical"),
        SearchEntry("neetpg", "NEET PG / NEET MDS (NBEMS)", "Medical"), SearchEntry("ssc", "SSC CGL / CHSL / MTS / Steno", "SSC"),
        SearchEntry("sscgd", "SSC GD Constable", "SSC"), SearchEntry("pan1", "PAN card (NSDL / Protean)", "Documents"),
        SearchEntry("id-us", "USA visa / passport (2 × 2 in)", "ID / Passport"), SearchEntry("id-in", "India passport / OCI / visa (35 × 45 mm)", "ID / Passport"),
        SearchEntry("upsc", "UPSC CSE / NDA / CDS / CAPF (OTR)", "UPSC"),
    )
    private fun top3(q: String) = SearchIndex.search(entries, q).take(3).map { it.id }

    @Test fun ssc() = assertTrue("ssc" in top3("ssc") && "sscgd" in top3("ssc"))
    @Test fun neet() = assertEquals("neet", top3("neet").first())
    @Test fun passport() = assertTrue(top3("passport").containsAll(listOf("id-us", "id-in")))
    @Test fun pan() = assertEquals("pan1", top3("pan").first())
    @Test fun blankKeepsAll() = assertEquals(entries.size, SearchIndex.search(entries, "  ").size)
    @Test fun noMatch() = assertTrue(SearchIndex.search(entries, "zzzz").isEmpty())
    @Test fun prefixBeatsContains() {
        val e = listOf(SearchEntry("a", "Xpassport"), SearchEntry("b", "Passport size"))
        assertEquals("b", SearchIndex.search(e, "pass").first().id)
    }
}

class ComplianceTest {
    private val spec = ComplianceSpec(200, 230, 20, 50, "jpg", dpi = 200, background = BgRule.WHITE)
    private fun ok() = Measured(200, 230, 30_000, "jpg", 200, cornerRgb = 0xFFFFFF)

    @Test fun passCase() {
        val c = Compliance.evaluate(spec, ok())
        assertTrue(c.all { it.pass }); assertEquals(0, Compliance.failed(c)); assertEquals(5, c.size)
    }
    @Test fun failingCustomCase() {
        val c = Compliance.evaluate(spec, ok().copy(widthPx = 199, bytes = 60_000, cornerRgb = 0x808080))
        assertEquals(setOf("Dimensions", "File size", "Background"), c.filter { !it.pass }.map { it.name }.toSet())
        assertEquals("Tap Smaller.", c.first { it.name == "File size" }.fix)
    }
    @Test fun tooSmallSaysBigger() = assertEquals("Tap Bigger.", Compliance.evaluate(spec, ok().copy(bytes = 10_000)).first { it.name == "File size" }.fix)
    @Test fun kbEdges() {
        assertTrue(Compliance.evaluate(spec, ok().copy(bytes = 50_000)).first { it.name == "File size" }.pass)
        assertFalse(Compliance.evaluate(spec, ok().copy(bytes = 50_001)).first { it.name == "File size" }.pass)
        assertTrue(Compliance.evaluate(spec, ok().copy(bytes = 20 * 1024)).first { it.name == "File size" }.pass)
    }
    @Test fun noOptionalRowsWhenNotRequired() = assertEquals(3, Compliance.evaluate(ComplianceSpec(1, 1, null, 10, "jpg"), Measured(1, 1, 100, "jpg", 72)).size)
    @Test fun faceRowOnlyWhenMeasured() {
        val s = ComplianceSpec(413, 531, null, null, "jpg", headRatio = 0.7f)
        val m = Measured(413, 531, 1000, "jpg", 300)
        assertTrue(Compliance.evaluate(s, m).none { it.name == "Face size" })
        assertFalse(Compliance.evaluate(s, m.copy(headRatio = 0.4f)).first { it.name == "Face size" }.pass)
    }
}

class SnipBrainTest {
    private fun r(e: SnipEvent) = SnipBrain.react(e)
    @Test fun firstRun() = r(SnipEvent.FirstRun).let { assertEquals(SnipExpression.HAPPY, it.expression); assertEquals(SnipAction.WAVE, it.action) }
    @Test fun noResults() = r(SnipEvent.SearchNoResults).let { assertEquals(SnipExpression.SEARCHING, it.expression); assertEquals(SnipAction.LOOK_SIDE, it.action); assertEquals("Try a custom size.", it.line) }
    @Test fun downloading() = r(SnipEvent.ModelDownloading(42)).let { assertEquals(SnipExpression.THINKING, it.expression); assertEquals(SnipAction.TAPE_SWAY, it.action); assertEquals("Getting the background tool… 42%", it.line) }
    @Test fun downloadingClamps() = assertTrue(r(SnipEvent.ModelDownloading(180)).line.endsWith("100%"))
    @Test fun unavailable() = r(SnipEvent.ModelUnavailable).let { assertEquals(SnipExpression.WORRIED, it.expression); assertEquals(SnipAction.NONE, it.action); assertEquals("Background tool needs Google Play services.", it.line) }
    @Test fun pass() = r(SnipEvent.AllChecksPass(200, 230, 42)).let { assertEquals(SnipExpression.PROUD, it.expression); assertEquals(SnipAction.FLOURISH, it.action); assertEquals("Fits: 200×230 px · 42 KB.", it.line) }
    @Test fun fail() { assertEquals("1 check needs a fix.", r(SnipEvent.ChecksFail(1)).line); assertEquals("3 checks need a fix.", r(SnipEvent.ChecksFail(3)).line); assertEquals(SnipExpression.THINKING, r(SnipEvent.ChecksFail(2)).expression) }
    @Test fun savedEmpty() = r(SnipEvent.SavedEmpty).let { assertEquals(SnipExpression.SLEEPY, it.expression); assertEquals(SnipAction.NONE, it.action); assertEquals("Saved photos appear here.", it.line) }
    @Test fun linesFitBudget() {
        listOf(SnipEvent.FirstRun, SnipEvent.SearchNoResults, SnipEvent.ModelDownloading(100), SnipEvent.ModelUnavailable, SnipEvent.AllChecksPass(1000, 1000, 500), SnipEvent.ChecksFail(12), SnipEvent.SavedEmpty)
            .forEach { assertTrue(r(it).line, r(it).line.length <= SnipBrain.MAX_LINE) }
    }
}

package com.wizlabs.photofit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsSetupTest {
    @Test fun adsAreOff() {
        assertFalse(AdsSetup.ENABLED)
        assertFalse(AdsSetup.config().enabled)
    }

    @Test fun fiveSlotsAsPlanned() {
        assertEquals(listOf("result", "exam_list", "about", "result_exit", "adfree"), AdsSetup.slots.map { it.id })
    }

    @Test fun noSlotSitsOnANoAdScreen() {
        for (s in AdsSetup.slots) assertFalse("${s.id} on ${s.screen}", s.screen in AdsSetup.noAdScreens)
    }

    @Test fun everyNoAdScreenExists() {
        assertTrue(AdsSetup.allScreens.containsAll(AdsSetup.noAdScreens))
        assertTrue(AdsSetup.allScreens.containsAll(AdsSetup.adScreens))
        assertEquals(11, AdsSetup.noAdScreens.size)
    }

    @Test fun debugGuardThrowsOnNoAdScreen() {
        for (screen in AdsSetup.noAdScreens) assertThrows(IllegalStateException::class.java) { AdsSetup.guard(screen, debug = true) }
        AdsSetup.guard(AdsSetup.RESULT, debug = true)   // allowed screen: no throw
        AdsSetup.guard(AdsSetup.EDITOR, debug = false)  // release: no throw
    }

    @Test fun resultExitRunsThenAtOnce() {
        var n = 0
        AdsSetup.resultExit(saved = true) { n++ }
        AdsSetup.resultExit(saved = false) { n++ }
        assertEquals(2, n)
    }
}

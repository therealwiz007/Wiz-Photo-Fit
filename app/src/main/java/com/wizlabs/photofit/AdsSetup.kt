package com.wizlabs.photofit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.wizlabs.ads.AdPalette
import com.wizlabs.ads.AdsConfig
import com.wizlabs.ads.compose.WizBannerAd
import com.wizlabs.ads.compose.WizNativeAdCard
import com.wizlabs.ads.compose.showInterstitialAtBreak
import com.wizlabs.photofit.ui.components.Wiz

/**
 * Photo Fit ad slots (handoff 16A). Ads are OFF and SDK-free in this build (wiz-ads 0.3 --no-sdk):
 * every slot renders nothing, takes zero height and emits no layout node. The later AdMob release
 * re-syncs wiz-ads without --no-sdk, adds the SDK, and flips [ENABLED].
 */
object AdsSetup {
    const val ENABLED = false

    // Screen names (ad-relevant view of the app's screens)
    const val HOME = "home"
    const val HOME_SEARCH = "home_search"
    const val EXAM_LIST = "exams"
    const val EXAM_DETAIL = "exam_detail"
    const val EDITOR = "editor"
    const val MODEL_CARD = "model_card"
    const val RESULT = "result"
    const val PRINT = "print"
    const val BUILDER = "builder"
    const val IMPORT = "import"
    const val COMPRESS = "compress"
    const val COMPRESS_RUNNING = "compress_running"
    const val JOINER = "joiner"
    const val SAVED = "saved"
    const val ABOUT = "about"
    const val SHARE_SHEET = "share_sheet"
    const val INTRO = "intro"
    const val NEXT_PART = "next_part"

    /** Every screen name a slot or a no-ad rule may refer to. */
    val allScreens: Set<String> = setOf(
        HOME, HOME_SEARCH, EXAM_LIST, EXAM_DETAIL, EDITOR, MODEL_CARD, RESULT, PRINT, BUILDER, IMPORT,
        COMPRESS, COMPRESS_RUNNING, JOINER, SAVED, ABOUT, SHARE_SHEET, INTRO, NEXT_PART,
    )

    /** Nothing may load or show here (handoff 16A). */
    val noAdScreens: Set<String> = setOf(
        HOME_SEARCH, EDITOR, MODEL_CARD, PRINT, BUILDER, IMPORT, COMPRESS_RUNNING, JOINER, SHARE_SHEET, INTRO, NEXT_PART,
    )

    enum class Format { NATIVE, BANNER, INTERSTITIAL, REWARDED }
    data class Slot(val id: String, val format: Format, val screen: String)

    val slots: List<Slot> = listOf(
        Slot("result", Format.NATIVE, RESULT),
        Slot("exam_list", Format.NATIVE, EXAM_LIST),
        Slot("about", Format.BANNER, ABOUT),
        Slot("result_exit", Format.INTERSTITIAL, RESULT),
        Slot("adfree", Format.REWARDED, ABOUT),
    )

    /** Screens where a slot may appear at all. */
    val adScreens: Set<String> = slots.map { it.screen }.toSet()

    /** Set from the app's debuggable flag in MainActivity. Debug builds fail loudly on a misplaced slot. */
    @Volatile var debug = false

    fun config() = AdsConfig(enabled = ENABLED, noAdScreens = noAdScreens, adScreens = adScreens, debug = debug)

    /** Debug guard: throws when a slot is placed on a no-ad screen. A no-op in release builds. */
    fun guard(screen: String, debug: Boolean = this.debug) {
        if (debug && screen in noAdScreens) throw IllegalStateException("Ad slot placed on no-ad screen '$screen'")
    }

    private fun palette() = AdPalette(
        surface = Wiz.Panel.toArgb(), line = Wiz.Line.toArgb(), text = Wiz.Text.toArgb(), muted = Wiz.Muted.toArgb(),
        accent = Wiz.Accent.toArgb(), onAccent = Wiz.OnAccent.toArgb(),
    )

    /** Native card with a 24 dp gap above it; nothing at all while ads are off or [visible] is false. */
    @Composable
    fun NativeSlot(screen: String, visible: Boolean = true, modifier: Modifier = Modifier) {
        guard(screen)
        if (!ENABLED || !visible) return
        Column(modifier.fillMaxWidth()) {
            Spacer(Modifier.height(24.dp))
            WizNativeAdCard(screen, palette())
        }
    }

    @Composable
    fun BannerSlot(screen: String, modifier: Modifier = Modifier) {
        guard(screen)
        if (!ENABLED) return
        WizBannerAd(screen, modifier)
    }

    /** "Watch 1 ad, 24 h without ads" row. Hidden until a rewarded ad is loaded and no Pro is owned. */
    @Composable
    fun AdFreeRow(screen: String = ABOUT) {
        guard(screen)
        if (!ENABLED) return
        // Later release: row appears here once a rewarded ad is loaded.
    }

    /** Leaving a finished, saved Result: with ads off (or nothing saved) [then] runs at once. */
    fun resultExit(saved: Boolean, then: () -> Unit) {
        guard(RESULT)
        if (!ENABLED || !saved) then() else showInterstitialAtBreak(RESULT, then)
    }
}

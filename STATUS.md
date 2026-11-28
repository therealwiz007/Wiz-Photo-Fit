# Wiz Photo Fit - STATUS (branch feat/photo-fit-ad-slots-1.3.0, 1.3.0 (4))

Earlier releases: 1.0.0 (1) live; 1.1.0 (2) redesign; 1.2.0 (3) INTERNET for Play in-app updates (published on Closed testing - Alpha per user, 2026-11-28).

## What this release does (handoff SONNET_FOLLOWUP_PHOTO_FIT_ADS.md, items 1-12 + 13A)
- wiz-ads 0.3 SDK-free copy (`com.wizlabs.ads`, `WIZ_ADS_SDK_LINKED=false`), `AdsSetup.kt` with 5 slots, ENABLED=false, `result_exit` hook, debug guard, `AdsSetupTest`. No AdMob/UMP dependency, no AD_ID, no app-ID meta-data.
- Launcher label "Photo Fit" (`app_name`); "Wiz Photo Fit" = `app_full_name` (About, update prompt).
- Studio Indigo theme (tokens in `ui/components/Theme.kt`), new tape-card icon, indigo splash, privacy-policy colours, website colours. Yellow grep (FFC400/FFD54D/8F6A00) = 0 in app code.
- Snip: 11 looks (10 emotions + neutral) with eyebrows, ~200 ms morph, always-alive 15 fps loop, 72 dp on Home, one-shot reactions, 4 tap reactions + haptic tick.
- Edge-to-edge: `enableEdgeToEdge()` in the only activity, deprecated-API grep = 0, insets per screen, cutout-safe landscape.
- Claims cleaned (README, fastlane en-US + hi-IN, policy x2, strings). Policy: "No ads are shown in this version. Ads by Google AdMob may come in a later version".
- Store art: all new (see SONNET_TO_HAIKU.md "New assets"). Screens are real `adb screencap` captures.

## Decisions taken (logged, not asked)
- Icon: 3 concepts in `store-assets/icon-concepts/`. Picked C "tape card" (white photo card + portrait, sky crop marks, sky tape measure). Reason: reads at 48 px (3 shapes), and the tape-measure strip is unique (rivals use a plain crop frame or camera glyph); B read cleanest but is the generic crop-bracket look.
- Splash: Android 12+ system splash can show only the icon, so the "Wiz Photo Fit" wordmark is on Home, not on the splash. AVD splash animation not authored.
- Mascot tech: Compose Canvas, code-drawn (no Lottie/Rive). Idle clock is Snip's own (`rememberAliveClock`, 15 fps, never self-settles); the app-wide 9 s idle settle is NOT used for Snip (contract overrides it). Still frame only for reduce-motion / Battery Saver / unfocused.
- `SnipExpression.SEARCHING` removed (replaced by CURIOUS/THINKING). ChecksFail is now WORRIED (kind line "...Easy to sort.").
- Editor crop brackets use fixed light indigo `#8B8CFF` and the head oval sky `#38BDF8` so they read on the dark editor canvas in both themes.
- Local release signing: `app/build.gradle.kts` reads the common key props by absolute path (`/Users/aashish/Desktop/Personal FIles/Wiz Labs/KEYS/keystore.properties`) when `KEYSTORE_FILE` is unset; CI still uses the `KEYSTORE_*` secrets. Local release APK verified signer CN=Wiz Labs. NOTE: Photo Fit 1.0.0 was originally signed with `photo fit.jks`; if Play rejects a Common-key upload (key mismatch), STOP and report, do not switch keys.
- Screenshot 8 = dark Home ("Light and dark themes") because the app ships English only (no Hindi UI). No hi-IN image folder exists, so none was made.
- Result screenshot (1) uses the Signature slot of SSC (passes all 4 checks). The Photo slot needs a white background, and the emulator has no Play-services ML modules (background tool "not ready"), so a Photo result would show a failing background check; that is real app behaviour, not hidden.
- Photo input = NASA public-domain portrait; signature input generated for this release (store-assets/inputs/SOURCES.md). No private person used.
- Emulator GNSS icon sits in the status bar of screenshot 2 (demo mode cannot hide it). Left unedited.
- Removed old 1.1.0 art sources: `tools/store-art/feature.html`, `sample-*.html`, old `raw/*`, old `screenshots/*`, old HTML-based icon. New pipeline: `tools/store-art/make_shots.py` (frame + headline only), `snip_svg.py`.

## Results (real)
- `gradle --offline --no-daemon :app:testDebugUnitTest :app:assembleRelease`: PASS. Unit tests 76 PASS / 0 FAIL / 0 errors.
- Release APK (signed, CN=Wiz Labs): 1,921,057 bytes (1.83 MB, limit 3.2 MB). Label "Photo Fit", versionCode 4, versionName 1.3.0, targetSdk 36.
- Merged permissions: INTERNET, ACCESS_NETWORK_STATE, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (AndroidX signature permission). No AD_ID.
- `grep -rE "com\.google\.android\.(libraries\.ads|ump)" app/src` = 0; releaseRuntimeClasspath has no ads-mobile-sdk / user-messaging-platform / play-services-ads.
- Edge-to-edge grep (statusBarColor ... FLAG_TRANSLUCENT) = 0 lines in app/src.
- Emulator wiz35 (API 35, 1080x2400), one at a time, release APK: Home, search, exam detail, picker, editor (photo + signature + ID), result + checks + compare slider, print sheet, builder, dark theme, About, Exam list: PASS. Ad slots take 0 height (About end, Result, Exam list show no gap).
- Snip budget on Home idle (release): CPU samples 4.0 / 0.0 / 4.0 % (mean 2.7 %, limit 3 %); 150 frames in 10 s = 15 fps; gfxinfo legacy janky 0.00 %, p50/p90/p99 = 17/17/20 ms (emulator frames sit at the vsync deadline, so the new "janky" metric reads 100 %: not claimed as PASS). No allocation in the draw path by construction (reused Paths/FloatArrays), not profiled.
- QA captures in `store-assets/qa/`: launcher icon on home, About end (gesture, 3-button, landscape+3-button), Exam list, emotion sheet, `home-idle-10s.mp4` (true 10 s screenrecord), `snip-emotions-10s.mp4`.

## NOT DONE / NOT TESTED
- `snip-emotions-10s.mp4` is a 36-frame screencap sequence at 4 fps (9 s), NOT a screenrecord: `screenrecord` captured only 1-3 frames while the search keyboard/taps ran. It shows blink, glance, wink and the curious/thinking change but is coarse.
- Face auto-fit and background swap on a device with Play-services ML modules; TalkBack; 200 % font scale; reduce-motion; RTL; tablet; API 26/29/36 runtime; keyboard-open forms in 3-button mode; every screen in landscape (only About checked); Hindi.
- Cold start timing, 12/48 MP timing (unchanged code).
- Play "deprecated APIs" scan: no AAB scanned. LIBRARY-INTERNAL classes (androidx.activity EdgeToEdge*, material) may still be listed by Play; unverified.
- Splash wordmark and AVD splash animation (system splash limits). Tablet screenshots (none exist).
- Gradle `-q` runs: lint not run.

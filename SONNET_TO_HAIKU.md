# SONNET -> HAIKU handoff: Wiz Photo Fit 1.3.0 (4)

| Field | Content |
|---|---|
| Implementation summary | Ad slots (ads OFF, SDK-free wiz-ads 0.3, `AdsSetup.kt`, debug guard, tests); launcher label "Photo Fit" (full name "Wiz Photo Fit"); claims cleanup; Studio Indigo theme; new tape-card icon + splash; lively Snip (10 emotions, tap reactions, alive on Home); edge-to-edge fixes; all store art redone from real emulator captures; local signing via common key; version 1.3.0 (4). Details: STATUS.md. |
| Branch + last code commit | `feat/photo-fit-ad-slots-1.3.0` @ `1575fa9` (code + art). One docs commit follows it; release from the **branch tip** (`git log -1`). Not merged into main yet. |
| Modified files | see "git diff --stat main...HEAD" below |
| New assets | below |
| Final architecture | Compose single-activity app. Ads: `com.wizlabs.ads` (stub, no SDK) + `photofit/AdsSetup.kt`. Theme: `ui/components/Theme.kt` (`Wiz` tokens). Snip: `engine/SnipBrain.kt` (pure), `ui/mascot/SnipPose.kt` (morph table + `SnipDriver`), `SnipArt.kt` (draw), `Snip.kt` (composable). Update popup: `com.wizlabs.update`. |
| Application ID | com.wizlabs.photofit |
| Version | versionName 1.3.0, versionCode 4 (`app/build.gradle.kts`) |
| SDKs | minSdk 26, targetSdk 36, compileSdk 36 |
| Signing status | CI: `KEYSTORE_BASE64`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` secrets (android.yml). Local: common Wiz key by absolute path (file `keystore.properties` in Wiz Labs/KEYS), no secrets in repo. Local release APK was signed, signer CN=Wiz Labs. WARNING: 1.0.0 was first signed with the older "photo fit" key; if Play rejects for key mismatch, stop and report. |
| Build results | `~/.local/opt/gradle-8.11.1/bin/gradle --offline --no-daemon :app:testDebugUnitTest :app:assembleRelease` = PASS. Release APK 1,921,057 bytes (1.83 MB). |
| Test results | 76 unit tests PASS, 0 FAIL. Emulator checklist: 12 PASS / 0 FAIL / many NOT TESTED (list in STATUS.md). Evidence: `store-assets/qa/`. |
| Permissions diff | vs 1.0.0: +INTERNET, +ACCESS_NETWORK_STATE (since 1.2.0, Play in-app updates). vs 1.2.0 (3): unchanged. No AD_ID. |
| Data collected/shared | Unchanged vs 1.2.0. Device IDs + Diagnostics via Google Play-services ML Kit (collected, not shared). INTERNET used by Play in-app update. Ad slots exist but no ad SDK, so no ad data. |
| Privacy policy | https://therealwiz007.github.io/Wiz-Photo-Fit/privacy-policy.html ; both copies (repo root + `app/src/main/assets/`) updated (colours; wording unchanged from WIP). |
| ASO content | `fastlane/metadata/android/{en-US,hi-IN}/{title,short_description,full_description}.txt`. Counts in characters (trailing newline excluded): en-US title 30 / short 75 / full 3104; hi-IN title 30 / short 70 / full 1930; all within Play limits (30 / 80 / 4000). en-US full description refreshed in 1.3.0 (opening line, Snip, themes, compressor heading); hi-IN unchanged. Changelog `en-US/changelogs/4.txt` (344 chars). Title: "Wiz Photo Fit: Exam Photo&Sign". |
| Known issues / blockers | see NOT DONE below |
| Release instructions | Track Closed testing - Alpha. **1.2.0 (3) is already published**, so create a NEW release now named `4 (1.3.0)`. Notes: `fastlane/metadata/android/en-US/changelogs/4.txt`. Re-check: Data safety (INTERNET new since 1.0.0; no ad data), Ads = No, target audience 18+, India-only. |
| Decisions taken | STATUS.md "Decisions taken" |

## Acceptance checklist (handoff §25)
- [x] wiz-ads `--no-sdk` exists (WIP; 0 SDK imports in stub); `--no-compose` not needed here (Compose app).
- [x] `grep` of ads/ump SDK in app/src = 0; release classpath has neither SDK.
- [x] 5 slots, 0 height; `result_exit` calls `then()` at once; `AdsSetupTest` green (6 tests incl. debug guard throws).
- [x] Launcher "Photo Fit"; About "Wiz Photo Fit".
- [x] No "no internet / 0 permissions / no ads" claims left (README, fastlane, policy x2, strings). Allowed wording: "No ads are shown in this version".
- [x] §23A: new icon (3 concepts), icon-512, feature graphic, 8 real-capture screenshots, no HTML mock-ups of UI, old files deleted.
- [~] §13A: Snip 10 emotions + eyebrows + morph + tap reactions, SnipBrainTest green, emotion sheet saved. Recording: true screenrecord for Home idle; emotion change is a screencap sequence (see STATUS).
- [x] Studio Indigo everywhere in app code; yellow grep = 0.
- [x] 1.3.0 (4); 76 tests green; APK 1.83 MB (<= 3.2 MB).
- [~] Edge-to-edge: deprecated-API grep 0, insets handled; LIBRARY-INTERNAL classes unverified on Play.

## New assets (replaced / deleted)
Icon: `app/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml` (new, gradient), `ic_launcher_monochrome.xml`, `ic_splash.xml`, `mipmap-anydpi-v26/ic_launcher.xml`; concepts `store-assets/icon-concepts/concept-{a-frame-marks-arrow,b-corner-brackets-portrait,c-tape-card}.svg` (chosen: C); `tools/store-art/icon.svg`; **Play icon `store-assets/icon-512.png`** (512x512 RGBA, replaced).
Feature graphic: **`store-assets/feature-graphic-1024x500.png`** (replaced; built by `tools/store-art/make_shots.py`).
Screenshots: **`store-assets/screenshots/1.png` ... `8.png`** (1080x1920, order: 1 Result checks "Exam photo & sign in exact KB", 2 Home search "36 exam presets, searchable", 3 Crop "Crop with guides", 4 Signature before/after "Clean, sharp signatures", 5 ID crop "Passport & ID photos", 6 Print sheet "Print 4x6 or A4 sheets", 7 Builder "Your own custom sizes", 8 Dark Home "Light and dark themes"). Real captures in `store-assets/raw/raw{1..8}_*.png`; inputs in `store-assets/inputs/`.
Mascot: `store-assets/mascot/snip.svg`, `snip-proud.svg`, `snip-512.png`, `snip-1024.png`; emotion sheet `store-assets/qa/emotion-sheet.{png,svg}`.
QA: `store-assets/qa/` (launcher icon, About/Exam list, `home-idle-10s.mp4`, `snip-emotions-10s.mp4`).
**Deleted:** `store-assets/screenshots/{1-result,2-search,3-crop,4-signature,5-passport,6-print,7-custom,8-dark}.png`, `store-assets/raw/{portrait_white,raw3_editor,raw4_sign_compare}.png`, old `icon-concepts/concept-{a-snipped-tile,b-crop-brackets,c-kb-tag}.svg`, `tools/store-art/{feature,sample-photo,sample-photo-white,sample-sign}.html`. Old 1.1.0 raw1/2/5/6/7/8 files were overwritten by new captures. No fastlane image folders exist (no hi-IN graphics in repo). `HAIKU_PROMPT_PHOTO_FIT_STORE_GRAPHICS.md` is no longer needed.

## Competitor ASO (new step 8c in the Haiku prompt)
Haiku researches the top Play apps for the main queries, records a competitor table and keyword gaps in `HAIKU_RELEASE_REPORT.md`, and applies only limit-safe, claim-safe edits to the fastlane listing files before upload. Sonnet did NOT do this research.

## Play Console graphics (do now, with the 1.3.0 (4) release)
Replace the app icon with `icon-512.png`, replace the feature graphic, DELETE all old phone screenshots, upload `1.png`..`8.png` in order and check the order; same for any translated listing that has its own graphics (hi-IN: only if it has its own). Send for review with the release.

## NOT DONE / NOT TESTED
- `snip-emotions-10s.mp4` is a 36-frame screencap sequence at 4 fps (9 s), NOT a screenrecord: `screenrecord` captured only 1-3 frames while the search keyboard/taps ran. It shows blink, glance, wink and the curious/thinking change but is coarse.
- Face auto-fit and background swap on a device with Play-services ML modules; TalkBack; 200 % font scale; reduce-motion; RTL; tablet; API 26/29/36 runtime; keyboard-open forms in 3-button mode; every screen in landscape (only About checked); Hindi.
- Cold start timing, 12/48 MP timing (unchanged code).
- Play "deprecated APIs" scan: no AAB scanned. LIBRARY-INTERNAL classes (androidx.activity EdgeToEdge*, material) may still be listed by Play; unverified.
- Splash wordmark and AVD splash animation (system splash limits). Tablet screenshots (none exist).
- Gradle `-q` runs: lint not run.

## git diff --stat main...HEAD (at code commit 1575fa9)
```
 README.md                                          |  13 +-
 app/build.gradle.kts                               |  17 +-
 app/src/main/assets/privacy-policy.html            |  10 +-
 app/src/main/java/com/wizlabs/ads/AdsConfig.kt     |  54 +++++
 app/src/main/java/com/wizlabs/ads/AdsPrefs.kt      |  37 ++++
 app/src/main/java/com/wizlabs/ads/WizAds.kt        | 157 ++++++++++++++
 .../java/com/wizlabs/ads/compose/WizAdSlots.kt     |  18 ++
 app/src/main/java/com/wizlabs/ads/core/AdGate.kt   |  72 +++++++
 .../java/com/wizlabs/ads/core/AdsInitializer.kt    |  16 ++
 .../main/java/com/wizlabs/ads/core/BannerHost.kt   |  14 ++
 .../java/com/wizlabs/ads/core/ConsentManager.kt    |  22 ++
 .../main/java/com/wizlabs/ads/core/NativeAdPool.kt |  22 ++
 .../java/com/wizlabs/ads/core/WizNativeAdView.kt   |  15 ++
 app/src/main/java/com/wizlabs/photofit/AdsSetup.kt | 117 +++++++++++
 .../main/java/com/wizlabs/photofit/MainActivity.kt |  13 +-
 .../java/com/wizlabs/photofit/engine/SnipBrain.kt  |  52 ++++-
 .../com/wizlabs/photofit/ui/components/Common.kt   |   7 +-
 .../com/wizlabs/photofit/ui/components/HeroArt.kt  |   2 +-
 .../com/wizlabs/photofit/ui/components/Loading.kt  |   2 +-
 .../com/wizlabs/photofit/ui/components/Specs.kt    |   2 +-
 .../com/wizlabs/photofit/ui/components/Theme.kt    |  34 +--
 .../wizlabs/photofit/ui/components/UpdatePrompt.kt |   8 +-
 .../java/com/wizlabs/photofit/ui/mascot/Snip.kt    |  49 ++++-
 .../java/com/wizlabs/photofit/ui/mascot/SnipArt.kt | 222 ++++++++++++++------
 .../com/wizlabs/photofit/ui/mascot/SnipPose.kt     | 229 ++++++++++++++++++---
 .../com/wizlabs/photofit/ui/screens/AboutScreen.kt |  10 +-
 .../wizlabs/photofit/ui/screens/EditorScreen.kt    |   4 +-
 .../wizlabs/photofit/ui/screens/ExamListScreen.kt  |  11 +-
 .../com/wizlabs/photofit/ui/screens/HomeScreen.kt  |  30 ++-
 .../wizlabs/photofit/ui/screens/ResultScreen.kt    |  11 +-
 .../main/res/drawable/ic_launcher_background.xml   |  11 +
 .../main/res/drawable/ic_launcher_foreground.xml   |  13 +-
 .../main/res/drawable/ic_launcher_monochrome.xml   |   7 +-
 app/src/main/res/drawable/ic_splash.xml            |  13 +-
 app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml |   2 +-
 app/src/main/res/values-night/themes.xml           |   2 +-
 app/src/main/res/values/themes.xml                 |   6 +-
 .../test/java/com/wizlabs/ads/core/AdGateTest.kt   |  46 +++++
 .../test/java/com/wizlabs/photofit/AdsSetupTest.kt |  41 ++++
 .../java/com/wizlabs/photofit/SnipBrainTest.kt     | 111 ++++++++++
 .../test/java/com/wizlabs/photofit/Wave2Test.kt    |  19 --
 fastlane/metadata/android/en-US/changelogs/1.txt   |   2 +-
 fastlane/metadata/android/en-US/changelogs/3.txt   |   2 +-
 fastlane/metadata/android/en-US/changelogs/4.txt   |   1 +
 .../metadata/android/en-US/full_description.txt    |   4 +-
 .../metadata/android/hi-IN/full_description.txt    |   4 +-
 index.html                                         |  10 +-
 privacy-policy.html                                |  10 +-
 store-assets/feature-graphic-1024x500.png          | Bin 42215 -> 120434 bytes
 store-assets/icon-512.png                          | Bin 8986 -> 95124 bytes
 .../icon-concepts/concept-a-frame-marks-arrow.svg  |   7 +
 .../concept-b-corner-brackets-portrait.svg         |   6 +
 store-assets/icon-concepts/concept-c-tape-card.svg |   7 +
 store-assets/inputs/SOURCES.md                     |   4 +
 store-assets/inputs/portrait_1200.jpg              | Bin 0 -> 236169 bytes
 store-assets/inputs/signature_sample.jpg           | Bin 0 -> 57475 bytes
 store-assets/mascot/snip-1024.png                  | Bin 32971 -> 53374 bytes
 store-assets/mascot/snip-512.png                   | Bin 15512 -> 48335 bytes
 store-assets/mascot/snip-proud.svg                 |  14 +-
 store-assets/mascot/snip.svg                       |  27 +--
 store-assets/qa/about-end-3button.png              | Bin 0 -> 251105 bytes
 store-assets/qa/about-end-gesture.png              | Bin 0 -> 263105 bytes
 store-assets/qa/about-landscape-3button.png        | Bin 0 -> 146688 bytes
 store-assets/qa/about-top-3button.png              | Bin 0 -> 227420 bytes
 store-assets/qa/emotion-sheet.png                  | Bin 0 -> 89125 bytes
 store-assets/qa/emotion-sheet.svg                  |   1 +
 store-assets/qa/examlist-gesture.png               | Bin 0 -> 261170 bytes
 store-assets/qa/examlist-scrolled-gesture.png      | Bin 0 -> 246595 bytes
 store-assets/qa/home-idle-10s.mp4                  | Bin 0 -> 1201964 bytes
 store-assets/qa/launcher-icon-home.png             | Bin 0 -> 1373363 bytes
 store-assets/qa/snip-emotions-10s.mp4              | Bin 0 -> 62544 bytes
 store-assets/raw/portrait_white.png                | Bin 54165 -> 0 bytes
 store-assets/raw/raw1_result.png                   | Bin 196900 -> 203797 bytes
 store-assets/raw/raw2_home_search.png              | Bin 66777 -> 92268 bytes
 store-assets/raw/raw3_crop_editor.png              | Bin 0 -> 1020773 bytes
 store-assets/raw/raw3_editor.png                   | Bin 157455 -> 0 bytes
 store-assets/raw/raw4_sign_compare.png             | Bin 276016 -> 0 bytes
 store-assets/raw/raw4_signature_compare.png        | Bin 0 -> 239496 bytes
 store-assets/raw/raw5_id_editor.png                | Bin 235131 -> 1090324 bytes
 store-assets/raw/raw6_print.png                    | Bin 256121 -> 666527 bytes
 store-assets/raw/raw7_builder.png                  | Bin 149072 -> 154369 bytes
 store-assets/raw/raw8_dark_home.png                | Bin 195471 -> 193584 bytes
 store-assets/screenshots/1-result.png              | Bin 185049 -> 0 bytes
 store-assets/screenshots/1.png                     | Bin 0 -> 256888 bytes
 store-assets/screenshots/2-search.png              | Bin 94208 -> 0 bytes
 store-assets/screenshots/2.png                     | Bin 0 -> 159212 bytes
 store-assets/screenshots/3-crop.png                | Bin 149594 -> 0 bytes
 store-assets/screenshots/3.png                     | Bin 0 -> 725061 bytes
 store-assets/screenshots/4-signature.png           | Bin 220296 -> 0 bytes
 store-assets/screenshots/4.png                     | Bin 0 -> 278990 bytes
 store-assets/screenshots/5-passport.png            | Bin 189432 -> 0 bytes
 store-assets/screenshots/5.png                     | Bin 0 -> 775589 bytes
 store-assets/screenshots/6-print.png               | Bin 185075 -> 0 bytes
 store-assets/screenshots/6.png                     | Bin 0 -> 506695 bytes
 store-assets/screenshots/7-custom.png              | Bin 162887 -> 0 bytes
 store-assets/screenshots/7.png                     | Bin 0 -> 227203 bytes
 store-assets/screenshots/8-dark.png                | Bin 190941 -> 0 bytes
 store-assets/screenshots/8.png                     | Bin 0 -> 257653 bytes
 tools/store-art/feature.html                       |  19 --
 tools/store-art/icon.svg                           |  12 +-
 tools/store-art/make_shots.py                      |  50 +++++
 tools/store-art/sample-photo-white.html            |  16 --
 tools/store-art/sample-photo.html                  |  16 --
 tools/store-art/sample-sign.html                   |   6 -
 tools/store-art/snip_svg.py                        |  65 ++++++
 105 files changed, 1497 insertions(+), 327 deletions(-)
```

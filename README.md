# Wiz Photo Fit

Android app by Wiz Labs: resize exam / government-form photos and signatures to the exact size and KB, make ID / passport photos with a print sheet, and batch-compress images.

- **1 permission (internet, used for Google Play app updates and, in a later version, ads). No ads are shown in this version, and the build contains no ad SDK.** Face and background models are fetched once by Google Play services. Photos come from the system Photo Picker or camera app; everything runs on the phone.
- 36 built-in exam presets (SSC, UPSC, RRB, IBPS/SBI/RBI, JEE, NEET, CUET, GATE, CAT, state PSCs, school entrances, PAN …) compiled from public notices, plus your own custom sizes (px / cm / mm / inch, DPI, KB range) that you can share as a code or QR.
- Kotlin + Jetpack Compose, minSdk 26, targetSdk 36. Package `com.wizlabs.photofit`.

## Build
```
ANDROID_HOME=~/Library/Android/sdk ~/.local/opt/gradle-8.11.1/bin/gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```
CI (`.github/workflows/android.yml`) builds the signed release APK and AAB on every push to `main` using the `KEYSTORE_*` secrets, uploaded as "Wiz Photo Fit (version) APK/AAB".

Design: `../WIZ_PHOTO_FIT_SPEC.md`, `../WIZ_PHOTO_FIT_ARCHITECTURE.md`. Presets: `app/src/main/assets/presets.json`.

## Changelog
### 1.3.0 (4) - ad slots (ads OFF), new icon and store art, edge-to-edge
- `wiz-ads` 0.3 SDK-free module and `AdsSetup.kt`: slots `result`, `exam_list`, `about`, `result_exit`, `adfree` with `ENABLED=false` (zero height, no ad SDK, no AdMob dependency). Debug guard + `AdsSetupTest`.
- Launcher label "Photo Fit"; "Wiz Photo Fit" stays in About, update prompt and the Play title.
- New text-free launcher icon, splash, feature graphic and 8 screenshots captured from the real app.
- Android 15 edge-to-edge: no deprecated window APIs, cutout-safe in landscape.
- Removed "no ads / offline / 0 permissions" wording (INTERNET is declared).

### 1.1.0 (2) - Studio Marigold redesign
- New light-first "Studio Marigold" look (dark theme follows the system), Plus Jakarta Sans + DM Mono, new text-free adaptive + monochrome icon, splash.
- Snip, a code-drawn mascot (first-run intro, empty states, model download/unavailable, result checks). Decorative, still when Battery Saver / reduce-motion is on.
- Search-first Home (ranked search over exams, ID sizes and your own sizes) and a Recent row (last 5, stored on the phone).
- Result screen: compliance check card (dimensions, KB, format, DPI, background colour, face size for ID photos) and a before/after compare slider with Original/Final buttons.
- Launcher label is now "Wiz Photo Fit". Preset values are unchanged (guarded by `PresetCountTest`).
- Tests: search ranking, compliance, Snip brain, recents, preset checksum.

### 1.0.0 (1)
- First release.

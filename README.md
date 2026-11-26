# Wiz Photo Fit

Offline Android app by Wiz Labs: resize exam / government-form photos and signatures to the exact size and KB, make ID / passport photos with a print sheet, and batch-compress images.

- **No ads · No internet · 0 permissions** (the app has no INTERNET permission; the optional face and background models are fetched once by Google Play services). Photos come from the system Photo Picker or camera app; everything runs on the phone.
- 36 built-in exam presets (SSC, UPSC, RRB, IBPS/SBI/RBI, JEE, NEET, CUET, GATE, CAT, state PSCs, school entrances, PAN …) compiled from public notices, plus your own custom sizes (px / cm / mm / inch, DPI, KB range) that you can share as a code or QR.
- Kotlin + Jetpack Compose, minSdk 26, targetSdk 36. Package `com.wizlabs.photofit`.

## Build
```
ANDROID_HOME=~/Library/Android/sdk ~/.local/opt/gradle-8.11.1/bin/gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```
CI (`.github/workflows/android.yml`) builds the signed release APK and AAB on every push to `main` using the `KEYSTORE_*` secrets, uploaded as "Wiz Photo Fit (version) APK/AAB".

Design: `../WIZ_PHOTO_FIT_SPEC.md`, `../WIZ_PHOTO_FIT_ARCHITECTURE.md`. Presets: `app/src/main/assets/presets.json`.

## Changelog
### 1.1.0 (2) - Studio Marigold redesign
- New light-first "Studio Marigold" look (dark theme follows the system), Plus Jakarta Sans + DM Mono, new text-free adaptive + monochrome icon, splash.
- Snip, a code-drawn mascot (first-run intro, empty states, model download/unavailable, result checks). Decorative, still when Battery Saver / reduce-motion is on.
- Search-first Home (ranked search over exams, ID sizes and your own sizes) and a Recent row (last 5, stored on the phone).
- Result screen: compliance check card (dimensions, KB, format, DPI, background colour, face size for ID photos) and a before/after compare slider with Original/Final buttons.
- Launcher label is now "Wiz Photo Fit". Preset values are unchanged (guarded by `PresetCountTest`).
- Tests: search ranking, compliance, Snip brain, recents, preset checksum.

### 1.0.0 (1)
- First release.

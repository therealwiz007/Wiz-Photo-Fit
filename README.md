# Wiz Photo Fit

Offline Android app by Wiz Labs: resize exam / government-form photos and signatures to the exact size and KB, make ID / passport photos with a print sheet, and batch-compress images.

- **No ads · No internet · 0 permissions.** Photos come from the system Photo Picker or camera app; everything runs on the phone.
- 36 built-in exam presets (SSC, UPSC, RRB, IBPS/SBI/RBI, JEE, NEET, CUET, GATE, CAT, state PSCs, school entrances, PAN …) compiled from public notices, plus your own custom sizes (px / cm / mm / inch, DPI, KB range) that you can share as a code or QR.
- Kotlin + Jetpack Compose, minSdk 26, targetSdk 36. Package `com.wizlabs.photofit`.

## Build
```
ANDROID_HOME=~/Library/Android/sdk ~/.local/opt/gradle-8.11.1/bin/gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```
CI (`.github/workflows/android.yml`) builds the signed release APK and AAB on every push to `main` using the `KEYSTORE_*` secrets, uploaded as "Wiz Photo Fit (version) APK/AAB".

Design: `../WIZ_PHOTO_FIT_SPEC.md`, `../WIZ_PHOTO_FIT_ARCHITECTURE.md`. Presets: `app/src/main/assets/presets.json`.

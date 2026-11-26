# SONNET -> HAIKU handoff: Wiz Photo Fit 1.1.0 (2)

| Field | Content |
|---|---|
| Summary | Studio Marigold light-first redesign (sections 9-12), text-free icon + monochrome + splash (15), Snip mascot + SnipBrain (13-14), search-first Home + Recent (N3), compliance check card (N4), before/after slider (N5), tests (N6), icon (N7), quality upgrade (Lanczos, 4x signature clean, unsharp), store assets (20-23). Preset data unchanged. |
| Branch + SHA | redesign/wiz-photo-fit-v1.1.0 @ d1bc348 (a docs commit follows on top) |
| Modified files | `git diff --stat main...HEAD` (85 files; see git) |
| New assets | store-assets/icon-512.png, store-assets/feature-graphic-1024x500.png, store-assets/screenshots/{1-result,2-search,3-crop,4-signature,5-passport,6-print,7-custom,8-dark}.png (1080x1920), store-assets/mascot/{snip.svg,snip-proud.svg,snip-512.png,snip-1024.png}, store-assets/raw/*.png (unframed emulator captures), tools/store-art/* (pipeline), res/drawable/{ic_launcher_foreground,ic_launcher_monochrome,ic_splash}.xml |
| Architecture | Same Nav/Compose app. `Wiz` tokens theme-aware (ui/components/Theme.kt); new: ui/mascot/{Snip,SnipArt,SnipPose}.kt, engine/{SnipBrain,Compliance,Resample}.kt, data/{SearchIndex,Recents}.kt, ui/components/CompareSlider.kt, ui/screens/IntroGuide.kt; Render.crop rewritten (halving + Lanczos). |
| Application ID | com.wizlabs.photofit |
| Version | versionName 1.1.0 / versionCode 2 (app/build.gradle.kts) |
| SDKs | minSdk 26 / targetSdk 36 / compileSdk 36 |
| Signing | Release signing from env KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD (CI secrets, .github/workflows/android.yml). Not touched. Local release test APK was signed with the DEBUG key only and is not shipped. |
| Build | `gradle --offline :app:testDebugUnitTest :app:assembleRelease` PASS. Release APK (unsigned) 1,894,235 bytes (1.8 MB). |
| Tests | 53 unit tests PASS, 0 FAIL. Emulator checklist: see STATUS.md (many PASS, several NOT TESTED with reasons). Evidence: store-assets/raw/*.png. |
| Acceptance | See STATUS.md "Acceptance". |
| Permissions diff | None added or removed. INTERNET/ACCESS_NETWORK_STATE still removed. |
| Data collected/shared | Unchanged: none by the app. Recents stores preset ids in local SharedPreferences. ML Kit models via Google Play services unchanged from 1.0.0. No code that sends data. |
| Privacy policy | https://therealwiz007.github.io/Wiz-Photo-Fit/privacy-policy.html. Not changed (data handling unchanged). Both copies identical. |
| ASO | fastlane/metadata/android/en-US/{title.txt (30 chars), short_description.txt (75), full_description.txt (~2950), changelogs/2.txt (283)}; hi-IN title/full updated for "Wiz" and removed "no ads" claim; Hindi lacks the new-feature text (machine translation not done). |
| Known issues | Cold start above 1100 ms on emulator (1.2-2.1 s compiled). Face/background untested on a Play-services device. No tablet layout. No splash animation. Jank metric inconclusive. Compare image looks soft at tiny sizes (output is only 140x60 px by form rules). |
| Release instructions | Closed testing - Alpha, release name "2 (1.1.0)", notes = fastlane/.../changelogs/2.txt. Re-check: Data safety (no data collected; ML Kit via Google Play services may be seen as Device IDs/Diagnostics: ask the user before changing), no permissions, Ads = No, target 18+, India only. Do not upload if 1.0.0 (1) is still in review. |
| Questions | 1) Title with "Wiz" (default yes) or keep "Photo Fit: Exam Photo & Sign"? 2) Ads-ready for Photo Fit (INTERNET + wiz-ads OFF + update popup) would remove the "no internet" claim: do it as 1.2.0? 3) Merge to main so CI builds the AAB? 4) Native review of Hindi text? |

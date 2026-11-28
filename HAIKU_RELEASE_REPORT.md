# Haiku release report: Wiz Photo Fit 1.3.0 (4)

Date: 2026-11-28. Release built from `main` (tip 99f8b06, merge 34345fb includes 1575fa9). Feature branch not merged again.

## Version
- applicationId com.wizlabs.photofit, versionCode 4, versionName 1.3.0, minSdk 26, targetSdk 36, compileSdk 36 (`app/build.gradle.kts`)
- Play track: Closed testing - Alpha, release name `4 (1.3.0)`. Previous release 3 (1.2.0) stays live.

## Build and tests
- `gradle --offline :app:testDebugUnitTest :app:assembleRelease`: BUILD SUCCESSFUL. Unit tests 76 run, 0 failures, 0 errors (counted from `app/build/test-results`).
- Release APK (local, signer CN=Wiz Labs): 1,921,057 bytes.
- CI "Wiz Photo Fit - Production APK and AAB" on main tip 99f8b06: success (run 46158414607).

## AAB
- Path: `dist/ci/Wiz Photo Fit (1.3.0) AAB/Wiz Photo Fit (1.3.0).aab` (gitignored)
- Size 4,586,075 bytes (under the 10 MB split limit, uploaded as one file)
- SHA-256 399a67e3847132f672250fc912dda5c08d8e2848afb768c3232c4b3f910b2272
- Signer: `CN=Wiz Labs, OU=Wiz Labs, O=Wiz Labs, L=Ambala, ST=Haryana, C=IN` (from `META-INF/*.RSA`). Upload accepted, no key-mismatch error.
- Play bundle row shows version 4 (1.3.0), API 26+, target SDK 36.
- versionCode 4 confirmed in the local APK manifest and in source; the AAB's binary manifest was not decoded (no bundletool here).

## Permissions (merged APK manifest)
- `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, the AndroidX `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` signature permission. No AD_ID, no AdMob app-ID meta-data.
- Ads SDK check: `grep` for `com.google.android.gms.ads` and `ump` in app sources returned 0 hits; APK contains 0 entries matching gms/ads, ump or admob.
- AdsSetup `ENABLED = false`; ad slots have zero height. Play "Contains ads" must stay **No**.

## Store assets
- App icon: `store-assets/icon-512.png` (512x512). Play now shows 1 icon (the new tape-card icon). Old icon removed.
- Feature graphic: `store-assets/feature-graphic-1024x500.png` (1024x500). Old feature graphic (which claimed "No ads / Offline / 0 permissions") removed; new one saved.
- Phone screenshots: `store-assets/screenshots/1.png`..`8.png` (1080x1920). Restyled this run to headline + subline + larger phone frame, using only real emulator captures. Uploaded in order 1..8 (verified via DOM order). All 8 old `01-home`..`08-compress` screenshots deleted.
- Generator: `tools/store-art/make_shots.py` (changed; uncommitted until the report commit).
- hi-IN: no graphics in repo; none uploaded.
- Tablet / foldable / desktop / XR screenshots: none in repo; not uploaded.

## Listing text (en-US, saved in Play)
- Title `Wiz Photo Fit: Exam Photo&Sign` 30/30
- Short description 75/80
- Full description 3104/4000 (matches `fastlane/metadata/android/en-US/full_description.txt`)
- Release notes (en-US, from `changelogs/4.txt`) pasted with `<en-US>` tags.
- No banned words (best, #1, top, free, price) in en-US text.
- hi-IN listing (saved): title `Wiz Photo Fit: परीक्षा फोटो KB` 30/30, short 70/80, full 1930/4000, matching `fastlane/metadata/android/hi-IN/`. The old Play hi-IN text said "पूरी तरह ऑफलाइन, कोई विज्ञापन नहीं, कोई परमिशन नहीं" (fully offline, no ads, no permissions) and "इंटरनेट परमिशन नहीं", both now false; both removed.
- Publishing overview after the hi-IN save: "Changes in review", no unsent changes shown.

## Competitor ASO check (step 8c)
- Scope: query "exam photo resizer" only (India, English), top 5 distinct apps checked: Exam Photo Resizer & Editor (Apptivize), Photo Resizer & Signature 20KB (Aarvion), GovPhoto (Dhanuk Softwares), Photo Resizer for Form Fill Up (FairSoftTech), Photo & Picture Resizer (Farluner). All 5 show "Contains ads" and in-app purchases/ads.
- Queries "photo size in KB", "signature resizer", "passport size photo", "SSC photo size", "compress photo to 20 KB" and the Hindi queries were NOT analysed.
- Decision: no listing text edit. The current en-US short and full descriptions already contain the competitor keywords (SSC, NEET, JEE, bank, KB, signature). No copying of competitor text.

## Data safety, App content, declarations
- Data safety: not re-edited in this run. Handoff says it is unchanged vs 1.2.0 (Device IDs + Diagnostics via ML Kit, not shared). INTERNET is used by Play in-app updates; no ad data.
- App content overview: "You're all caught up", nothing pending.
- Ads = No, target audience 18+, India-only: as already set; not re-verified in this run.
- Edge-to-edge warnings (step 8b): not present in the Play review screen for this release. The only warning was "Changes to your supported devices" (informational). Pre-launch report not checked.

## Submission status
- Release saved on Closed testing - Alpha, then "Send changes for review" clicked.
- Publishing overview now shows **"Changes in review"** for Photo Fit (4 (1.3.0)). Seen on screen; not yet approved.
- Production: NOT touched. Needs a separate explicit yes from the user. Play also needs 12+ opted-in testers for 14 days on new personal-account apps; current count not checked.

## Outstanding issues
2. Competitor check covers one query, not all six.
3. Data safety and App content declarations not re-verified in this run.
4. Snip emotion recording is a screencap sequence, not a screenrecord (from Sonnet's STATUS.md); no store impact.
5. Face auto-fit and background swap not tested on a device with Play-services ML modules (STATUS.md).
6. Local release key is the common Wiz key. 1.0.0 used the older "photo fit" key; Play accepted the 1.3.0 AAB under the common key.

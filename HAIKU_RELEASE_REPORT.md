# Haiku release report: Wiz Photo Fit 1.1.0 (2)

Date: 2026-11-26. Branch merged to main (fast-forward) at e76388a.

## Version and build
- applicationId com.wizlabs.photofit, versionCode 2, versionName 1.1.0, minSdk 26, targetSdk 36, compileSdk 36.
- Local: `gradle --offline :app:testDebugUnitTest :app:assembleRelease` BUILD SUCCESSFUL. Unit tests 53 run, 0 failures, 0 errors (counted from XML results). Unsigned release APK 1,894,235 bytes.
- CI (run 45743657858, push to main): success. Artifacts: `Wiz Photo Fit (1.1.0) AAB` 4,389,553 bytes, `Wiz Photo Fit (1.1.0) APK` 1,719,763 bytes.
- Local copies: dist/ci/Wiz Photo Fit (1.1.0) AAB/Wiz Photo Fit (1.1.0).aab (4,534,859 bytes on disk; gitignored).

## Signing
- `jarsigner -verify -certs` on the AAB: jar verified, signed by CN=Wiz Labs, OU=Wiz Labs, O=Wiz Labs, L=Ambala, ST=Haryana, C=IN. Signature file alias WIZFUELD.
- Same signer as the 1.0.0 (1) AAB in play-uploads-v2 (matching upload key).
- APK badging (aapt2 on CI APK): package com.wizlabs.photofit, versionCode 2, versionName 1.1.0, targetSdk 36.
- Secrets and keystore not printed, copied or changed.
- Not checked: AAB manifest versionCode read directly (no bundletool here). Versioning taken from the CI APK built from the same commit.

## Play Console (Closed testing - Alpha)
- Existing 1.0.0 (1): live on the track ("Available to selected testers", released Nov 14 2026, 178 countries). Publishing submissions for the app: 0 in review. No review in progress.
- New draft release "2 (1.1.0)" created on Closed testing - Alpha:
  - AAB uploaded (App bundle 2 (1.1.0), API 26+, target SDK 36, 4 ABIs).
  - Release notes en-US (changelogs/2.txt, wrapped in language tags). hi-IN not provided.
  - Saved. Publishing overview: "Changes not yet submitted for review" (Closed testing - Alpha, 2 (1.1.0)).
  - NOT sent for review.
- Warning shown by Play: native code without debug symbols (optional symbol upload).
- Not done: store listing screenshots upload (en-US, 8 images in order from store-assets/screenshots/). Listing page not reached in this run.

## Store assets and listing
- icon-512.png: 512x512, text-free. OK.
- feature-graphic-1024x500.png: 1024x500. OK.
- screenshots 1-8: all 1080x1920. Not re-viewed in this run.
- Title (30 chars): "Wiz Photo Fit: Exam Photo&Sign". OK.
- Short description: 75 chars. OK.
- Full description en-US: 2,941 chars. Changed: "Small download and smooth on low-end phones" -> "Small download" (claim not verified: jank inconclusive, cold start over target).
- Hindi (hi-IN) full description still has no new-feature text. Known.
- Promo-word scan of en-US listing and changelog: no hits for best, #1, top, free, price, ads, promo.

## Claims vs build (spot-checked in code)
- Present: name+date on photo (PresetBuilderScreen), join (SavedJoin), WebP output (CompressScreen, BitmapCodec), face and background tools (FaceFinder, BackgroundSwap), compare slider (CompareSlider), compliance check (Compliance), search (SearchIndex), recents (Recents), Snip (SnipBrain, SnipArt).
- Not verified on device: automatic face fit and background swap on a Play-services device; TalkBack; tablet; 200 % font scale.

## Privacy policy
- Changed (privacy-policy.html at root and app/src/main/assets/, identical): ML Kit line now says Google Play services downloads the models once, the app runs them on the phone, the app has no internet access, and the download is under Google's privacy policy. "Last updated" 26 November 2026.
- Published by GitHub Pages from main (source branch main, path /). Live after the merge; not re-fetched in this run.

## Data safety (not changed; needs owner answers)
- Current declaration: no data collected.
- Proposed review: Play-services ML Kit model download may count as Device or other IDs and Diagnostics. Owner must confirm before any change in Play Console.
- Not yet read from the Play Console Data safety page in this run.

## Compliance checks
- Merged manifest: no INTERNET, no ACCESS_NETWORK_STATE; only AndroidX DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (signature). Matches "no permissions" declaration.
- Ads: No (no ad SDK).
- Target audience 18+ and India-only availability: not re-checked in Play Console this run.

## Production
- Not touched. Requires explicit owner approval. 12+ opted-in testers for 14 days may apply to new personal accounts; count not read.

## Ads-ready 1.2.0 (not done)
- Blocked on decision: wiz-ads (/Users/aashish/Desktop/Personal FIles/Claude Code/wiz-ads) header says v0.2 links the GMA Next-Gen SDK and UMP. The SDK-free v0.1 pattern (PDF Master 1.5.0) adds no ad SDK. Need owner pick: v0.1 SDK-free, or v0.2 with SDK.
- Going ads-ready also drops the "No internet" claim (listing, trust text, README, privacy policy) and needs Data safety changes.

## Outstanding issues (unchanged from Sonnet)
- Cold start 1.2–2.1 s compiled on emulator vs 1.1 s target.
- Face auto-fit and background swap untested on a Play-services device.
- No tablet layout.
- Hindi listing lacks new-feature text.
- Native debug symbols not uploaded (Play warning).

## Status summary
- Merged and pushed: yes (main, e76388a).
- CI AAB: built, downloaded, signed by Wiz Labs.
- Play draft 2 (1.1.0): AAB and notes staged, not submitted.
- Waiting on owner: Data safety answers, store screenshots upload, ads-ready decision, review submit yes, production (no).

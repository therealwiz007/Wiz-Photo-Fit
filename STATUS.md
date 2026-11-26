# Wiz Photo Fit - STATUS (branch redesign/wiz-photo-fit-v1.1.0, 1.1.0 (2))

## Drift from the handoff (found 2026-11-26)
- HEAD matched d1092d0 on main. `.gitignore` had an uncommitted user change (keystore ignores): left as is, not committed. `Wiz Photo Fit (1.0.0 debug).apk` untracked in root: not committed.
- Tests: handoff said "1 test file". Real: 1 file, 7 classes, 14 tests (Units, TargetEncoder, Padding, PdfWriter, PresetJson, CropMath...). Baseline 14/14 PASS before changes.
- The app ships English resources only (`resourceConfigurations = en`). The handoff's "Hindi Home" screenshot and "every shipped locale" strings do not apply. Hindi exists only in the store listing.
- Fonts came from github.com/google/fonts (Plus Jakarta Sans variable 176 KB + DM Mono Medium 50 KB = 226 KB, OFL, under the 250 KB cap).
- Play status of 1.0.0 (1) is not recorded in the repo; Haiku must check before uploading.

## Decisions taken (small, logged not asked)
- Kept package `ui.components` for tokens (`Wiz` object made theme-aware, light/dark state) instead of a new `ui/theme/` package: one change re-themes all 14 screens. Added `AccentInk` (#8F6A00 / #FFD54D) for text/icons; `Accent` (#FFC400) for fills only.
- Launcher label and `app_name` now "Wiz Photo Fit" (manifest uses @string/app_name).
- Snip is code-drawn Compose (no Lottie/Rive). SVG: store-assets/mascot/snip.svg (+ proud variant), PNG 512/1024.
- Splash: static marigold-disc icon (ic_splash.xml) via the system SplashScreen API. NOT DONE: the 400 ms animated-vector "crop marks snap in" (no AVD authored).
- Store art tooling replaced: old tools/StoreArt.java (dark Wiz theme) removed; tools/store-art/ (HTML + headless Chrome, render.sh) is the new pipeline.
- Screenshot 8 is "Light and dark themes" (dark Home) instead of Hindi Home.
- Sample photos in screenshots are illustrated (tools/store-art/sample-*.html), not real people.
- Screenshot 4 headline: "Signatures on clean white paper" (not "sharp": output is only 140x60 px).
- Added Lanczos resampling, 4x signature cleaning and light unsharp (user reported blur). No cloud processing: it would need INTERNET and upload ID photos.
- Face-size check only appears when FaceFinder ran (Work.face/headRatio).
- Recent row shows up to 3 of the last 5 presets on Home.

## Results
- Unit tests: 53 PASS / 0 FAIL (baseline 14). New: PresetCountTest(2), SearchIndexTest(7), ComplianceTest(6), SnipBrainTest(9), RecentsTest(3), ResampleTest(6).
- Release build: `gradle --offline :app:testDebugUnitTest :app:assembleRelease` PASS. app-release-unsigned.apk = 1,894,235 bytes (1.8 MB, limit 2.5 MB).
- Merged manifest: no INTERNET, no ACCESS_NETWORK_STATE (only the AndroidX DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION signature permission, as in 1.0.0).
- Emulator wiz35 (API 35, no Play ML modules), one at a time, release APK signed with the local DEBUG key for testing only.
- Cold start (am start -W, release): fresh install 2.8-4.8 s; after `cmd package compile -m speed-profile` 1.19 / 1.39 / 2.06 s. FAIL against the 1100 ms target (emulator, intro screen on first run). Needs a real-phone check.
- Idle CPU 10 s after touch on Home: 0.0 %. Memory: PSS 30 MB, RSS 134 MB (limit 180 MB): PASS.
- Jank (gfxinfo, scroll Home 10 s): 204 frames, legacy janky 0.49 %, new-metric janky 99 % with 50th percentile 17 ms. Inconclusive on the emulator (frames sit at the 16.7 ms deadline). Not claimed as PASS.
- Functional (emulator): Home, search ("ssc" -> SSC CGL, SSC GD first), exam detail, picker, editor + crop + oval, result + gauge + check card, signature flow + compare slider, ID passport + print sheet, custom builder, dark theme, intro guide: PASS. ModelCard shows worried Snip when Play ML modules are missing: PASS.
- Crash found and fixed during testing: nested verticalScroll on Home "Recent" row.
- NOT TESTED: face auto-fit and background swap on a device with Play services ML modules; TalkBack pass; 200 % font scale; reduce-motion (animator scale 0); RTL (ar); 360x640 and tablet profiles; landscape; API 26/29 runtime; 12 MP and 48 MP timing; 5-preset end-to-end file px/KB check via adb pull (only SSC photo 100x120 21 KB and signature 140x60 11 KB verified in-app by the check card); compress batch; joiner; Hindi; share code round-trip (existing code untouched).
- NOT DONE: tablet 2-column preset grid + editor side panel; AVD splash animation; Hindi translation of new text (listing only); baseline-prof.txt already wildcards the whole package (unchanged).

## Acceptance (handoff section 25)
- [x] PresetCountTest 36 presets / 13 groups / checksum 1985948845:1513
- [ ] 5 presets end-to-end px/KB in saved files: PARTIAL (2 verified, see above)
- [x] N3 search: ssc, neet, passport, pan top 3 (unit tests) + recent row (unit + emulator)
- [x] N4 pass case (emulator) and failing case (unit test)
- [~] N5 slider works; Original/Final buttons are the TalkBack alternative. TalkBack NOT TESTED
- [x] No INTERNET in merged manifest
- [x] Icon text-free + monochrome, label "Wiz Photo Fit", Snip states per section 14 (brain tests)
- [~] Section 19 measured partly (see Results); APK 1.8 MB OK

## Contrast (token pairs, WCAG)
Light: text #15151A on #FAFAF7 ~17.5; muted #62626E on #FFFFFF ~5.9; AccentInk #8F6A00 on #FFFFFF ~5.3; OnAccent #1A1400 on #FFC400 ~12.9; Good #16A34A on white ~3.3 (used for large text/icons only); Bad #DC2626 ~4.8. Dark: text #F3F3F6 on #0F0F12 ~17; AccentInk #FFD54D on #18181D ~11. (Handoff planner numbers, spot-computed, not exhaustively re-verified.)

## Ads-ready (user request, 2026-11-26)
The user wants all apps ad-ready with no ads shown. For Photo Fit that means INTERNET + wiz-ads (OFF) + wiz-update, which contradicts the current "no internet permission" listing, trust pills, privacy policy and Data safety. Not done in 1.1.0. Proposed as 1.2.0 after this release (needs the user's OK on the claim changes).

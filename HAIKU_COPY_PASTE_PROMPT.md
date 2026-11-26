BEGIN HAIKU COPY-PASTE PROMPT
You are Claude Haiku, Release and Publishing Engineer for Wiz Labs (GitHub: therealwiz007).
App: Wiz Photo Fit · package com.wizlabs.photofit · repo https://github.com/therealwiz007/Wiz-Photo-Fit
Local path: /Users/aashish/Desktop/Personal FIles/Claude Code/Wiz-Photo-Fit
Branch to release: redesign/wiz-photo-fit-v1.1.0 @ d1bc348 (plus a docs commit on top: use git log -1) (not yet merged into main)
Version: 1.1.0 (2) · minSdk 26 · targetSdk 36 · compileSdk 36
Previous Play release: 1.0.0 (1) on Closed testing - Alpha (status per docs: 1.0.0 (1) sent to Closed testing - Alpha review 2026-11-14 (India only); later status not recorded)
Privacy policy: https://therealwiz007.github.io/Wiz-Photo-Fit/privacy-policy.html (HTTP 200 on 2026-11-25)
Sonnet results: build PASS, unit tests 53 passed / 0 failed, emulator checklist 12 PASS / 1 FAIL (cold start 1.2-2.1 s compiled vs 1.1 s target) / 13 NOT TESTED (see STATUS.md), release APK 1.8 MB (1,894,235 bytes, unsigned)
Known issues from Sonnet: cold start above target on emulator; face auto-fit and background swap untested on a Play-services device (the emulator lacks the ML modules; the worried Snip card appeared as designed); no tablet layout; no splash animation; Hindi listing lacks new-feature text; ads-ready (INTERNET) not done and would contradict the no-internet claim; the user asked for ads-ready across apps: ask before doing it
Store assets: store-assets/screenshots/{1-result,2-search,3-crop,4-signature,5-passport,6-print,7-custom,8-dark}.png, store-assets/feature-graphic-1024x500.png, store-assets/icon-512.png, fastlane/metadata/android/en-US/{title,short_description,full_description}.txt, changelogs/2.txt, hi-IN/{title,short_description,full_description}.txt
Declarations to re-check: Data safety (no data collected; ML Kit runs on device via Google Play services, which Play may treat as Device IDs/Diagnostics — compare with the current declaration and ask the user before changing), no permissions, Ads = No, target audience 18+, India-only availability

Read first: /Users/aashish/Desktop/Personal FIles/Claude Code/Wiz-Photo-Fit/SONNET_TO_HAIKU.md (complete), then STATUS.md and README.md.

Do these steps in order, and write evidence for each:
1. Read SONNET_TO_HAIKU.md completely.
2. Inspect the repository yourself: git status, git log -5, confirm the branch and SHA above.
3. Spot-check the implementation against the acceptance list in SONNET_TO_HAIKU.md: open at least 5 changed files and confirm the claimed features exist in code.
4. Re-run: ~/.local/opt/gradle-8.11.1/bin/gradle --offline :app:testDebugUnitTest :app:assembleRelease. Report the real counts. Do not copy Sonnet's numbers.
5. Check applicationId, versionCode (must be greater than 1), versionName and targetSdk 36 in app/build.gradle.kts.
6. Verify the signing setup exists (CI secrets KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD are referenced in .github/workflows/android.yml and app/build.gradle.kts). Never print, copy or change secrets or keystores.
7. Produce the signed AAB with the existing authorised path: ask the user for an OK to merge redesign/wiz-photo-fit-v1.1.0 into main (or to run CI on the branch), let GitHub Actions build it, then download the "Wiz Photo Fit (1.1.0)" AAB artifact into dist/ci/ (gitignored). Confirm the signer is CN=Wiz Labs and the versionCode inside it. If CI or secrets are missing, stop and list the exact steps for the user.
8. Verify the store assets: 512x512 icon (text-free, no "Wiz" word), 1024x500 feature graphic, 2-8 phone screenshots in the planned order, each showing real app UI.
9. Verify the listing text: title ≤30 chars, short ≤80, full ≤4000. No "best", "#1", "top", "free" or price/promo words. No claim for a feature that is not in the build.
10. Review the merged-manifest permissions against the Play declarations listed above.
11. Review privacy-policy.html (repo + assets) and Data safety against the code. Never invent a declaration. If anything changed, show the user the proposed answers and wait for an OK.
12. Check App content declarations in Play Console (Data safety (no data collected; ML Kit runs on device via Google Play services, which Play may treat as Device IDs/Diagnostics — compare with the current declaration and ask the user before changing), no permissions, Ads = No, target audience 18+, India-only availability).
13. Play Console work uses the user's Chrome (Claude in Chrome tools), not the built-in browser. If the AAB is >10 MB, split it for upload and join it in the page with the play-uploads-v2 tooling in "/Users/aashish/Desktop/Personal FIles/Claude Code/play-uploads-v2/". Create a release on Closed testing - Alpha named "2 (1.1.0)". Upload the AAB and paste the release notes. Upload the screenshots in order. Save. Do NOT send for review until the user says yes in chat. If an earlier release is still in review, stop and tell the user.
14. After the user's yes: send for review, then verify the status in Publishing overview. Never claim a status you did not see.
15. Production: prepare the promotion steps only. Do not roll out to production without the user's explicit approval in chat. Production also needs 12+ opted-in testers for 14 days on new personal-account apps; report the current count if Play shows it.
16. Write HAIKU_RELEASE_REPORT.md in the repo root: final version, versionCode, build status, AAB location, signing verification, store assets, listing status, testing-track status, compliance checks, outstanding issues, production approval status. Commit it on the release branch with "docs: Haiku release report" and push.

Never: expose secrets, overwrite signing credentials, fabricate test results, invent privacy declarations, claim publishing succeeded without seeing it, bypass Google Play policy, enter passwords (the user does that), or publish to production without explicit approval.
END HAIKU COPY-PASTE PROMPT

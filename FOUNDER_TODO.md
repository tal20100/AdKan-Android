# AdKan Android — what's left for you to do

Things Claude can't do for you (secrets, external consoles, admin-only OS actions,
paid signups). Everything else is being handled in the code directly.

## 1. Supabase credentials (blocks sign-in / any backend call)

**This is a copy-paste, not new setup** — same Supabase project as iOS.

1. Open `C:\Users\Tal\OneDrive\Documents\Studying\code\git-repos\AdKan\config\SupabaseSecrets.plist`
   (the iOS app's real values live here).
2. Open `C:\dev\AdKan-Android\local.properties`.
3. Copy the URL and anon key from the plist into the properties file as:
   ```
   SUPABASE_URL=<paste from the plist>
   SUPABASE_ANON_KEY=<paste from the plist>
   ```

Claude deliberately won't read or copy this value itself, even via a script —
`CLAUDE.md`'s own rule treats any `eyJ*`-format key (which the Supabase anon
key is) as radioactive, no exceptions. This is the one step that's quicker for
you to do by hand than to explain how to automate safely.

**Status: you've filled this in** (confirmed present, values not inspected/logged).

## 2. Google Sign-In setup (blocks real sign-in, currently safely stubbed)

- **Google Cloud Console**: create an OAuth 2.0 **Web application** client ID
  (this is the "server client ID" the Credential Manager flow needs) and an
  **Android** OAuth client registered with package name `com.talhayun.adkan`
  plus your debug/release SHA-1 fingerprint.
- **Supabase Dashboard** → Authentication → Providers → Google: enable it,
  paste in that same Web client ID + its secret.
- Add the **Web** client ID (not the Android one) to `local.properties`:
  ```
  GOOGLE_WEB_CLIENT_ID=xxxxx.apps.googleusercontent.com
  ```

Until this is done, sign-in safely no-ops via a stub — it won't crash, it just
won't do anything real yet.

**Status: unconfirmed whether you've done the Cloud Console + Supabase provider
steps** — `local.properties` has a `GOOGLE_WEB_CLIENT_ID` entry but that alone
doesn't mean the Cloud Console OAuth clients and Supabase provider toggle are
actually set up. Test by tapping "התחברות עם Google" during onboarding.

## 3. Google Play Store — publishing (not free, but cheap)

- **One-time $25 USD registration fee** at
  [play.google.com/console/signup](https://play.google.com/console/signup) —
  no annual renewal (unlike Apple's $99/year). 2026 requires stronger identity
  verification (2-step verification, matching ID/payment/profile details).
- **Real gate you cannot skip**: since Nov 2023, new *personal* developer
  accounts must run a **closed test with 12 distinct testers, opted in
  continuously for 14 days**, before Google allows publishing to production at
  all. Real people on real devices — emulators and duplicate accounts don't
  count. The 14-day clock starts once all 12 have opted in, so line people up
  early (friends/family/beta testers).
- Test track order: **Internal testing** (up to 100 testers, instant, no
  review wait — start here) → **Closed testing** (this is where the 12/14-day
  requirement applies) → **Open testing** (public beta) → production.
- If your Play Console account is an **organization** account (not personal),
  or was created before Nov 13 2023, the 12-tester rule doesn't apply — but a
  fresh signup today will be personal by default.

## 4. One-time environment fixes already applied (informational, no action needed)

- Project moved from the OneDrive-synced path to `C:\dev\AdKan-Android` —
  update any shortcuts/scripts that still point at the old
  `OneDrive\...\AdKan-Android` location.
- Windows Defender exclusions added for the project, `.gradle`, the Android
  SDK, and Android Studio's own folders (via Studio's own prompt) — if you
  ever move the project again, Studio will likely prompt for this again at
  the new path.
- If you ever hit "Unable to delete directory" build errors again: close
  Android Studio, run `./gradlew --stop`, delete `app/build` + `build` +
  `.gradle` folders, reopen Studio for a clean sync. Nothing in `app/build`
  or `.gradle` is source code — always safe to delete.
- Project is now a real git repo, pushed to
  [github.com/tal20100/AdKan-Android](https://github.com/tal20100/AdKan-Android),
  default branch `main`.

## 5. Known gaps (honest current status)

- **All 5 tabs have real screens** — Home, Friends, Groups (with podium),
  Blocking/Focus, Settings. Bottom tab bar matches iOS (same 5 tabs, same
  Hebrew labels, same order, real vector icons — not emoji anymore).
- **Real screen-time data on Home** — today/yesterday minutes come from a real
  `UsageStatsManager` query once you've granted Usage Access; falls back to
  sample numbers before that.
- **Real app-blocking enforcement now exists** — an Accessibility Service
  detects when a selected app opens and shows a full-screen "עד כאן" block
  screen, based on real per-app usage minutes and your toggle settings.
  **This is the one thing that genuinely needs your phone to verify** — see
  `docs/BLOCKING_ENFORCEMENT_TEST_CHECKLIST.md` for the exact test steps and
  known real-device risks (OEM battery optimization, background-activity-launch
  restrictions). Nothing about this could be confirmed working without a
  device — please run through that checklist and report back what happens.
- **Real app-selection picker** — the "בחירת אפליקציות לחסימה" row shows your
  actual installed apps, not a hardcoded count.
- Groups/Friends member lists are still **sample/fake data** — not wired to
  Supabase yet.
- The today/week toggle on Groups now shows a real "פרימיום" upsell snackbar
  when you tap the locked option (previously a silent no-op).
- The "הפעל פוקוס" button on Home now navigates to the Blocking tab for real.
- Onboarding's profile (name + avatar) now persists and Settings reads it back.
- The 3 premium-gated sections on the Blocking screen (hours-based blocking,
  Hard Mode, shield design) are still visual-only locks — no real entitlement
  check exists yet.
- Unit tests exist now (JUnit4) for all pure-logic pieces — none for
  Compose UI or anything requiring a real Android runtime (that would need
  Robolectric or a device, deliberately not introduced).
- Mascot animation curves are approximated (Compose infinite transitions),
  not a pixel-identical port of iOS's SwiftUI spring/easeInOut timing — states,
  thresholds, colors, and art are exact, motion feel is close-but-not-identical.

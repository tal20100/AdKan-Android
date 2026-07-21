# AdKan Android — what's left for you to do

Things Claude can't do for you (secrets, external consoles, admin-only OS actions).
Everything else is being handled in the code directly.

## 1. Supabase credentials (blocks sign-in / any backend call)

Open `local.properties` in the project root yourself (never paste secrets into
chat) and add:

```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

Same values the iOS app already uses — one shared backend, nothing new to create.

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

## 3. One-time environment fixes already applied (informational, no action needed)

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

## 4. Known gaps in the current visual/functional pass (not blocking, just honest status)

- **All 5 tabs now have real screens** — Home, Friends, Groups (with podium),
  Blocking/Focus, and Settings. Bottom tab bar matches iOS exactly (same 5
  tabs, same Hebrew labels, same order).
- **All data everywhere is sample/fake data** — today's minutes, streak,
  leaderboard members, group list, friends list. Nothing is wired to real
  `UsageStatsManager` or Supabase yet. Toggles on the Blocking screen are
  local `remember`d state only — flipping them does nothing real.
- The today/week toggle on Groups and the "הפעל פוקוס" button on Home are
  intentionally non-functional (no-op), matching the screenshots' locked/
  static treatment — not bugs, just not wired up yet.
- The 3 premium-gated sections on the Blocking screen (hours-based blocking,
  Hard Mode, shield design) are visual-only locks — no real entitlement check.
- No unit tests exist yet for this Android project.
- Mascot animation curves are approximated (Compose infinite transitions),
  not a pixel-identical port of iOS's SwiftUI spring/easeInOut timing — states,
  thresholds, colors, and art are exact, motion feel is close-but-not-identical.

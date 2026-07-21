# AdKan Android — task list

## Phase 1 (done this session): visual parity
- [x] Theme tokens: podium colors, LeagueBadge model
- [x] RankedMemberRow shared component
- [x] PodiumView component
- [x] Groups screen (podium + toggle + ranked list + all-groups section)
- [x] Friends screen
- [x] Blocking/Focus screen
- [x] Home screen rework (percentile card, focus button, daily-goal bar)
- [x] StreakCalendarView simplified to match screenshot
- [x] All 5 tabs wired into the bottom nav bar
- [x] Onboarding font/color pass, Home scroll + centering fixes

Everything above is **visual only** — sample/fake data, several elements
intentionally non-functional (no-op) to match static screenshots.

## Phase 2: real functionality, not just visuals

- [x] **Real screen-time data** — `ScreenTimeReader.kt` queries
  `UsageStatsManager` for real today/yesterday foreground minutes. Home
  screen uses it when `UsageAccessPermission.isGranted()`, falls back to
  sample data otherwise (so it still demos before permission is granted).
- [x] **Persist Blocking screen toggle state** — `BlockingPrefs.kt`
  (SharedPreferences), both toggles now survive app restart.
- [x] **Make "בחירת אפליקציות לחסימה" real** — `InstalledApps.kt` queries
  actually-installed launchable apps via `PackageManager`; `AppPickerDialog.kt`
  is a real checklist; selection persists via `BlockingPrefs`; the "N נבחרו"
  caption reflects the real count. Also added the required `<queries>`
  manifest declaration — without it this silently returns nothing on real
  Android 11+ devices.
- [ ] **Make the Groups today/week toggle actually switch data** — even with
  sample data, tapping it should change what's displayed, not just look
  selectable.
- [ ] **Make "הפעל פוקוס" button do something real** — at minimum navigate to
  the Blocking tab; ideally trigger a real focus-session state.
- [ ] **Persist onboarding profile setup** — display name/avatar currently
  vanish on `onComplete()`; save to `SharedPreferences` so Settings/Home can
  read them back.
- [ ] Real Google Sign-In end-to-end test — blocked on you filling in
  `local.properties` + Google Cloud Console setup (see `FOUNDER_TODO.md`).
- [ ] Real app-blocking enforcement (actually stopping usage of selected apps)
  — needs either an Accessibility Service or a foreground polling service;
  not started, genuinely the biggest remaining piece of real functionality.

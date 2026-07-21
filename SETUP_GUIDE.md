# AdKan Android — Setup Guide (zero prior Android knowledge assumed)

## What you're about to do

Install one program (Android Studio), open this folder in it, wait for it to finish setting itself up, create a virtual phone, and press one button to see the app running.

## Step 1 — Install Android Studio

1. Go to `developer.android.com/studio` and download Android Studio for Windows.
2. Run the installer, keep all default options, let it finish.
3. First launch: it may offer a "Setup Wizard" — click through with defaults ("Standard" install type is fine). This downloads the Android SDK (a few GB, can take a while on first run).

## Step 2 — Open this project

1. Open Android Studio.
2. Choose **"Open"** (not "New Project").
3. Navigate to and select this exact folder: `AdKan-Android` (the one containing this file).
4. Android Studio will start "Gradle Sync" automatically — a progress bar at the bottom. **This can take several minutes the first time** (downloading dependencies). Just wait for it to finish. If it asks to use the Gradle wrapper or offers to create one, accept — this project doesn't ship one yet, and Android Studio generates it automatically on first open.
5. If you see a banner asking to update the Android Gradle Plugin or Kotlin version, you can decline for now (the versions in this project are intentionally set) — only update if a sync error tells you to.

## Step 3 — Create a virtual phone (the "emulator")

This is the Android equivalent of the iOS Simulator — a virtual phone that runs on your computer, no physical Android device needed.

1. In Android Studio, open **Tools → Device Manager** (or click the phone icon in the top toolbar).
2. Click **"Create Device"**.
3. Pick any modern phone (e.g. "Pixel 8") → Next.
4. Pick a system image (a version of Android to install on the virtual phone) — pick the one marked **"Recommended"** with the highest API level available → it'll download if needed (another few-minutes wait) → Next → Finish.
5. You now have a virtual phone listed in Device Manager.

## Step 4 — Run the app

1. At the top of Android Studio, make sure your new virtual device is selected in the device dropdown (next to the green "Run" ▶️ button).
2. Click the green **Run ▶️** button (or press Shift+F10).
3. The virtual phone window will open, boot up (first boot takes a minute or two, like a real phone), and the AdKan app will install and launch automatically.

That's it — you're looking at the app.

## What you're looking at right now

This is still a **visual comparison only** — everything shows sample/fake data (not your real screen time, not connected to the real AdKan backend yet) so you can compare the look against the iOS app. On first launch you'll see:

1. **Onboarding** — a stub welcome → "enable Screen Time" ask → two survey questions (hours/day, who you'll compete with) → sign-in step → profile setup (name + emoji avatar). None of these buttons do anything real yet (no Android screen-time permission request, no real sign-in) — they just advance to the next step so you can see the flow and copy.
2. **Home** — the today's-usage card, the streak card (now with the full 5x7 dot-grid streak calendar, not just the number), and a group leaderboard preview, using the exact same colors pulled from the iOS app's design file.
3. **Settings** — tap the "Settings" text button above the Home cards to see it, and "← Back" at the top of Settings to return to Home. Mirrors the iOS Settings screen's sections: sign-in banner, profile, language, appearance, daily goal, permissions, notifications (6 toggles), premium, retake survey, legal links (Privacy Policy / Terms / Contact — these actually open your browser or mail app, they're real links), sign out / delete account (with confirmation dialogs), and version.

**Known gaps in this pass, on purpose, not bugs:**
- No app icon yet (using a placeholder system icon) — a real one can be added later via Android Studio's Image Asset tool.
- The rounded font iOS uses (SF Rounded) has no direct Android equivalent — currently using the system default font. A close substitute font can be added later if it matters to you.
- Onboarding always shows on every app launch — it isn't persisted (no DataStore/SharedPreferences wired up yet), so closing and reopening the app starts you back at the welcome screen.
- Screen navigation is a simple in-memory state switch in `MainActivity.kt`, not a real navigation library (Navigation-Compose) yet — fine for this stage, but there's no system back-button handling between Home and Settings yet.
- All text is English only for now (Hebrew/RTL comes once the layout itself is approved).
- Not connected to real screen-time data or the Supabase backend yet — that's the next phase after you're happy with how this looks.

**Where things live**, if you want to poke around the code:
- `ui/onboarding/OnboardingFlow.kt` — the whole onboarding flow (all steps in one file).
- `ui/home/HomeScreen.kt` + `ui/home/StreakCalendarView.kt` — Home screen and the 5x7 streak dot-grid.
- `ui/settings/SettingsScreen.kt` — the Settings screen.
- `MainActivity.kt` — wires the three together (onboarding → home ↔ settings).

## If something goes wrong

- **Gradle sync fails:** most common cause is no internet connection on first open (it needs to download dependencies). Check your connection and try **File → Sync Project with Gradle Files**.
- **"SDK not found" error:** Android Studio should prompt you to install missing SDK components automatically — click the prompt to fix it, don't try to configure paths manually.
- **Emulator won't start / very slow:** this usually means your computer doesn't have virtualization enabled in the BIOS, or is low on RAM. Not something to debug yourself — flag it and we'll figure out an alternative (a lower-spec virtual device, or eventually testing on a real Android phone via USB instead).

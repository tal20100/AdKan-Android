# Testing real app-blocking enforcement on a real device

This code compiles cleanly (verified with real `./gradlew` builds), but its
actual runtime behavior cannot be verified without your phone — no emulator
or device was available while building this. Please run through this
checklist and report back what actually happens at each step.

## Setup
1. Install the app fresh (or reinstall, so the Accessibility Service starts unregistered).
2. Open the app, go to the **Blocking** tab.
3. You should see a row: "⚠️ החסימה דורשת הפעלת שירות נגישות" with an "הפעלה" button.
4. Tap it — this should open system Settings → Accessibility.
5. Find "עד כאן" in the list, enable it. Android will show a warning dialog about what accessibility services can do — this is standard OS behavior for any accessibility service, not specific to a bug in this app.
6. Return to the app (back button) — the row should now say "✅ החסימה פעילה במכשיר" without you doing anything else (this confirms the re-check-on-resume logic works).

## Core blocking behavior
7. In the Blocking screen, tap "בחירת אפליקציות לחסימה" and select one app you actually use (e.g. Instagram, TikTok, whatever you have installed) — NOT a system app.
8. Make sure "הפעלת חסימה" toggle is ON.
9. Turn ON "חסום תמיד את האפליקציות האלו" (always-block) for the fastest test — this should block immediately, without needing to wait 30 minutes of usage.
10. Open the app you selected. **Expected:** within a second or two, our full-screen "עד כאן" block screen should appear on top of it.
11. Tap "חזרה למסך הבית" — you should land on your phone's home screen, NOT back in the blocked app.
12. Try opening the blocked app again — the block screen should appear again (this tests `ForegroundBlockTracker.reset()` working correctly when you left and came back).

## Threshold-based blocking (not always-block)
13. Turn OFF "חסום תמיד את האפליקציות האלו".
14. Use the selected app for real, until roughly 30 minutes of cumulative usage today (or lower this temporarily for testing if you want — that requires a code change, ask if you want that).
15. **Expected:** once you cross 30 minutes today in that specific app, the next time you open it (or the next foreground-check while it's open), the block screen should appear.

## Things likely to go wrong (real, known risks — report exactly what you see)
- **Delay or no trigger at all:** some phone manufacturers (Samsung, Xiaomi, Huawei especially) aggressively kill background services to save battery, including accessibility services. If blocking doesn't trigger reliably, this is the most likely cause — there may be a manufacturer-specific "allow background activity" / "don't optimize battery" setting needed for "עד כאן" specifically. Report your phone's brand/model if this happens.
- **Block screen doesn't fully cover the blocked app**, or the blocked app is visible behind/around it for a moment.
- **Block screen appears then immediately disappears.**
- **The Settings → Accessibility page doesn't show "עד כאן" in the list at all** (would indicate the manifest/config XML has a real bug, not a device quirk — report this specifically, it's the one outcome that's more likely our bug than a device quirk).

Please report back exactly what happened at each numbered step, including anything that felt slow, glitchy, or wrong even if it "mostly worked."

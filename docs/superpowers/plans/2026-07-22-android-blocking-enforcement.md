# Real App-Blocking Enforcement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Blocking screen's toggles actually block apps, using an Accessibility Service to detect foreground app changes and a full-screen blocking Activity to interrupt blocked apps — replacing the decision-only `BlockingDecisionEngine` (built in the prior Phase 2 plan) with a real, wired enforcement path.

**Architecture:** An `AccessibilityService` receives `TYPE_WINDOW_STATE_CHANGED` events whenever the foreground app changes, reads the new foreground package name off the event, asks `BlockingDecisionEngine.shouldBlock(...)` (existing, unmodified) whether to block it — supplying real per-app usage minutes and the persisted toggle state — and if yes, launches a full-screen `BlockedActivity` on top of it. A small pure `ForegroundBlockTracker` prevents re-launching the block screen repeatedly while the same blocked app stays in the foreground. No `SYSTEM_ALERT_WINDOW` overlay permission is used — launching an Activity needs no extra permission beyond what accessibility services already require.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Android `AccessibilityService` API, `UsageStatsManager` (extending the existing `ScreenTimeReader`), JUnit4 (existing test infra from the prior plan).

## Global Constraints

- Package root: `com.talhayun.adkan`. All new files use `com.talhayun.adkan.<subpackage>`.
- All user-facing strings are Hebrew, matching the tone of existing screens. Do not introduce English UI copy.
- RTL layout: `BlockedActivity` is a separate Activity (not part of the existing Compose tree under `MainActivity`'s forced `LocalLayoutDirection.Rtl`), so it must explicitly wrap its own content in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` too — do not assume it inherits RTL from `MainActivity`.
- Persistence pattern: read-only consumption of the existing `com.talhayun.adkan.ui.blocking.BlockingPrefs` (do not modify its shape) and the existing `com.talhayun.adkan.blocking.BlockingDecisionEngine.shouldBlock(...)` (do not modify its signature or logic — it is already implemented, tested, and reviewed clean; this plan only wires real inputs into it).
- Testing: JUnit4 only. No Robolectric, no instrumented tests, no new test-only dependency. Logic under test must be a plain Kotlin class/object/function with no Android framework type in its signature. `AccessibilityService`, `UsageStatsManager`, and Compose Activities cannot be unit tested in this project's setup — see the honesty note below.
- Do not restyle or reposition any already-built screen from the prior plan (Home, Groups, Friends, Settings, the existing Blocking screen's toggle rows). This plan adds a new screen (`BlockedActivity`) and new background-service wiring; it does not touch prior UI beyond one new settings row (Task 5).
- **Honesty constraint, load-bearing for this plan:** No compiler, emulator, or physical device is available in this environment. Every task's "self-review" step is a manual read-through, not a real build/run verification. `AccessibilityService` behavior in particular — whether Android actually delivers the events reliably, whether OEM battery optimization kills the service in the background, whether the launched Activity reliably appears on top — **can only be confirmed by the founder on a real device.** No task's report or review may claim "confirmed working" for anything beyond "compiles per manual review" and "passes its unit tests" (where unit tests exist). The final task explicitly produces a founder test checklist for exactly this reason.

---

### Task 1: Extend `ScreenTimeReader` with per-app usage minutes

**Context:** `app/src/main/java/com/talhayun/adkan/screentime/ScreenTimeReader.kt` currently only reports *total* device foreground minutes (`todayMinutes`/`yesterdayMinutes`, summed across every app). Real per-app blocking needs *that specific app's* cumulative minutes today — the existing Blocking-screen copy already says "ייחסמו לאחר חצי שעה של שימוש מצטבר" (blocked after half an hour of *cumulative* use), meaning cumulative use of that one app, not total phone time. This task adds that missing per-package query, reusing the existing `aggregateMinutes`/`startOfDay` helpers.

**Files:**
- Modify: `app/src/main/java/com/talhayun/adkan/screentime/ScreenTimeReader.kt`

**Interfaces:**
- Produces: `ScreenTimeReader.todayMinutesForPackage(context: Context, packageName: String): Int` — today's cumulative foreground minutes for one specific package (0 if the package has no usage stats entry for today, e.g. never opened).
- Consumes: nothing from other tasks.

- [ ] **Step 1: Add the per-package function**

In `app/src/main/java/com/talhayun/adkan/screentime/ScreenTimeReader.kt`, add this function inside the `ScreenTimeReader` object, right after the existing `yesterdayMinutes` function:

```kotlin
    /** Today's cumulative foreground minutes for one specific package — 0 if it has no usage today. */
    fun todayMinutesForPackage(context: Context, packageName: String): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0
        val start = startOfDay(daysAgo = 0)
        val end = System.currentTimeMillis()
        val stats = usageStatsManager.queryAndAggregateUsageStats(start, end)
        val millis = stats[packageName]?.totalTimeInForeground ?: 0L
        return (millis / 60_000L).toInt()
    }
```

- [ ] **Step 2: Self-review (no compiler/device available)**

Read the full edited file. Confirm: the new function reuses `startOfDay` (already `private` in this same object, so no import needed), the `stats[packageName]` map lookup uses Kotlin's `Map` `get` operator correctly (returns `UsageStats?`), and the null-safe chain `?.totalTimeInForeground ?: 0L` compiles (that property is a `Long` on `UsageStats`). Confirm brace/paren balance for the whole file.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/screentime/ScreenTimeReader.kt
git commit -m "feat: add per-package usage minutes to ScreenTimeReader"
```

---

### Task 2: `ForegroundBlockTracker` — pure dedup logic

**Context:** `AccessibilityService.onAccessibilityEvent` fires on every window-state change, which can happen multiple times per second while a user scrolls within the same app (unrelated window state changes inside one app still fire events for some UI transitions) or interacts with system UI over a blocked app. Without dedup, the service would try to re-launch `BlockedActivity` repeatedly. This task adds the pure decision of "should I (re-)launch the block screen right now" as its own testable unit.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/blocking/ForegroundBlockTracker.kt`
- Create: `app/src/test/java/com/talhayun/adkan/blocking/ForegroundBlockTrackerTest.kt`

**Interfaces:**
- Produces: `com.talhayun.adkan.blocking.ForegroundBlockTracker` — a stateful (not `object`, must be instantiable per-service-instance) class with one method: `fun shouldLaunchBlockScreen(currentForegroundPackage: String): Boolean`. Internally remembers the last package it launched the block screen for. Returns `true` (and updates its internal memory) only when `currentForegroundPackage` differs from the last package it already launched the block screen for. Returns `false` on repeat calls with the same package. A separate `fun reset()` clears the memory (used when the foreground app is confirmed to have changed to something NOT blocked, so a later return to the same blocked app re-triggers correctly).
- Consumes: nothing from other tasks. This class does not call `BlockingDecisionEngine` itself — the Accessibility Service (Task 4) calls both separately: first `BlockingDecisionEngine.shouldBlock(...)` to decide, then `ForegroundBlockTracker.shouldLaunchBlockScreen(...)` to dedup.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/talhayun/adkan/blocking/ForegroundBlockTrackerTest.kt`:

```kotlin
package com.talhayun.adkan.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForegroundBlockTrackerTest {

    private lateinit var tracker: ForegroundBlockTracker

    @Before
    fun setUp() {
        tracker = ForegroundBlockTracker()
    }

    @Test
    fun `first call for a package returns true`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
    }

    @Test
    fun `repeated calls for the same package return false after the first`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertFalse(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertFalse(tracker.shouldLaunchBlockScreen("com.instagram.android"))
    }

    @Test
    fun `a different package returns true again`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertTrue(tracker.shouldLaunchBlockScreen("com.tiktok.android"))
    }

    @Test
    fun `reset allows the same package to re-trigger`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertFalse(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        tracker.reset()
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.blocking.ForegroundBlockTrackerTest"`
Expected: FAIL — `com.talhayun.adkan.blocking.ForegroundBlockTracker` does not exist yet.

- [ ] **Step 3: Implement the tracker**

Create `app/src/main/java/com/talhayun/adkan/blocking/ForegroundBlockTracker.kt`:

```kotlin
package com.talhayun.adkan.blocking

/**
 * Pure dedup logic for the blocking Accessibility Service: prevents
 * re-launching BlockedActivity repeatedly while the same blocked app stays
 * in the foreground (onAccessibilityEvent can fire many times per second
 * for unrelated window-state changes within one app). One instance lives
 * for the lifetime of the AccessibilityService (see AppBlockAccessibilityService).
 * No Android framework dependency — instantiable and testable on a plain JVM.
 */
class ForegroundBlockTracker {
    private var lastBlockedPackage: String? = null

    fun shouldLaunchBlockScreen(currentForegroundPackage: String): Boolean {
        if (currentForegroundPackage == lastBlockedPackage) return false
        lastBlockedPackage = currentForegroundPackage
        return true
    }

    fun reset() {
        lastBlockedPackage = null
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.blocking.ForegroundBlockTrackerTest"`
Expected: PASS (4 tests, 0 failures)

- [ ] **Step 5: Self-review**

Read the full file. Confirm no Android framework import exists in `ForegroundBlockTracker.kt` (only plain Kotlin), matching the testing constraint.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/blocking/ForegroundBlockTracker.kt app/src/test/java/com/talhayun/adkan/blocking/ForegroundBlockTrackerTest.kt
git commit -m "feat: add pure ForegroundBlockTracker dedup logic for the blocking service"
```

---

### Task 3: `BlockedActivity` — the full-screen block screen

**Context:** When a blocked app is detected, the user needs to see a full-screen interruption they cannot dismiss back into the blocked app. This mirrors iOS's shield screen, whose real default title is `"עד כאן"` (confirmed by reading `App/Settings/ShieldCustomizationView.swift:73`, `App/Models/SharedDefaults.swift:208` in the iOS repo) — the app's own name, meaning "that's enough, stop here." This task builds that screen as a standalone Activity (not part of the existing Compose tree under `MainActivity`).

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/blocking/BlockedActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `com.talhayun.adkan.blocking.BlockedActivity`, a `ComponentActivity`. Reads the blocked package's display name from an `Intent` extra keyed `"blockedAppLabel"` (a `String`, already human-readable — the caller, Task 4, is responsible for resolving the label via `PackageManager` before launching this Activity; `BlockedActivity` itself does no `PackageManager` lookups, keeping it a pure UI layer).
- Consumes: nothing from other tasks' code directly, but is launched by Task 4's service.

- [ ] **Step 1: Add the manifest entry**

In `app/src/main/AndroidManifest.xml`, add this `<activity>` declaration as a sibling of the existing `<activity android:name=".MainActivity" ...>` block, inside `<application>`:

```xml
        <activity
            android:name=".blocking.BlockedActivity"
            android:exported="false"
            android:excludeFromRecents="true"
            android:launchMode="singleTask"
            android:theme="@style/Theme.AdKan" />
```

`excludeFromRecents` keeps this interruption screen out of the recent-apps list (it isn't a real "app" the user navigated to). `singleTask` prevents stacking multiple block screens if the service somehow launches it twice in quick succession despite `ForegroundBlockTracker`'s dedup.

- [ ] **Step 2: Implement the Activity**

Create `app/src/main/java/com/talhayun/adkan/blocking/BlockedActivity.kt`:

```kotlin
package com.talhayun.adkan.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.AdKanTheme
import com.talhayun.adkan.ui.theme.BrandGreen

/**
 * Full-screen interruption shown over a blocked app. Standalone Activity
 * (not part of MainActivity's Compose tree), so it must explicitly force
 * RTL itself rather than inheriting it — see this plan's Global Constraints.
 * No PackageManager lookups here; the caller (AppBlockAccessibilityService)
 * resolves the blocked app's display name and passes it in via Intent extra.
 */
class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLabel = intent.getStringExtra(EXTRA_BLOCKED_APP_LABEL) ?: ""

        setContent {
            AdKanTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AdKanSpacing.screenPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = "🛡️", fontSize = 56.sp)

                            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 16.dp))

                            Text(
                                text = "עד כאן",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )

                            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))

                            Text(
                                text = if (appLabel.isNotBlank()) {
                                    "בחרתם להגביל את $appLabel. הגיע הזמן להתנתק להיום."
                                } else {
                                    "בחרתם להגביל את האפליקציה הזו. הגיע הזמן להתנתק להיום."
                                },
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 32.dp))

                            Button(
                                onClick = { goHome() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            ) {
                                Text(text = "חזרה למסך הבית")
                            }
                        }
                    }
                }
            }
        }
    }

    /** Sends the user to the device home screen — never back into the blocked app. */
    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onBackPressed() {
        // Deliberately does not call super.onBackPressed() / does not finish()
        // back into the blocked app — back button also goes home, matching
        // the primary button's behavior. There is no path from this screen
        // back into the app that triggered it.
        goHome()
    }

    companion object {
        const val EXTRA_BLOCKED_APP_LABEL = "blockedAppLabel"
    }
}
```

- [ ] **Step 3: Self-review (no compiler/device available)**

Read the full file plus the manifest change. Confirm: `AdKanSpacing`, `AdKanTheme`, `BrandGreen` are all imported from their real existing locations (`com.talhayun.adkan.ui.theme.*`, already used elsewhere in this codebase — e.g. `ui/home/HomeScreen.kt` imports the same three), `onBackPressed()` is a valid override for `ComponentActivity` (it is — `ComponentActivity` implements `OnBackPressedDispatcherOwner` and still supports the classic `onBackPressed()` override for this API level range), the manifest's new `<activity>` block is a properly closed self-closing tag and sits inside `<application>...</application>`, and `android:theme="@style/Theme.AdKan"` matches the existing theme name already used by `MainActivity`'s manifest entry (check `AndroidManifest.xml`'s existing `MainActivity` entry for the exact theme resource name before assuming `Theme.AdKan` is correct — copy it exactly if it differs).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/blocking/BlockedActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add full-screen BlockedActivity for app-blocking enforcement"
```

---

### Task 4: `AppBlockAccessibilityService` — the enforcement wiring

**Context:** This is the task that makes blocking real: an `AccessibilityService` that listens for foreground-app changes, and for each one, asks the existing `BlockingDecisionEngine.shouldBlock(...)` (with real inputs from `BlockingPrefs`, Task 1's new `ScreenTimeReader.todayMinutesForPackage`, and Task 2's `ForegroundBlockTracker`) whether to launch Task 3's `BlockedActivity`.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/blocking/AppBlockAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml` (create this file if it doesn't already exist — check first; this project may currently only use hardcoded Compose string literals with no `strings.xml` at all, in which case create it fresh with just the two keys below)

**Interfaces:**
- Produces: `com.talhayun.adkan.blocking.AppBlockAccessibilityService`, registered in the manifest, requiring manual user enablement via system Settings (no runtime dialog exists for `BIND_ACCESSIBILITY_SERVICE`, same permission-model shape as `PACKAGE_USAGE_STATS` — see `com.talhayun.adkan.permissions.UsageAccessPermission` for the established pattern this plan's Task 5 will mirror for the *checking* side; this task only builds the service itself).
- Consumes: `com.talhayun.adkan.blocking.BlockingDecisionEngine.shouldBlock(...)` (existing, unmodified), `com.talhayun.adkan.ui.blocking.BlockingPrefs.isBlockingEnabled/isAlwaysBlockEnabled/selectedApps` (existing, unmodified), `com.talhayun.adkan.screentime.ScreenTimeReader.todayMinutesForPackage` (Task 1), `com.talhayun.adkan.blocking.ForegroundBlockTracker` (Task 2), `com.talhayun.adkan.blocking.BlockedActivity.EXTRA_BLOCKED_APP_LABEL` (Task 3).

- [ ] **Step 1: Check whether `strings.xml` exists**

Run: `test -f app/src/main/res/values/strings.xml && echo EXISTS || echo MISSING`

If `MISSING`, create `app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="accessibility_service_description">עד כאן משתמש/ת בשירות הנגישות רק כדי לזהות מתי אפליקציה שנבחרה לחסימה נפתחת. שום מידע על השימוש שלך לא עוזב את המכשיר.</string>
</resources>
```

If `EXISTS`, add just the one `<string name="accessibility_service_description">...</string>` line (same text as above) inside the existing `<resources>` block, without removing any existing strings there.

- [ ] **Step 2: Create the accessibility service config XML**

Create `app/src/main/res/xml/accessibility_service_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="false"
    android:packageNames="" />
```

`android:packageNames=""` intentionally does NOT restrict which packages this service receives events for — it must observe every app to detect when a blocked one opens, not just its own. `canRetrieveWindowContent="false"` because this service only reads the event's package name, never inspects UI hierarchy/content of other apps — an explicit, honest privacy boundary matching this project's existing "only dailyTotalMinutes crosses the network" discipline (see the iOS CLAUDE.md's privacy-boundary rule) even though this is a purely local, non-networked service.

- [ ] **Step 3: Implement the service**

Create `app/src/main/java/com/talhayun/adkan/blocking/AppBlockAccessibilityService.kt`:

```kotlin
package com.talhayun.adkan.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.talhayun.adkan.screentime.ScreenTimeReader
import com.talhayun.adkan.ui.blocking.BlockingPrefs

/**
 * Detects foreground-app changes (TYPE_WINDOW_STATE_CHANGED) and launches
 * BlockedActivity over any app the founder selected for blocking, once its
 * cumulative usage today crosses the threshold (or immediately, if
 * "always block" is on) — see BlockingDecisionEngine for the exact rule.
 *
 * IMPORTANT — cannot be verified from this environment (no device/emulator
 * available here): whether Android reliably delivers these events in the
 * background, whether OEM battery optimization suspends this service, and
 * whether the launched Activity reliably draws on top of the blocked app
 * are all real-device concerns this implementation cannot confirm. See the
 * plan's final founder-testing task.
 */
class AppBlockAccessibilityService : AccessibilityService() {

    private val tracker = ForegroundBlockTracker()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        val selectedApps = BlockingPrefs.selectedApps(this)
        if (packageName !in selectedApps) {
            tracker.reset()
            return
        }

        val shouldBlock = BlockingDecisionEngine.shouldBlock(
            packageName = packageName,
            selectedApps = selectedApps,
            blockingEnabled = BlockingPrefs.isBlockingEnabled(this),
            alwaysBlockEnabled = BlockingPrefs.isAlwaysBlockEnabled(this),
            todayForegroundMinutes = ScreenTimeReader.todayMinutesForPackage(this, packageName),
        )

        if (shouldBlock && tracker.shouldLaunchBlockScreen(packageName)) {
            launchBlockedActivity(packageName)
        }
    }

    private fun launchBlockedActivity(packageName: String) {
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }

        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(BlockedActivity.EXTRA_BLOCKED_APP_LABEL, label)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override, no cleanup needed — this service holds no
        // resources beyond the in-memory ForegroundBlockTracker.
    }
}
```

- [ ] **Step 4: Register the service in the manifest**

In `app/src/main/AndroidManifest.xml`, add this `<service>` declaration as a sibling of the `<activity android:name=".MainActivity" ...>` block (and Task 3's `BlockedActivity` entry), inside `<application>`:

```xml
        <service
            android:name=".blocking.AppBlockAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true"
            android:label="עד כאן">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
```

`android:exported="true"` is required here specifically because the system (not this app) binds to accessibility services — this is the one correct, narrow exception to "don't export components," gated by the `BIND_ACCESSIBILITY_SERVICE` permission which only the system holds.

- [ ] **Step 5: Self-review (no compiler/device available — this is the most important review in this plan)**

Read the full service file, the full config XML, and the manifest diff. Confirm specifically:
1. `event.packageName` is a `CharSequence?` — the `?.toString()` conversion and null-coalescing `?: return` are both present and correctly ordered.
2. The early-return guard `if (packageName == applicationContext.packageName) return` exists — without it, this app's own screens (including `BlockedActivity` itself, and the Blocking screen where the user picks apps) could theoretically trigger recursive/self-blocking logic if `com.talhayun.adkan` ever ended up in `selectedApps` (it can't currently, since `InstalledApps.kt`'s `installedLaunchableApps` already excludes this app's own package — but this guard is cheap, correct defense-in-depth, keep it).
3. `tracker.reset()` is called when the foreground app is NOT in `selectedApps` — this is required for the dedup logic to correctly re-trigger the next time the user returns to a blocked app after visiting something else; verify this call site exists and is not accidentally only inside the blocked-app branch.
4. Every import resolves to a real, existing symbol: `com.talhayun.adkan.screentime.ScreenTimeReader` (Task 1), `com.talhayun.adkan.ui.blocking.BlockingPrefs` (already existed pre-plan), `BlockingDecisionEngine`/`ForegroundBlockTracker`/`BlockedActivity` (same package, `com.talhayun.adkan.blocking`, no import needed for those three).
5. Brace/paren balance across the service file.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/blocking/AppBlockAccessibilityService.kt app/src/main/res/xml/accessibility_service_config.xml app/src/main/res/values/strings.xml app/src/main/AndroidManifest.xml
git commit -m "feat: add AppBlockAccessibilityService to wire real blocking enforcement"
```

(If `strings.xml` already existed and was only appended to rather than created, that's still the correct file to stage — `git add` the same path either way.)

---

### Task 5: Settings — accessibility permission check + status row

**Context:** `BIND_ACCESSIBILITY_SERVICE` has no runtime permission dialog — same shape as `PACKAGE_USAGE_STATS`, which `com.talhayun.adkan.permissions.UsageAccessPermission` already solved with a `checkOpNoThrow`-style check plus a Settings deep-link. This task adds the equivalent for the accessibility service, and surfaces it as a status row so the founder (and later, real users) can tell whether enforcement is actually active — without this, a user could select apps to block, flip "הפעלת חסימה" on, and see nothing happen, with no way to tell why.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/permissions/AccessibilityServicePermission.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt`

**Interfaces:**
- Produces: `com.talhayun.adkan.permissions.AccessibilityServicePermission.isEnabled(context: Context): Boolean`, `com.talhayun.adkan.permissions.AccessibilityServicePermission.settingsIntent(): Intent`.
- Consumes: nothing from other tasks directly (does not reference `AppBlockAccessibilityService` by class — see Step 1 for why).

- [ ] **Step 1: Implement the permission checker**

Create `app/src/main/java/com/talhayun/adkan/permissions/AccessibilityServicePermission.kt`:

```kotlin
package com.talhayun.adkan.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * Checks whether AppBlockAccessibilityService is currently enabled by the
 * user in system settings. Mirrors UsageAccessPermission's shape (no
 * runtime dialog exists for BIND_ACCESSIBILITY_SERVICE either — the only
 * path is Settings.ACTION_ACCESSIBILITY_SETTINGS, checked on return-to-app).
 * Reads the enabled-services list as a colon-separated string per Android's
 * documented Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES format, rather
 * than referencing AppBlockAccessibilityService's class object directly —
 * this keeps this file dependency-free from the blocking package, matching
 * this project's existing separation between permissions/ and its subjects.
 */
object AccessibilityServicePermission {

    private const val SERVICE_ID = "com.talhayun.adkan/com.talhayun.adkan.blocking.AppBlockAccessibilityService"

    fun isEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (component in splitter) {
            if (component.equals(SERVICE_ID, ignoreCase = true)) return true
        }
        return false
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
```

- [ ] **Step 2: Add a status row to BlockingScreen**

Read the current full content of `app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt` first — it has been edited by prior tasks in the earlier Phase 2 plan and its exact current line numbers may not match what you'd assume from memory.

Add these two imports:

```kotlin
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.talhayun.adkan.permissions.AccessibilityServicePermission
```

Inside the `BlockingScreen` composable function, add this state and re-check logic right after the existing `val context = LocalContext.current` line:

```kotlin
    var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityServicePermission.isEnabled(context)) }
    val accessibilitySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        isAccessibilityEnabled = AccessibilityServicePermission.isEnabled(context)
    }
```

Add this new composable row, called from inside `BlockingScreen`'s `Column` right after the `HeroCard()` call (before `FocusTimerRow()`):

```kotlin
        AccessibilityStatusRow(
            isEnabled = isAccessibilityEnabled,
            onEnableClick = { accessibilitySettingsLauncher.launch(AccessibilityServicePermission.settingsIntent()) },
        )
```

Add the new composable function itself, anywhere among the other private composables in this file (e.g. right after `FocusTimerRow`):

```kotlin
@Composable
private fun AccessibilityStatusRow(isEnabled: Boolean, onEnableClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isEnabled) "✅ " else "⚠️ ", fontSize = 16.sp)
            Text(
                text = if (isEnabled) "החסימה פעילה במכשיר" else "החסימה דורשת הפעלת שירות נגישות",
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!isEnabled) {
            androidx.compose.material3.TextButton(onClick = onEnableClick) {
                Text(text = "הפעלה")
            }
        }
    }
}
```

- [ ] **Step 3: Self-review (no compiler/device available)**

Read the full edited `BlockingScreen.kt`. Confirm: `rememberCoroutineScope`/other imports from prior tasks are untouched, no duplicate import of anything already present, `AccessibilityStatusRow` call site sits inside the same `Column` as the other rows (not accidentally outside it), and the two new imports (`rememberLauncherForActivityResult`, `ActivityResultContracts`) aren't already imported under a different form elsewhere in the file (check — `OnboardingFlow.kt` uses this exact pattern already for `PermissionStep`, so the import path is confirmed correct by precedent).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/permissions/AccessibilityServicePermission.kt app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt
git commit -m "feat: add accessibility-service status row and permission check to Blocking screen"
```

---

### Task 6: Founder device-testing checklist (documentation, not code)

**Context:** Per this plan's Global Constraints, nothing in Tasks 1-5 can be confirmed actually working without a real device — accessibility services, foreground detection reliability, and Activity-launch-over-another-app behavior are all real-device concerns. This task's deliverable is a clear, honest test checklist handed to the founder, not more code.

**Files:**
- Create: `docs/BLOCKING_ENFORCEMENT_TEST_CHECKLIST.md`

- [ ] **Step 1: Write the checklist**

Create `docs/BLOCKING_ENFORCEMENT_TEST_CHECKLIST.md`:

```markdown
# Testing real app-blocking enforcement on a real device

This cannot be verified without your phone — no emulator or compiler was
available while building this. Please run through this checklist and report
back what actually happens at each step.

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
```

- [ ] **Step 2: Commit**

```bash
git add docs/BLOCKING_ENFORCEMENT_TEST_CHECKLIST.md
git commit -m "docs: add founder device-testing checklist for real blocking enforcement"
```

---

## Explicitly out of scope for this plan

- Any UI/UX beyond the one new Settings status row and the `BlockedActivity` screen itself — no changes to the app-selection picker, no changes to toggle rows beyond what Task 5 adds.
- Handling OEM-specific "don't kill this app" settings programmatically — flagged as a real risk in Task 6's checklist for the founder to discover and report, not solved in code (there is no universal API for this across manufacturers).
- Any attempt to make the service more resilient to being killed (e.g. `START_STICKY` foreground service tricks, WorkManager re-registration) — if Task 6's real-device testing reveals the service gets killed, that's a follow-up plan informed by what actually happens, not something to guess at now.
- Migrating away from Accessibility Service to a different enforcement mechanism (e.g. `UsageStatsManager` polling via a foreground Service, like the "Zenith" reference app researched earlier this project) — Accessibility Service was chosen for this plan because it reacts to window changes immediately rather than polling on an interval; revisit only if Task 6 reveals it's unreliable in practice.

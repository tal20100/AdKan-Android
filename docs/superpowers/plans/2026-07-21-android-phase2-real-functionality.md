# Android Phase 2: Real Functionality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the remaining non-functional/no-op UI elements in the AdKan Android app with real, testable behavior — a working premium-upsell signal on the locked Groups range toggle, real tab navigation from the Home screen's Focus button, persisted onboarding profile data, and a pure decision-logic engine for app blocking.

**Architecture:** Each task extracts one piece of previously-inline or previously-missing logic into a small, plain-Kotlin, unit-testable unit (no Android `Context`/framework dependency where avoidable), then wires it into the existing Compose screens with minimal changes to those screens' visual layout. Persistence follows the existing `BlockingPrefs`/`AuthService` pattern: plain `SharedPreferences` via a singleton object, not DataStore.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), plain `SharedPreferences`, JUnit4 (new — this project currently has zero test infrastructure).

## Global Constraints

- Package root: `com.talhayun.adkan`. All new files use `com.talhayun.adkan.<subpackage>`.
- All user-facing strings are Hebrew, matching the tone of existing screens (e.g. `"בחירת אפליקציות לחסימה"`, `"פרימיום"`). Do not introduce English UI copy.
- RTL layout is already forced app-wide in `MainActivity.kt` via `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` — do not add a second RTL provider anywhere else.
- Persistence pattern: plain `SharedPreferences` accessed through a singleton `object` with `context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)`, mirroring `com.talhayun.adkan.ui.blocking.BlockingPrefs` exactly (already in the codebase — read it before writing new prefs objects). Do not introduce DataStore.
- Testing: JUnit4 only. No Robolectric, no instrumented (`androidx.test`) tests, no new test-only dependency beyond `junit:junit:4.13.2`. Every task that adds logic must extract that logic into a plain Kotlin class/object/function that takes no Android framework type (`Context`, `SharedPreferences`, etc.) as a parameter, so it is runnable as a pure JVM unit test. Thin Android-framework-dependent wrapper code (actual `SharedPreferences` reads/writes, `@Composable` functions) is not itself unit tested — this matches the existing codebase's `BlockingPrefs`, which has no tests today.
- Do not restyle or reposition any already-built screen. These tasks add behavior to existing UI elements; the visual layout from the prior pass stays as-is.
- **Real app-blocking enforcement (an Accessibility Service or overlay that actually stops app usage) is explicitly OUT OF SCOPE for this plan.** Task 4 builds only the decision logic (`shouldBlock(...)` style pure function) that a future enforcement mechanism would call. Do not attempt to wire real enforcement in this plan — that is a separate, larger piece of work requiring its own plan.
- Every task ends with a working `./gradlew test` run for the new test file, plus a full-file read-through self-review (no compiler/emulator is available to the implementer — manual correctness review substitutes for a build check on the Compose-touching files).

---

### Task 1: JUnit test infrastructure + real premium-upsell signal on the locked Groups range toggle

**Context:** `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt` currently has a private `GroupsRange` enum (`WEEK` locked, `TODAY` unlocked) and a `RangeToggle` composable that renders both pills but never wires a click handler — tapping either does nothing at all. The real screenshots this was built from show `WEEK` behind a lock icon; tapping it should surface that it's premium-gated (e.g. via a Snackbar), not silently do nothing. Tapping `TODAY` should stay a no-op only in the sense that there's nothing to switch to yet (only one dataset exists) — but the tap itself must register.

Note for the implementer/reviewer: Step 6 below wraps `GroupsScreen`'s content in its own `Scaffold` purely to host a `SnackbarHostState` — this is intentional, not an accidental structural change. `GroupsScreen()` is already rendered inside `MainActivity.kt`'s own outer `Scaffold` (which owns the bottom nav bar), but that outer Scaffold has no `SnackbarHost` and lifting the snackbar state up to `MainActivity` would mean threading it through every screen for one feature. Nesting a content-only `Scaffold` (no `topBar`/`bottomBar`) here is the standard, low-risk way to add a screen-local Snackbar in Compose — it won't double-reserve system-bar insets since the outer Scaffold already consumed those.

**Files:**
- Modify: `app/build.gradle.kts` (add the JUnit test dependency — this project has no test infrastructure yet)
- Create: `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsRange.kt` (extracted, testable enum + pure function)
- Create: `app/src/test/java/com/talhayun/adkan/ui/groups/GroupsRangeTest.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt` (remove the private enum, import the new one, wire real click handling + a Snackbar-style message)

**Interfaces:**
- Produces: `com.talhayun.adkan.ui.groups.GroupsRange` enum with `label: String`, `locked: Boolean` properties (same shape as today) plus a new function `fun upsellMessage(): String?` returning a non-null Hebrew message only when `locked == true`, else `null`.
- Consumes: nothing from other tasks.

- [ ] **Step 1: Add the JUnit dependency**

Open `app/build.gradle.kts` and add this line inside the existing `dependencies { ... }` block, right after the `security-crypto` line:

```kotlin
    // Test-only — plain JUnit4, no Robolectric/instrumented tests (see plan's Global Constraints).
    testImplementation("junit:junit:4.13.2")
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/talhayun/adkan/ui/groups/GroupsRangeTest.kt`:

```kotlin
package com.talhayun.adkan.ui.groups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class GroupsRangeTest {

    @Test
    fun `today is not locked and has no upsell message`() {
        assertEquals(false, GroupsRange.TODAY.locked)
        assertNull(GroupsRange.TODAY.upsellMessage())
    }

    @Test
    fun `week is locked and has a non-null Hebrew upsell message`() {
        assertEquals(true, GroupsRange.WEEK.locked)
        assertNotNull(GroupsRange.WEEK.upsellMessage())
        assertEquals("צפייה בנתוני השבוע דורשת מנוי פרימיום", GroupsRange.WEEK.upsellMessage())
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.ui.groups.GroupsRangeTest"`
Expected: FAIL — `GroupsRange` is not yet a top-level type in its own file with an `upsellMessage()` function (compile error, since the enum currently lives privately inside `GroupsScreen.kt` with no such function).

- [ ] **Step 4: Create the extracted, testable enum**

Create `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsRange.kt`:

```kotlin
package com.talhayun.adkan.ui.groups

/**
 * Extracted from GroupsScreen.kt so it's unit-testable on its own (see
 * GroupsRangeTest). WEEK is premium-gated per the real screenshots (shown
 * behind a lock icon); upsellMessage() is what the UI shows when a user taps
 * a locked range instead of silently doing nothing.
 */
enum class GroupsRange(val label: String, val locked: Boolean) {
    WEEK("השבוע", locked = true),
    TODAY("היום", locked = false);

    fun upsellMessage(): String? = if (locked) "צפייה בנתוני השבוע דורשת מנוי פרימיום" else null
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.ui.groups.GroupsRangeTest"`
Expected: PASS (2 tests, 0 failures)

- [ ] **Step 6: Wire real click handling into GroupsScreen.kt**

In `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt`:

1. Delete the existing private enum block at the bottom of the file:

```kotlin
private enum class GroupsRange(val label: String, val locked: Boolean) {
    WEEK("השבוע", locked = true),
    TODAY("היום", locked = false),
}
```

2. Add these imports near the top (alongside the existing `androidx.compose.material3.*` imports):

```kotlin
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
```

3. Replace the `GroupsScreen` function body to host a `SnackbarHostState` and pass a real click callback down to `RangeToggle`:

```kotlin
@Composable
fun GroupsScreen() {
    var selectedRange by remember { mutableStateOf(GroupsRange.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(AdKanSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AdKanSpacing.cardSpacing),
        ) {
            Text(text = "קבוצות", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐ ", fontSize = 16.sp)
                Text(text = "החברי'ה", style = CardTitle)
            }

            PodiumView(
                entries = sampleGroupMembers.take(3).map {
                    PodiumEntry(
                        id = it.id,
                        displayName = it.name,
                        avatarEmoji = it.emoji,
                        minutes = it.minutes,
                        streak = it.streak,
                        leagueBadge = it.badge,
                        rank = it.rank,
                        isCurrentUser = it.isCurrentUser,
                    )
                },
            )

            RangeToggle(
                selected = selectedRange,
                onSelect = { tapped ->
                    val upsell = tapped.upsellMessage()
                    if (upsell != null) {
                        scope.launch { snackbarHostState.showSnackbar(upsell) }
                    } else {
                        selectedRange = tapped
                    }
                },
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sampleGroupMembers.forEach { member ->
                    RankedMemberRow(
                        rank = member.rank,
                        avatarEmoji = member.emoji,
                        displayName = member.name,
                        formattedMinutes = formatMinutesHebrew(member.minutes),
                        hasMinutesData = member.minutes > 0,
                        minutesColor = minutesColor(member.minutes, sampleGroupGoal),
                        streak = member.streak,
                        badge = member.badge,
                        isCurrentUser = member.isCurrentUser,
                        wins = member.wins,
                    )
                }
            }

            Text(text = "כל הקבוצות", style = CardTitle)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sampleOtherGroups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${group.emoji} ", fontSize = 18.sp)
                            Text(text = group.name, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = "${group.memberCount} חברים",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
```

4. Make `RangeToggle`'s rows actually clickable — replace its body with:

```kotlin
@Composable
private fun RangeToggle(selected: GroupsRange, onSelect: (GroupsRange) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GroupsRange.entries.forEach { range ->
            val isSelected = range == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelect(range) }
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (range.locked) {
                    Text(text = "🔒 ", fontSize = 12.sp)
                }
                Text(
                    text = range.label,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (range.locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
```

5. Add the `clickable` import at the top of the file:

```kotlin
import androidx.compose.foundation.clickable
```

- [ ] **Step 7: Self-review the whole file (no compiler available)**

Read the full, current `GroupsScreen.kt` top to bottom. Confirm: every import is used, no leftover reference to the deleted private `GroupsRange` enum, brace/paren counts balance (`grep -o "{" file | wc -l` should equal `grep -o "}" file | wc -l`, same for parens), and `RangeToggle`'s call site in `GroupsScreen()` still matches its new signature (`selected`, `onSelect`).

- [ ] **Step 8: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/talhayun/adkan/ui/groups/GroupsRange.kt app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt app/src/test/java/com/talhayun/adkan/ui/groups/GroupsRangeTest.kt
git commit -m "feat: real premium-upsell Snackbar on locked Groups week toggle"
```

---

### Task 2: Focus button navigates to the Blocking tab

**Context:** `app/src/main/java/com/talhayun/adkan/MainActivity.kt` has a private `MainTab` enum (`HOME, FRIENDS, GROUPS, BLOCKING, SETTINGS`) and an `AppRoot` composable holding `var tab by remember { mutableStateOf(MainTab.HOME) }`. `app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt`'s `FocusButton` composable currently has `onClick = { /* no-op */ }`. This task makes tapping "הפעל פוקוס" on Home actually switch the bottom tab to Blocking.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/MainTab.kt` (extracted, testable enum)
- Create: `app/src/test/java/com/talhayun/adkan/MainTabTest.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/MainActivity.kt` (remove the private enum, import the new one, thread a navigation callback into `HomeScreen`)
- Modify: `app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt` (`HomeScreen` and `FocusButton` take an `onFocusClick: () -> Unit` parameter)

**Interfaces:**
- Produces: `com.talhayun.adkan.MainTab` enum with `label: String`, `emoji: String` (same shape as today).
- Consumes: nothing from other tasks.
- Note for Task 3/4 implementers (if dispatched after this task lands): `HomeScreen()`'s signature changes from `fun HomeScreen()` to `fun HomeScreen(onFocusClick: () -> Unit)` — any other call site must pass this argument.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/talhayun/adkan/MainTabTest.kt`:

```kotlin
package com.talhayun.adkan

import org.junit.Assert.assertEquals
import org.junit.Test

class MainTabTest {

    @Test
    fun `entries are in Home, Friends, Groups, Blocking, Settings order`() {
        assertEquals(
            listOf(MainTab.HOME, MainTab.FRIENDS, MainTab.GROUPS, MainTab.BLOCKING, MainTab.SETTINGS),
            MainTab.entries.toList(),
        )
    }

    @Test
    fun `each tab has a non-blank Hebrew label and emoji`() {
        MainTab.entries.forEach { tab ->
            assert(tab.label.isNotBlank()) { "${tab.name} has a blank label" }
            assert(tab.emoji.isNotBlank()) { "${tab.name} has a blank emoji" }
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.MainTabTest"`
Expected: FAIL — `MainTab` is currently `private` inside `MainActivity.kt`, not visible to a test in a different file.

- [ ] **Step 3: Extract MainTab into its own file**

Create `app/src/main/java/com/talhayun/adkan/MainTab.kt`:

```kotlin
package com.talhayun.adkan

// Extracted from MainActivity.kt so it's unit-testable on its own (see
// MainTabTest) and importable from ui/home/HomeScreen.kt for the Focus-button
// navigation wiring (Task 2 of the Phase 2 plan).
enum class MainTab(val label: String, val emoji: String) {
    HOME("בית", "🏠"),
    FRIENDS("חברים", "👥"),
    GROUPS("קבוצות", "🏆"),
    BLOCKING("פוקוס", "🛡️"),
    SETTINGS("הגדרות", "⚙️"),
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.MainTabTest"`
Expected: PASS (2 tests, 0 failures)

- [ ] **Step 5: Remove the private enum from MainActivity.kt and wire the callback**

In `app/src/main/java/com/talhayun/adkan/MainActivity.kt`:

1. Delete this block (it now lives in `MainTab.kt`):

```kotlin
// [SKILL-DECL] Ported from iOS App/RootView.swift's MainTabView — same 5 tabs,
// same order, same Hebrew labels pulled directly from App/Localizable.xcstrings
// (tab.home/tab.friends/tab.leaderboard/tab.blocking/tab.settings). iOS uses SF
// Symbols (house.fill, person.2.fill, trophy.fill, shield.checkered,
// gearshape.fill) for tab icons; Android's core Material icon set doesn't
// include people/trophy/shield equivalents without the material-icons-extended
// dependency, so emoji are used instead — consistent with this codebase's
// existing emoji-icon style already used throughout HomeScreen.kt (🎯, 🏆, 🔥).
private enum class MainTab(val label: String, val emoji: String) {
    HOME("בית", "🏠"),
    FRIENDS("חברים", "👥"),
    GROUPS("קבוצות", "🏆"),
    BLOCKING("פוקוס", "🛡️"),
    SETTINGS("הגדרות", "⚙️"),
}
```

2. In the `when (tab)` block inside `AppRoot`, change the `MainTab.HOME` line from:

```kotlin
                    MainTab.HOME -> HomeScreen()
```

to:

```kotlin
                    MainTab.HOME -> HomeScreen(onFocusClick = { tab = MainTab.BLOCKING })
```

- [ ] **Step 6: Thread the callback through HomeScreen.kt**

In `app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt`:

1. Change the `HomeScreen` function signature and its call to `FocusButton`:

```kotlin
@Composable
fun HomeScreen(onFocusClick: () -> Unit) {
```

(only the signature line changes — the body stays the same except the `FocusButton()` call, below)

2. Change the `FocusButton()` call inside the `Column` to:

```kotlin
        FocusButton(onClick = onFocusClick)
```

3. Change the `FocusButton` composable itself from:

```kotlin
@Composable
private fun FocusButton() {
    Button(
        onClick = { /* no-op — Focus tab isn't wired to real functionality yet */ },
```

to:

```kotlin
@Composable
private fun FocusButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
```

(the rest of the `Button { ... }` block — modifier, colors, shape, the `Text` inside — is unchanged)

- [ ] **Step 7: Self-review the whole diff (no compiler available)**

Read the full current `MainActivity.kt` and `HomeScreen.kt`. Confirm: `MainTab` is imported implicitly (same package, no import statement needed since `MainTab.kt` and `MainActivity.kt` are both in `com.talhayun.adkan`), `HomeScreen`'s only call site (`MainActivity.kt`'s `when (tab)` block) passes the new argument, and there is no remaining reference to a no-op `FocusButton()` with zero arguments anywhere in `HomeScreen.kt`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/MainTab.kt app/src/main/java/com/talhayun/adkan/MainActivity.kt app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt app/src/test/java/com/talhayun/adkan/MainTabTest.kt
git commit -m "feat: Home screen's Focus button navigates to the Blocking tab"
```

---

### Task 3: Persist onboarding profile (display name + avatar emoji)

**Context:** `app/src/main/java/com/talhayun/adkan/ui/onboarding/OnboardingFlow.kt`'s `ProfileSetupStep` collects `displayName` and `selectedEmoji` into local `remember`d state, then calls `onComplete()` — the values are never saved anywhere and vanish immediately. `app/src/main/java/com/talhayun/adkan/ui/settings/SettingsScreen.kt` separately has its own `displayName`/`avatarEmoji` local state that always starts empty (`""` / `"😎"`), showing a "הגדירו שם" placeholder — it has no way to know what the user entered during onboarding. This task adds a shared, persisted `Profile` so both screens agree.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/onboarding/Profile.kt` (pure, testable data class + defaulting logic)
- Create: `app/src/main/java/com/talhayun/adkan/onboarding/ProfilePrefs.kt` (SharedPreferences wrapper, mirrors `BlockingPrefs`)
- Create: `app/src/test/java/com/talhayun/adkan/onboarding/ProfileTest.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/onboarding/OnboardingFlow.kt` (`ProfileSetupStep` saves via `ProfilePrefs` before calling `onComplete`)
- Modify: `app/src/main/java/com/talhayun/adkan/ui/settings/SettingsScreen.kt` (initial `displayName`/`avatarEmoji` state loads from `ProfilePrefs`)

**Interfaces:**
- Produces: `com.talhayun.adkan.onboarding.Profile(displayName: String, avatarEmoji: String)` data class with a companion `Profile.default(): Profile` (returns `Profile("", "😎")` — matches `SettingsScreen`'s current hardcoded defaults exactly, so behavior for a user who hasn't onboarded yet doesn't change). `com.talhayun.adkan.onboarding.ProfilePrefs` object with `fun load(context: Context): Profile` and `fun save(context: Context, profile: Profile)`.
- Consumes: nothing from other tasks.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/talhayun/adkan/onboarding/ProfileTest.kt`:

```kotlin
package com.talhayun.adkan.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {

    @Test
    fun `default profile has an empty name and the default emoji`() {
        val profile = Profile.default()
        assertEquals("", profile.displayName)
        assertEquals("😎", profile.avatarEmoji)
    }

    @Test
    fun `a profile with a real name is not equal to the default`() {
        val profile = Profile(displayName = "טל", avatarEmoji = "🐸")
        assert(profile != Profile.default())
        assertEquals("טל", profile.displayName)
        assertEquals("🐸", profile.avatarEmoji)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.onboarding.ProfileTest"`
Expected: FAIL — `com.talhayun.adkan.onboarding.Profile` does not exist yet.

- [ ] **Step 3: Create the pure data class**

Create `app/src/main/java/com/talhayun/adkan/onboarding/Profile.kt`:

```kotlin
package com.talhayun.adkan.onboarding

/**
 * Shared between OnboardingFlow's ProfileSetupStep (writer) and
 * SettingsScreen (reader) via ProfilePrefs — see ProfilePrefs.kt. Defaults
 * match SettingsScreen's pre-existing hardcoded initial state exactly
 * ("" / "😎") so a user who hasn't completed onboarding sees identical
 * behavior to before this task.
 */
data class Profile(val displayName: String, val avatarEmoji: String) {
    companion object {
        fun default() = Profile(displayName = "", avatarEmoji = "😎")
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.onboarding.ProfileTest"`
Expected: PASS (2 tests, 0 failures)

- [ ] **Step 5: Create the SharedPreferences wrapper (not unit tested — see Global Constraints)**

Create `app/src/main/java/com/talhayun/adkan/onboarding/ProfilePrefs.kt`:

```kotlin
package com.talhayun.adkan.onboarding

import android.content.Context

/**
 * Local persistence for the onboarding profile — plain SharedPreferences,
 * matching com.talhayun.adkan.ui.blocking.BlockingPrefs exactly. Not synced
 * to Supabase from here — AuthService.updateProfile is the real server sync
 * path and is unaffected by this local cache.
 */
object ProfilePrefs {
    private const val PREFS_NAME = "com.talhayun.adkan.profile_prefs"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_AVATAR_EMOJI = "avatarEmoji"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): Profile {
        val p = prefs(context)
        val default = Profile.default()
        return Profile(
            displayName = p.getString(KEY_DISPLAY_NAME, default.displayName) ?: default.displayName,
            avatarEmoji = p.getString(KEY_AVATAR_EMOJI, default.avatarEmoji) ?: default.avatarEmoji,
        )
    }

    fun save(context: Context, profile: Profile) {
        prefs(context).edit()
            .putString(KEY_DISPLAY_NAME, profile.displayName)
            .putString(KEY_AVATAR_EMOJI, profile.avatarEmoji)
            .apply()
    }
}
```

- [ ] **Step 6: Save the profile at the end of onboarding**

In `app/src/main/java/com/talhayun/adkan/ui/onboarding/OnboardingFlow.kt`:

1. Add these imports near the top, alongside the existing `com.talhayun.adkan.*` imports:

```kotlin
import com.talhayun.adkan.onboarding.Profile
import com.talhayun.adkan.onboarding.ProfilePrefs
```

2. In `ProfileSetupStep`, add `val context = LocalContext.current` as the first line of the function body (before `var displayName by remember { ... }`) — `LocalContext` is already imported in this file for `PermissionStep`/`SignInStep`, so no new import is needed for this line.

3. Change the final `Button`'s `onClick` from:

```kotlin
        Button(
            onClick = onComplete,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "המשך")
        }
```

to:

```kotlin
        Button(
            onClick = {
                ProfilePrefs.save(context, Profile(displayName = displayName, avatarEmoji = selectedEmoji))
                onComplete()
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "המשך")
        }
```

- [ ] **Step 7: Load the profile in SettingsScreen.kt**

In `app/src/main/java/com/talhayun/adkan/ui/settings/SettingsScreen.kt`:

1. Add this import near the top, alongside the existing `com.talhayun.adkan.*` imports:

```kotlin
import com.talhayun.adkan.onboarding.ProfilePrefs
```

2. Change these two lines:

```kotlin
    var displayName by remember { mutableStateOf("") }
    var avatarEmoji by remember { mutableStateOf("😎") }
```

to:

```kotlin
    var displayName by remember { mutableStateOf(ProfilePrefs.load(context).displayName) }
    var avatarEmoji by remember { mutableStateOf(ProfilePrefs.load(context).avatarEmoji) }
```

(`context` is already defined two lines above as `val context = LocalContext.current` — confirm this before editing; if the local variable has a different name, use that name instead.)

- [ ] **Step 8: Self-review the whole diff (no compiler available)**

Read the full current `OnboardingFlow.kt` and `SettingsScreen.kt`. Confirm: `context` is in scope at both edited call sites, `Profile`/`ProfilePrefs` imports are present and not duplicated, and `ProfileSetupStep`'s `context` declaration doesn't collide with an existing variable of the same name in that function.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/onboarding/Profile.kt app/src/main/java/com/talhayun/adkan/onboarding/ProfilePrefs.kt app/src/main/java/com/talhayun/adkan/ui/onboarding/OnboardingFlow.kt app/src/main/java/com/talhayun/adkan/ui/settings/SettingsScreen.kt app/src/test/java/com/talhayun/adkan/onboarding/ProfileTest.kt
git commit -m "feat: persist onboarding profile so Settings reflects the real name/emoji"
```

---

### Task 4: Blocking decision engine (pure logic only — no enforcement)

**Context:** `app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingPrefs.kt` (already in the codebase) persists whether blocking is enabled, whether "always block" is on, and the set of selected app package names. `app/src/main/java/com/talhayun/adkan/screentime/ScreenTimeReader.kt` (already in the codebase) can report today's total foreground minutes. Neither is connected to any decision about whether a *specific app* should currently be blocked. This task adds that pure decision function — **it does not make blocking actually happen** (no Accessibility Service, no overlay — see Global Constraints). A future plan wires this function's output to a real enforcement mechanism.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/blocking/BlockingDecisionEngine.kt`
- Create: `app/src/test/java/com/talhayun/adkan/blocking/BlockingDecisionEngineTest.kt`

**Interfaces:**
- Produces: `com.talhayun.adkan.blocking.BlockingDecisionEngine.shouldBlock(packageName: String, selectedApps: Set<String>, blockingEnabled: Boolean, alwaysBlockEnabled: Boolean, todayForegroundMinutes: Int, thresholdMinutes: Int = 30): Boolean` — a pure function, no Android framework types in its signature, matching the existing UI copy's "ייחסמו לאחר חצי שעה של שימוש מצטבר" (blocked after half an hour of cumulative use), i.e. `thresholdMinutes` defaults to 30.
- Consumes: nothing from other tasks (this task is independent of Tasks 1-3 and can be done in any order relative to them).

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/talhayun/adkan/blocking/BlockingDecisionEngineTest.kt`:

```kotlin
package com.talhayun.adkan.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingDecisionEngineTest {

    private val instagram = "com.instagram.android"
    private val calculator = "com.android.calculator2"

    @Test
    fun `app not in the selected set is never blocked`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = calculator,
            selectedApps = setOf(instagram),
            blockingEnabled = true,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 999,
        )
        assertFalse(result)
    }

    @Test
    fun `blocking disabled and always-block off means never blocked, even over threshold`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = false,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 999,
        )
        assertFalse(result)
    }

    @Test
    fun `selected app under the threshold is not blocked`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = true,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 29,
            thresholdMinutes = 30,
        )
        assertFalse(result)
    }

    @Test
    fun `selected app at or over the threshold is blocked when blocking is enabled`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = true,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 30,
            thresholdMinutes = 30,
        )
        assertTrue(result)
    }

    @Test
    fun `always-block enabled blocks a selected app regardless of minutes`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = false,
            alwaysBlockEnabled = true,
            todayForegroundMinutes = 0,
        )
        assertTrue(result)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.blocking.BlockingDecisionEngineTest"`
Expected: FAIL — `com.talhayun.adkan.blocking.BlockingDecisionEngine` does not exist yet.

- [ ] **Step 3: Implement the decision engine**

Create `app/src/main/java/com/talhayun/adkan/blocking/BlockingDecisionEngine.kt`:

```kotlin
package com.talhayun.adkan.blocking

/**
 * Pure decision logic for whether a given app should currently be blocked.
 * Deliberately has NO Android framework dependency (no Context, no
 * UsageStatsManager call) so it's fully unit-testable — callers are
 * responsible for supplying today's foreground minutes (from
 * com.talhayun.adkan.screentime.ScreenTimeReader) and the persisted toggle
 * state (from com.talhayun.adkan.ui.blocking.BlockingPrefs).
 *
 * IMPORTANT: this function only decides — it does not enforce. No
 * Accessibility Service or overlay exists yet to act on its return value.
 * That is a separate, larger piece of work (see the Phase 2 plan's Global
 * Constraints).
 */
object BlockingDecisionEngine {
    fun shouldBlock(
        packageName: String,
        selectedApps: Set<String>,
        blockingEnabled: Boolean,
        alwaysBlockEnabled: Boolean,
        todayForegroundMinutes: Int,
        thresholdMinutes: Int = 30,
    ): Boolean {
        if (packageName !in selectedApps) return false
        if (alwaysBlockEnabled) return true
        if (!blockingEnabled) return false
        return todayForegroundMinutes >= thresholdMinutes
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.talhayun.adkan.blocking.BlockingDecisionEngineTest"`
Expected: PASS (5 tests, 0 failures)

- [ ] **Step 5: Self-review (no compiler available)**

Read the full `BlockingDecisionEngine.kt` and confirm the parameter order/names exactly match every call in the test file (Kotlin named arguments make order flexible, but confirm no name mismatch causes a resolution error).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/blocking/BlockingDecisionEngine.kt app/src/test/java/com/talhayun/adkan/blocking/BlockingDecisionEngineTest.kt
git commit -m "feat: add pure BlockingDecisionEngine (decision logic only, no enforcement)"
```

---

## Explicitly out of scope for this plan

- Real app-blocking enforcement (Accessibility Service or overlay) — `BlockingDecisionEngine` (Task 4) is the decision logic a future enforcement mechanism would call; nothing calls it yet.
- Wiring `BlockingDecisionEngine` into a live polling loop or service.
- Any change to Supabase-side profile sync (`AuthService.updateProfile`) — Task 3 is a local cache only, matching how `displayName` already worked in `SettingsScreen` before this plan (local state, not yet synced).
- Real percentile computation for the Home screen's `PercentileCard` — still sample data, untouched by this plan.

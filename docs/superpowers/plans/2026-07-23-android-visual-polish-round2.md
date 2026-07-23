# Android Visual Polish Round 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the remaining gaps from a rigorous visual-fidelity audit against the real iOS app — a real rounded Hebrew font, correct hero-number sizing, consistent list containers/dividers/title sizes across Groups/Friends/Settings, and two color-contrast fixes.

**Architecture:** Each task is a scoped, independently-verifiable Compose change. No new architecture — all changes are to existing composables' styling.

**Tech Stack:** Kotlin, Jetpack Compose (Material3). A font file (`app/src/main/res/font/varela_round.ttf`, Google's OFL-licensed Varela Round, confirmed to have dedicated Hebrew glyph coverage) has already been downloaded and is present in the repo, staged for Task 1.

## Global Constraints

- Package root: `com.talhayun.adkan`.
- All user-facing strings Hebrew, no English UI copy.
- Do not modify `BlockingDecisionEngine`, `BlockingPrefs`, `ForegroundBlockTracker`, `AppBlockAccessibilityService`, `ScreenTimeReader`, or anything under `blocking/` beyond `BlockingScreen.kt`'s gradient constant named in Task 5 — those are real, tested, reviewed enforcement code from a prior plan, completely unrelated to this visual pass.
- **Every task must end with a real `./gradlew :app:compileDebugKotlin` run showing actual output (task list + `BUILD SUCCESSFUL` line), not an assertion.** A prior task in a different plan was approved on a compile claim that turned out to be unevidenced; don't repeat that gap. Always run `./gradlew --stop` afterward to release the daemon.
- **Scope discipline:** each task implements only what its brief specifies. Do not proactively continue to further tasks, spawn additional agents, or make further edits after your task's steps are complete and committed — report your status and stop. A prior session had an agent exceed this exact boundary (dispatching its own parallel implementers after finishing an audit it was told was read-only) and had to be manually killed mid-work; do not repeat that failure mode.
- Verify referenced values in the *actual current* file before editing — several files in this codebase have been edited multiple times across prior passes and may not match older assumptions.

---

### Task 1: Wire the real Hebrew-rounded font (Varela Round) app-wide

**Context:** iOS uses SF Rounded (`design: .rounded`) everywhere (`App/DesignSystem/Theme.swift:52-56`). Android currently hardcodes `FontFamily.Default` (Roboto) in all four `Type.kt` styles and every `MaterialTheme.typography.*` reference falls back to Material3's default (also Roboto) because `AdKanTheme` never passes a custom `Typography` into `MaterialTheme`. This is one of the most pervasive "doesn't feel like the same app" gaps. A font file is already downloaded and present at `app/src/main/res/font/varela_round.ttf` (Google's OFL-licensed Varela Round — verified to include a dedicated Hebrew glyph subset). It's a single static Regular-weight file; Compose's default font synthesis (`FontSynthesis.All`) will render bold/semibold correctly from it without needing separate bold font files.

**Files:**
- Create: `app/src/main/java/com/talhayun/adkan/ui/theme/AppFont.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/theme/Theme.kt`

**Interfaces:**
- Produces: `com.talhayun.adkan.ui.theme.AppRoundedFont: FontFamily`.
- Consumes: nothing from other tasks.

- [ ] **Step 1: Confirm the font resource name**

Run: `ls app/src/main/res/font/`
Expected: `varela_round.ttf` present. Android font resource IDs are generated from the filename without extension, lowercase+underscore — this file already satisfies that (`varela_round`), so `R.font.varela_round` will resolve.

- [ ] **Step 2: Create the FontFamily definition**

Create `app/src/main/java/com/talhayun/adkan/ui/theme/AppFont.kt`:

```kotlin
package com.talhayun.adkan.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.talhayun.adkan.R

// [SKILL-DECL] Real rounded Hebrew-capable font closing the "iOS SF Rounded
// vs Android FontFamily.Default (Roboto)" gap flagged in the visual audit.
// Varela Round (OFL-licensed, bundled at res/font/varela_round.ttf) has a
// dedicated Hebrew glyph subset (verified against Google Fonts' METADATA.pb
// before downloading — subsets include "hebrew", not just "latin"). Only a
// Regular-weight file exists; bold/semibold text using this family relies on
// Compose's default FontSynthesis.All to synthesize weight, which is
// standard, well-supported behavior, not a workaround.
val AppRoundedFont = FontFamily(Font(R.font.varela_round))
```

- [ ] **Step 3: Apply it to the four Type.kt styles**

Read the current full content of `app/src/main/java/com/talhayun/adkan/ui/theme/Type.kt` first. Replace every `fontFamily = FontFamily.Default` with `fontFamily = AppRoundedFont`, and remove the now-stale comment about the gap (the file's top comment currently says "flagged as a visual gap, not silently faked... would need to be added later" — replace it with a short note that this is now resolved). Also remove the `import androidx.compose.ui.text.font.FontFamily` line if it's no longer referenced elsewhere in the file (check — `TextStyle`'s `fontFamily` parameter type doesn't require importing `FontFamily` itself if you're not referencing `FontFamily.Default` anymore, but `AppRoundedFont` is already typed `FontFamily` so Kotlin doesn't need the import for that usage — check the compiler on this, don't assume).

The resulting file's four `val` declarations should read:
```kotlin
val HeroNumber = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold, fontFamily = AppRoundedFont)
val HeroLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = AppRoundedFont)
val CardTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppRoundedFont)
val CardBody = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFamily = AppRoundedFont)
```
(Note: `HeroNumber`'s `72.sp` value will be changed to `36.sp` by Task 2 — if Task 2 hasn't landed yet when you do this task, leave the existing size as-is and only change `fontFamily`; do not also try to fix the size yourself, that's a different task's job.)

- [ ] **Step 4: Give MaterialTheme a real Typography using the new font**

In `app/src/main/java/com/talhayun/adkan/ui/theme/Theme.kt`, add these imports:
```kotlin
import androidx.compose.material3.Typography
```

Add this above the `AdKanTheme` composable function:
```kotlin
private val AppTypography = Typography().let { default ->
    Typography(
        displayLarge = default.displayLarge.copy(fontFamily = AppRoundedFont),
        displayMedium = default.displayMedium.copy(fontFamily = AppRoundedFont),
        displaySmall = default.displaySmall.copy(fontFamily = AppRoundedFont),
        headlineLarge = default.headlineLarge.copy(fontFamily = AppRoundedFont),
        headlineMedium = default.headlineMedium.copy(fontFamily = AppRoundedFont),
        headlineSmall = default.headlineSmall.copy(fontFamily = AppRoundedFont),
        titleLarge = default.titleLarge.copy(fontFamily = AppRoundedFont),
        titleMedium = default.titleMedium.copy(fontFamily = AppRoundedFont),
        titleSmall = default.titleSmall.copy(fontFamily = AppRoundedFont),
        bodyLarge = default.bodyLarge.copy(fontFamily = AppRoundedFont),
        bodyMedium = default.bodyMedium.copy(fontFamily = AppRoundedFont),
        bodySmall = default.bodySmall.copy(fontFamily = AppRoundedFont),
        labelLarge = default.labelLarge.copy(fontFamily = AppRoundedFont),
        labelMedium = default.labelMedium.copy(fontFamily = AppRoundedFont),
        labelSmall = default.labelSmall.copy(fontFamily = AppRoundedFont),
    )
}
```

Then change the `MaterialTheme(...)` call inside `AdKanTheme` from:
```kotlin
    MaterialTheme(colorScheme = colors, content = content)
```
to:
```kotlin
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
```

- [ ] **Step 5: Compile and verify**

Run: `./gradlew :app:compileDebugKotlin --console=plain` — paste the real output including the `BUILD SUCCESSFUL in Xs` line into your report. Then run `./gradlew --stop`.
Expected: `BUILD SUCCESSFUL`. If it fails on the font resource not resolving, run `ls app/src/main/res/font/` again and confirm the exact filename — do not guess a different name.

- [ ] **Step 6: Self-review**

Read the full `AppFont.kt`, `Type.kt`, and `Theme.kt`. Confirm no leftover `FontFamily.Default` reference anywhere in `Type.kt`, confirm `Typography()` (no-arg constructor) is a valid Material3 API call that returns the default type scale to copy from (it is — this is the standard way to get Material3's baseline `TextStyle`s to override just the `fontFamily` field on each, without hand-specifying every other property).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/ui/theme/AppFont.kt app/src/main/java/com/talhayun/adkan/ui/theme/Type.kt app/src/main/java/com/talhayun/adkan/ui/theme/Theme.kt
git commit -m "feat: apply real rounded Hebrew font (Varela Round) app-wide via MaterialTheme.typography"
```

---

### Task 2: Fix Home hero number sizing (shrink, don't just clip)

**Context:** `HeroNumber` is `72.sp` with no shrink; for any non-round duration (e.g. the sample `87` minutes → `"שעה ו-27 דקות"`, a multi-word phrase), this either wraps to 2-3 lines or — after a previous partial fix — gets silently clipped/cut off mid-word. iOS's actual equivalent is `36.sp` for Hebrew (`App/Visualization/TimeReclaimedView.swift:41`) with `.minimumScaleFactor(0.5)`.

**Files:**
- Modify: `app/src/main/java/com/talhayun/adkan/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `AppRoundedFont` (Task 1) if Task 1 has already landed — if not, this task should still change the font-size value on whatever `fontFamily` `HeroNumber` currently has; do not block on Task 1.

- [ ] **Step 1: Change HeroNumber's size**

Read the current `Type.kt`. Change `HeroNumber`'s `fontSize = 72.sp` to `fontSize = 36.sp`. Leave everything else on that line as-is (whatever `fontFamily` value is currently there, from Task 1 or still `FontFamily.Default`).

- [ ] **Step 2: Check HomeScreen.kt's current hero-number Text call**

Read the current full content of `app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt`, specifically the `UsageHeroCard` composable's `Text(text = formatMinutesHebrew(todayMinutes), style = HeroNumber, ...)` call. If it already has `maxLines = 1` and `overflow = TextOverflow.Clip` (from a prior partial fix), that's fine to keep as a safety net — but at `36.sp` instead of `72.sp`, the full Hebrew phrase should now fit on one line without needing to actually clip anything in practice, since the audit found iOS uses exactly `36.sp` for this same string shape.

If `overflow` is currently `TextOverflow.Clip`, consider changing it to `TextOverflow.Ellipsis` instead — `Clip` silently truncates mid-character with no visual indication text was cut, `Ellipsis` at least shows "…" so a genuinely too-long string (e.g. a future locale or edge-case duration) doesn't look like a rendering bug. This is a judgment call, not a strict requirement — either is acceptable, but note which one you chose and why in your report.

- [ ] **Step 3: Compile and verify**

Run: `./gradlew :app:compileDebugKotlin --console=plain` — paste real output. Then `./gradlew --stop`.

- [ ] **Step 4: Self-review**

Confirm `36.sp` (not `32`, not `40`) is the exact value now in `Type.kt`, matching the iOS citation.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/ui/theme/Type.kt app/src/main/java/com/talhayun/adkan/ui/home/HomeScreen.kt
git commit -m "fix: Home hero number sized to match iOS (36sp, not 72sp)"
```

---

### Task 3: Groups screen — flat list container + chevron rows (match Friends' new pattern)

**Context:** `FriendsScreen.kt` was already updated (in a prior, separately-reviewed pass) to wrap its `RankedMemberRow` list in one flat card with hairline dividers, using `RankedMemberRow`'s new `flat: Boolean = false` parameter. `GroupsScreen.kt` still uses the old per-row rounded-chip style for its member list, and its "כל הקבוצות" (other groups) section has no card/chevron/divider treatment at all. This task brings Groups to the same standard as Friends.

**Files:**
- Modify: `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt`

**Interfaces:**
- Consumes: `RankedMemberRow`'s existing `flat: Boolean = false` parameter (already added, do not modify `RankedMemberRow.kt` itself in this task).

- [ ] **Step 1: Read the current file**

Read the full current content of `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt` — do not assume it matches any earlier version verbatim.

- [ ] **Step 2: Wrap the ranked-member list in a flat card with dividers**

Find the block that looks like:
```kotlin
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
```
Replace it with:
```kotlin
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                sampleGroupMembers.forEachIndexed { index, member ->
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
                        flat = true,
                    )
                    if (index < sampleGroupMembers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp, end = 12.dp),
                            thickness = Dp.Hairline,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }
```

- [ ] **Step 3: Add chevron + card treatment to the "כל הקבוצות" list**

Find the block iterating `sampleOtherGroups` (a `Row` per group with emoji/name and member count, no card/chevron). Wrap it the same way, adding a leading (in RTL, visually-left) chevron icon and dividers between rows:
```kotlin
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                sampleOtherGroups.forEachIndexed { index, group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${group.emoji} ", fontSize = 18.sp)
                            Text(text = group.name, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${group.memberCount} חברים",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < sampleOtherGroups.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = Dp.Hairline,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }
```
Note: `Icons.AutoMirrored.Filled.KeyboardArrowLeft` (not the non-`AutoMirrored` `Icons.Filled.KeyboardArrowLeft`) automatically flips direction under RTL layout, which this whole app forces app-wide — using the non-mirrored version would point the wrong way in the app's actual Hebrew/RTL context. This requires the `material-icons-extended` dependency, which is already present in `app/build.gradle.kts` (added in an earlier, unrelated pass for the tab bar icons) — confirm this before assuming, but do not add a new dependency if it's already there.

- [ ] **Step 4: Add missing imports**

Add, if not already present: `androidx.compose.material3.HorizontalDivider`, `androidx.compose.material3.Icon`, `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft`, `androidx.compose.ui.unit.Dp`, `androidx.compose.foundation.layout.Spacer` (check each against the file's current imports first — several may already be present from other uses in this file).

- [ ] **Step 5: Compile and verify**

Run: `./gradlew :app:compileDebugKotlin --console=plain` — paste real output. Then `./gradlew --stop`.

- [ ] **Step 6: Self-review**

Read the full edited file. Confirm brace/paren balance, confirm `flat = true` is passed at the ranked-member-row call site, confirm the two `forEachIndexed` divider conditions correctly skip the divider after the last item in each list (off-by-one is an easy mistake here — trace it for a 2-element and a 5-element list by hand).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt
git commit -m "feat: Groups screen flat-list containers + chevron rows, matching Friends screen"
```

---

### Task 4: Consistent large titles + real Settings dividers

**Context:** iOS uses large navigation titles (~32-34pt bold) on Groups/Blocking/Settings; Android currently uses `headlineSmall` (24sp) on Groups/Blocking and a hardcoded `28.sp` on Settings — both smaller and inconsistent with each other (Friends was already fixed to `headlineLarge` in a prior pass). Separately, Settings' section dividers (`SettingsDivider()`) are literally just a `Spacer` — no visible line — while iOS shows real hairline dividers between notification rows.

**Files:**
- Modify: `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Groups title**

In `GroupsScreen.kt`, find `Text(text = "קבוצות", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)` and change `headlineSmall` to `headlineLarge`.

- [ ] **Step 2: Blocking title**

In `BlockingScreen.kt`, find `Text(text = "חסימת אפליקציות", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)` and change `headlineSmall` to `headlineLarge`.

- [ ] **Step 3: Settings title**

In `SettingsScreen.kt`, find `Text(text = "הגדרות", style = CardTitle, fontSize = 28.sp, fontWeight = FontWeight.Bold)`. Change to use the same scale as the other three screens: `Text(text = "הגדרות", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)` (dropping the `CardTitle` style and hardcoded `28.sp` in favor of the shared `headlineLarge` token, for consistency across all four screens). Confirm `CardTitle` is not now an unused import in this file — check its other usages first (grep the file) before removing the import.

- [ ] **Step 4: Real Settings dividers**

In `SettingsScreen.kt`, find:
```kotlin
@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(12.dp))
}
```
Replace with:
```kotlin
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        thickness = Dp.Hairline,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}
```
Add imports if not already present: `androidx.compose.material3.HorizontalDivider`, `androidx.compose.ui.unit.Dp`.

- [ ] **Step 5: Compile and verify**

Run: `./gradlew :app:compileDebugKotlin --console=plain` — paste real output. Then `./gradlew --stop`.

- [ ] **Step 6: Self-review**

Read all three edited files in full. Confirm `CardTitle` import in `SettingsScreen.kt` is removed only if genuinely unused elsewhere (grep for `CardTitle` in the file — if any other line still references it, keep the import).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt app/src/main/java/com/talhayun/adkan/ui/settings/SettingsScreen.kt
git commit -m "fix: consistent large titles across all 4 screens + real Settings dividers"
```

---

### Task 5: Groups toggle contrast + Blocking hero gradient

**Context:** Two small, independent color-contrast fixes. (1) The Groups today/week segmented toggle currently has its *selected* segment (`colorScheme.surface`, `#1C1C1E`) darker than the *unselected* track (`colorScheme.surfaceVariant`, `#2C2C2E`) — inverted from iOS, where the selected segment is visibly lighter and pops forward. (2) The Blocking screen's green hero gradient fades from `BrandGreen` (`#78C96F`) down to `BrandGreenLight` (`#C5EDBA`, a pale mint) — washing out toward near-white at the bottom, unlike the more consistently saturated green in the iOS screenshot.

**Files:**
- Modify: `app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt`
- Modify: `app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt`

- [ ] **Step 1: Fix the Groups toggle contrast**

In `GroupsScreen.kt`'s `RangeToggle` composable, find:
```kotlin
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
```
Change to:
```kotlin
                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
```
This makes the selected pill a light-gray fill that visibly pops against the darker `surfaceVariant` track, rather than a same-or-darker fill that recedes.

- [ ] **Step 2: Fix the Blocking hero gradient**

In `BlockingScreen.kt`, find:
```kotlin
private val greenHeroGradient = Brush.verticalGradient(listOf(BrandGreen, BrandGreenLight))
```
Change to:
```kotlin
private val greenHeroGradient = Brush.verticalGradient(listOf(Color(0xFF5FB85A), BrandGreen))
```
This keeps the gradient in a consistently saturated green range (matching iOS's `primaryGradient` stops of roughly `#478E42 → #7AC76B`, adapted slightly lighter to read well against this screen's dark card background) instead of fading to pale mint. Add the import `androidx.compose.ui.graphics.Color` if not already present (check first — this file may already import it for other uses).

- [ ] **Step 3: Compile and verify**

Run: `./gradlew :app:compileDebugKotlin --console=plain` — paste real output. Then `./gradlew --stop`.

- [ ] **Step 4: Self-review**

Read both edited files. Confirm `BrandGreenLight` import in `BlockingScreen.kt` is removed only if truly unused elsewhere in the file (grep first).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/talhayun/adkan/ui/groups/GroupsScreen.kt app/src/main/java/com/talhayun/adkan/ui/blocking/BlockingScreen.kt
git commit -m "fix: Groups toggle selected-state contrast + Blocking hero gradient saturation"
```

---

## Explicitly out of scope for this plan

- Any change to `blocking/` files beyond the one gradient constant in Task 5 — the enforcement logic itself is untouched.
- Full color-palette audit beyond the two specific fixes in Task 5 — the audit's "verified correct, do not change" section (palette hexes, card corner radius, podium, RankedMemberRow internals, mascot state machine) stays as-is.
- Any further agent self-continuation beyond each task's own scope — see Global Constraints.

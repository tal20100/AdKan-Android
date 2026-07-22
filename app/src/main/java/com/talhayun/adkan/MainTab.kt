package com.talhayun.adkan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

// Extracted from MainActivity.kt so it's unit-testable on its own (see
// MainTabTest) and importable from ui/home/HomeScreen.kt for the Focus-button
// navigation wiring (Task 2 of the Phase 2 plan).
//
// [SKILL-DECL] ui-ux-pro-max audit: emoji-as-structural-navigation-icon is a
// real anti-pattern (font-dependent rendering, not tintable to match
// selected/unselected nav-bar states, doesn't scale like a vector asset) —
// switched from emoji to real Material vector icons, matching iOS's tab bar
// which uses actual SF Symbols (house.fill, person.2.fill, trophy.fill,
// shield.checkered, gearshape.fill), not emoji either. ImageVector is a plain
// data-holder type with no Android-runtime dependency, so MainTabTest can
// still reference it in a pure JUnit4 test without Robolectric.
enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("בית", Icons.Filled.Home),
    FRIENDS("חברים", Icons.Filled.Group),
    GROUPS("קבוצות", Icons.Filled.EmojiEvents),
    BLOCKING("פוקוס", Icons.Filled.Shield),
    SETTINGS("הגדרות", Icons.Filled.Settings),
}

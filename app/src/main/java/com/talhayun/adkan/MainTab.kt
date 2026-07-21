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

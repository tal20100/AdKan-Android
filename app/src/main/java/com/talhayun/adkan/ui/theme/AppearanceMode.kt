package com.talhayun.adkan.ui.theme

// Extracted from ui/settings/SettingsScreen.kt (previously private, local-only
// state with no effect on the actual theme) so MainActivity can read/persist
// it and pass a real dark/light decision into AdKanTheme.
enum class AppearanceMode(val label: String) {
    LIGHT("בהיר"),
    DARK("כהה"),
    SYSTEM("מערכת"),
}

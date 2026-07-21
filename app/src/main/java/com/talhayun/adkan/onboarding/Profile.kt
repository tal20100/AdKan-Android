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

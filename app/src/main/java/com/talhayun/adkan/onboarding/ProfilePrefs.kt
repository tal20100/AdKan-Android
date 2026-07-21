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

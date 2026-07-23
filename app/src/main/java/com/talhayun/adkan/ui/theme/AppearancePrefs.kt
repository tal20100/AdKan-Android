package com.talhayun.adkan.ui.theme

import android.content.Context

/**
 * Local persistence for the appearance (dark/light/system) setting — plain
 * SharedPreferences, mirrors com.talhayun.adkan.ui.blocking.BlockingPrefs
 * exactly. Previously this setting was local-only Compose state in
 * SettingsScreen with no effect on the actual theme; this makes it real.
 */
object AppearancePrefs {
    private const val PREFS_NAME = "com.talhayun.adkan.appearance_prefs"
    private const val KEY_MODE = "appearanceMode"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): AppearanceMode {
        val name = prefs(context).getString(KEY_MODE, null) ?: return AppearanceMode.SYSTEM
        return try {
            AppearanceMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            AppearanceMode.SYSTEM
        }
    }

    fun save(context: Context, mode: AppearanceMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }
}

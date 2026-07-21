package com.talhayun.adkan.ui.blocking

import android.content.Context

/**
 * Local persistence for the Blocking screen's toggle state — plain
 * SharedPreferences, matching this codebase's existing pattern (see
 * AuthService's plainPrefs / SecureTokenStore) rather than introducing
 * DataStore for two booleans. Not synced to Supabase — mirrors iOS's
 * @AppStorage-backed local toggles, which also aren't server-synced.
 */
object BlockingPrefs {
    private const val PREFS_NAME = "com.talhayun.adkan.blocking_prefs"
    private const val KEY_BLOCKING_ENABLED = "blockingEnabled"
    private const val KEY_ALWAYS_BLOCK_ENABLED = "alwaysBlockEnabled"
    private const val KEY_SELECTED_APPS = "selectedApps"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isBlockingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCKING_ENABLED, true)

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCKING_ENABLED, enabled).apply()
    }

    fun isAlwaysBlockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALWAYS_BLOCK_ENABLED, false)

    fun setAlwaysBlockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALWAYS_BLOCK_ENABLED, enabled).apply()
    }

    fun selectedApps(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()

    fun setSelectedApps(context: Context, packageNames: Set<String>) {
        prefs(context).edit().putStringSet(KEY_SELECTED_APPS, packageNames).apply()
    }
}

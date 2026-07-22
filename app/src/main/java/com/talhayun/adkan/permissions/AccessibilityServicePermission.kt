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

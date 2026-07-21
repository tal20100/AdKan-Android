package com.talhayun.adkan.ui.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class AppInfo(val packageName: String, val label: String)

/**
 * Real installed-app list for the block-selection picker, via the standard
 * "what shows up on the home screen" query (ACTION_MAIN + CATEGORY_LAUNCHER)
 * — the same approach system launchers and other usage-tracking apps use.
 * Excludes this app itself (blocking yourself out of AdKan makes no sense).
 */
fun installedLaunchableApps(context: Context): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val packageManager = context.packageManager
    val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

    return resolveInfos
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
            if (packageName == context.packageName) return@mapNotNull null
            val label = resolveInfo.loadLabel(packageManager).toString()
            AppInfo(packageName = packageName, label = label)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

package com.talhayun.adkan.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

// [SKILL-DECL] Confirmed via developer.android.com UsageStatsManager /
// AppOpsManager docs (per
// plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md): PACKAGE_USAGE_STATS
// is a protected permission with no runtime request dialog — the only path is
// AppOpsManager.checkOpNoThrow to read current state, and sending the user to
// Settings.ACTION_USAGE_ACCESS_SETTINGS to flip it manually. This is the
// Android analog of iOS FamilyControls, but unlike FamilyControls there is no
// system permission prompt Android can show on our behalf, so the UI must
// guide the user to Settings and re-check on return-to-app.
object UsageAccessPermission {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent {
        // Per the verified pattern: ACTION_USAGE_ACCESS_SETTINGS does not
        // reliably support a package-scoped data URI across OEMs, so this
        // opens the general Usage Access list rather than risking an
        // ActivityNotFoundException on stricter ROMs.
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }
}

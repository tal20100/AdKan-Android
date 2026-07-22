package com.talhayun.adkan.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.talhayun.adkan.screentime.ScreenTimeReader
import com.talhayun.adkan.ui.blocking.BlockingPrefs

/**
 * Detects foreground-app changes (TYPE_WINDOW_STATE_CHANGED) and launches
 * BlockedActivity over any app the founder selected for blocking, once its
 * cumulative usage today crosses the threshold (or immediately, if
 * "always block" is on) — see BlockingDecisionEngine for the exact rule.
 *
 * IMPORTANT — cannot be verified from this environment (no device/emulator
 * available here): whether Android reliably delivers these events in the
 * background, whether OEM battery optimization suspends this service, and
 * whether the launched Activity reliably draws on top of the blocked app
 * are all real-device concerns this implementation cannot confirm. See the
 * plan's final founder-testing task.
 */
class AppBlockAccessibilityService : AccessibilityService() {

    private val tracker = ForegroundBlockTracker()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberately `event == null ||` rather than `event?.eventType != ...` —
        // Kotlin does not smart-cast `event` to non-null through a safe-call
        // comparison like `event?.x != y`, only through a direct null check.
        // Without this, `event.packageName` below would not compile.
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        val selectedApps = BlockingPrefs.selectedApps(this)
        if (packageName !in selectedApps) {
            tracker.reset()
            return
        }

        val shouldBlock = BlockingDecisionEngine.shouldBlock(
            packageName = packageName,
            selectedApps = selectedApps,
            blockingEnabled = BlockingPrefs.isBlockingEnabled(this),
            alwaysBlockEnabled = BlockingPrefs.isAlwaysBlockEnabled(this),
            todayForegroundMinutes = ScreenTimeReader.todayMinutesForPackage(this, packageName),
        )

        if (shouldBlock && tracker.shouldLaunchBlockScreen(packageName)) {
            launchBlockedActivity(packageName)
        }
    }

    private fun launchBlockedActivity(packageName: String) {
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }

        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(BlockedActivity.EXTRA_BLOCKED_APP_LABEL, label)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override, no cleanup needed — this service holds no
        // resources beyond the in-memory ForegroundBlockTracker.
    }
}

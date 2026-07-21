package com.talhayun.adkan.blocking

/**
 * Pure decision logic for whether a given app should currently be blocked.
 * Deliberately has NO Android framework dependency (no Context, no
 * UsageStatsManager call) so it's fully unit-testable — callers are
 * responsible for supplying today's foreground minutes (from
 * com.talhayun.adkan.screentime.ScreenTimeReader) and the persisted toggle
 * state (from com.talhayun.adkan.ui.blocking.BlockingPrefs).
 *
 * IMPORTANT: this function only decides — it does not enforce. No
 * Accessibility Service or overlay exists yet to act on its return value.
 * That is a separate, larger piece of work (see the Phase 2 plan's Global
 * Constraints).
 */
object BlockingDecisionEngine {
    fun shouldBlock(
        packageName: String,
        selectedApps: Set<String>,
        blockingEnabled: Boolean,
        alwaysBlockEnabled: Boolean,
        todayForegroundMinutes: Int,
        thresholdMinutes: Int = 30,
    ): Boolean {
        if (packageName !in selectedApps) return false
        if (alwaysBlockEnabled) return true
        if (!blockingEnabled) return false
        return todayForegroundMinutes >= thresholdMinutes
    }
}

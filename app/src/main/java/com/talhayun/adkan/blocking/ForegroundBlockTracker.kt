package com.talhayun.adkan.blocking

/**
 * Pure dedup logic for the blocking Accessibility Service: prevents
 * re-launching BlockedActivity repeatedly while the same blocked app stays
 * in the foreground (onAccessibilityEvent can fire many times per second
 * for unrelated window-state changes within one app). One instance lives
 * for the lifetime of the AccessibilityService (see AppBlockAccessibilityService).
 * No Android framework dependency — instantiable and testable on a plain JVM.
 */
class ForegroundBlockTracker {
    private var lastBlockedPackage: String? = null

    fun shouldLaunchBlockScreen(currentForegroundPackage: String): Boolean {
        if (currentForegroundPackage == lastBlockedPackage) return false
        lastBlockedPackage = currentForegroundPackage
        return true
    }

    fun reset() {
        lastBlockedPackage = null
    }
}

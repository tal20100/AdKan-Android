package com.talhayun.adkan.ui.groups

/**
 * Extracted from GroupsScreen.kt so it's unit-testable on its own (see
 * GroupsRangeTest). WEEK is premium-gated per the real screenshots (shown
 * behind a lock icon); upsellMessage() is what the UI shows when a user taps
 * a locked range instead of silently doing nothing.
 */
enum class GroupsRange(val label: String, val locked: Boolean) {
    WEEK("השבוע", locked = true),
    TODAY("היום", locked = false);

    fun upsellMessage(): String? = if (locked) "צפייה בנתוני השבוע דורשת מנוי פרימיום" else null
}

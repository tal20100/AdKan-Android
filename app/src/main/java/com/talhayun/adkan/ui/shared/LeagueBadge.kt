package com.talhayun.adkan.ui.shared

// [SKILL-DECL] Ported from iOS App/Models/LeagueBadge.swift — same emoji values,
// same displayable rule (only NONE hides). The two `from(...)` classifier
// functions aren't ported here since nothing in this Android pass computes
// real streak/perfect-week data yet (sample data only) — just the enum shape
// needed by RankedMemberRow/PodiumView to render a badge.
enum class LeagueBadge(val emoji: String) {
    NONE(""),
    BRONZE("🥉"),
    SILVER("🥈"),
    GOLD("🥇"),
    DIAMOND("💎"),
    CROWN("👑");

    val displayable: Boolean get() = this != NONE
}

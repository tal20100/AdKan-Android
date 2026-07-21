package com.talhayun.adkan.ui.home

// [SKILL-DECL] Ported from iOS App/Localization/TimeFormatter.swift line-by-line
// (both the English branch used elsewhere and now the Hebrew branch, including
// the singular "שעה"/"שעתיים 2h"/"דקה" 1m special cases and the RLM (U+200F)
// + non-breaking hyphen (U+2011) wrapping so "X שעות ו-Y דקות" doesn't get
// visually reordered by the Compose/ICU bidi algorithm the way a plain ASCII
// "-" would).
fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun hebrewHours(h: Int): String = when (h) {
    1 -> "שעה"
    2 -> "שעתיים"
    else -> "$h שעות"
}

private fun hebrewMinutes(m: Int): String = when (m) {
    1 -> "דקה"
    else -> "$m דקות"
}

/** Mirrors TimeFormatter.format(minutes:locale:)'s Hebrew branch exactly, RLM marks and all. */
fun formatMinutesHebrew(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val rlm = "‏"
    val nbHyphen = "‑"
    return when {
        h > 0 && m > 0 -> "$rlm${hebrewHours(h)} ו$nbHyphen${hebrewMinutes(m)}$rlm"
        h > 0 -> "$rlm${hebrewHours(h)}$rlm"
        else -> "$rlm${hebrewMinutes(m)}$rlm"
    }
}

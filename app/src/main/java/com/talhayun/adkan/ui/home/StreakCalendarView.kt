package com.talhayun.adkan.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.CardBody
import com.talhayun.adkan.ui.theme.CardTitle

// [SKILL-DECL] Ported from iOS App/Home/StreakCalendarView.swift + StreakDotGridView.swift
// (5x7 dot grid, oldest day top-left, today bottom-right, green dot = daily goal
// met that day) and App/Localizable.xcstrings for the exact Hebrew copy
// (home.streakCalendar.title = "רצף הריכוז", home.streakCalendar.heroSubtitle =
// "ימי ריכוז", home.streakBest %lld = "שיא: %lld ימים") — switched from this
// file's original English placeholder copy to Hebrew to match the app's
// Hebrew-first identity seen in every real screenshot. Sample/fake data
// only in this pass — no ServiceContainer / server score sync wired up yet, and
// the goal-met-per-day logic (DayStatusProvider on iOS) is not ported, just the
// visual grid shape.

/** Number of columns (days per week row) in the streak dot grid — mirrors iOS. */
const val StreakCalendarColumns = 7

/** Number of rows (weeks) in the streak dot grid — mirrors iOS (5 weeks = 35 days). */
const val StreakCalendarRows = 5

/**
 * 5x7 dot-grid streak calendar, mirroring StreakCalendarView.swift's compact card.
 *
 * @param metGoalDays exactly [StreakCalendarColumns] * [StreakCalendarRows] entries,
 *   oldest day first, today last — true where the daily goal was met that day.
 */
@Composable
fun StreakCalendarView(
    currentStreak: Int,
    longestStreak: Int,
    metGoalDays: List<Boolean>,
    modifier: Modifier = Modifier,
) {
    require(metGoalDays.size == StreakCalendarColumns * StreakCalendarRows) {
        "metGoalDays must have exactly ${StreakCalendarColumns * StreakCalendarRows} entries (5 weeks x 7 days), got ${metGoalDays.size}"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = "רצף הריכוז", style = CardTitle)

        Spacer(Modifier.height(12.dp))

        for (row in 0 until StreakCalendarRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (col in 0 until StreakCalendarColumns) {
                    val index = row * StreakCalendarColumns + col
                    val metGoal = metGoalDays[index]
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (metGoal) BrandGreen else BrandGreen.copy(alpha = 0.10f)),
                    )
                }
            }
            if (row != StreakCalendarRows - 1) Spacer(Modifier.height(8.dp))
        }

        if (longestStreak > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "שיא: $longestStreak ימים",
                style = CardBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Sample data for HomeScreen preview/integration only — not real usage history.
 * Last 6 entries true to line up with HomeScreen.kt's sampleStreak = 6, with a
 * few scattered earlier hits so the grid doesn't look suspiciously empty.
 */
val sampleMetGoalDays: List<Boolean> = List(StreakCalendarColumns * StreakCalendarRows) { index ->
    when {
        index >= (StreakCalendarColumns * StreakCalendarRows - 6) -> true
        index in setOf(5, 12, 18, 25) -> true
        else -> false
    }
}

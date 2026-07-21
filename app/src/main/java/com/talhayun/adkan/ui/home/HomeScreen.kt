package com.talhayun.adkan.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.permissions.UsageAccessPermission
import com.talhayun.adkan.screentime.ScreenTimeReader
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.CardTitle
import com.talhayun.adkan.ui.theme.DangerRed
import com.talhayun.adkan.ui.theme.HeroGradientDark
import com.talhayun.adkan.ui.theme.HeroGradientLight
import com.talhayun.adkan.ui.theme.HeroNumber
import com.talhayun.adkan.ui.theme.SuccessGreen
import com.talhayun.adkan.ui.theme.minutesColor

// [SKILL-DECL] Ported from iOS App/Home/HomeView.swift's card order (Mascot ->
// usage hero w/ yesterday row -> percentile card -> focus button -> streak ->
// daily-goal bar) per plan/serialized-tinkering-pony-agent-a605d627b562c6fac.md
// section 8. Now wired to real data: when PACKAGE_USAGE_STATS is granted
// (UsageAccessPermission.isGranted), today/yesterday minutes come from
// ScreenTimeReader (real UsageStatsManager query); otherwise falls back to
// sample numbers so the screen still demos sensibly pre-permission. Daily
// goal is still a hardcoded default (120 min) — no user-configurable goal
// setting exists yet, that's a separate piece of work. The old
// GroupLeaderboardCard block is removed — screenshots show no leaderboard
// preview on the Home tab; that content now lives in ui/groups/GroupsScreen.kt.

private const val sampleGoalMinutes = 120
private const val sampleTodayMinutes = 87
private const val sampleYesterdayMinutes = 104
private const val sampleStreak = 6
private const val samplePercentile = 41

@Composable
fun HomeScreen(onFocusClick: () -> Unit) {
    val context = LocalContext.current
    val hasRealData = remember { UsageAccessPermission.isGranted(context) }
    val todayMinutes = remember(hasRealData) {
        if (hasRealData) ScreenTimeReader.todayMinutes(context) else sampleTodayMinutes
    }
    val yesterdayMinutes = remember(hasRealData) {
        if (hasRealData) ScreenTimeReader.yesterdayMinutes(context) else sampleYesterdayMinutes
    }
    val goalMinutes = sampleGoalMinutes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AdKanSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AdKanSpacing.cardSpacing),
    ) {
        MascotView(
            todayMinutes = todayMinutes,
            goalMinutes = goalMinutes,
            modifier = Modifier.fillMaxWidth(),
        )
        UsageHeroCard(todayMinutes = todayMinutes, yesterdayMinutes = yesterdayMinutes, goalMinutes = goalMinutes)
        PercentileCard(todayMinutes = todayMinutes)
        FocusButton(onClick = onFocusClick)
        StreakCard()
        DailyGoalCard(todayMinutes = todayMinutes, goalMinutes = goalMinutes)
    }
}

/**
 * Dark navy-gradient hero card mirroring screenshot
 * "WhatsApp Image 2026-07-11 at 20.14.00.jpeg": big Hebrew minutes number,
 * "זמן מסך היום" subtitle, progress bar, warning row once over goal, a
 * divider, then a goal/yesterday summary row — merged into one card per the
 * screenshot (previously split across two separate cards).
 */
@Composable
private fun UsageHeroCard(todayMinutes: Int, yesterdayMinutes: Int, goalMinutes: Int) {
    val isOverGoal = todayMinutes > goalMinutes
    val progress = (todayMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1.5f)
    val barColor = minutesColor(todayMinutes, goalMinutes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AdKanSpacing.cardCornerRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSystemInDarkTheme()) HeroGradientDark else HeroGradientLight)
                .padding(AdKanSpacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatMinutesHebrew(todayMinutes),
                style = HeroNumber,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "זמן מסך היום",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // Progress track: full-width background, filled portion clamped
            // at 100% width, colored to match the state (green/orange/red).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor),
                )
            }

            if (isOverGoal) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚠️ ", color = DangerRed)
                    Text(
                        text = "${formatMinutesHebrew(todayMinutes - goalMinutes)} מעל היעד",
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row {
                    Text(text = "אתמול: ", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatMinutesHebrew(yesterdayMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row {
                    Text(text = "🎯 ", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = formatMinutesHebrew(goalMinutes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * "המיקום שלך היום" percentile card — mirrors the screenshot's share-up icon
 * + dart emoji + body text comparing today's usage against other users.
 * Percentile itself is still a sample number — no server-side percentile
 * computation exists on the Android side yet, only the display uses real
 * today-minutes when available.
 */
@Composable
private fun PercentileCard(todayMinutes: Int) {
    PlainCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📤 ", fontSize = 16.sp)
            Text(text = "המיקום שלך היום", style = CardTitle)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(text = "🎯 ", fontSize = 15.sp)
            Text(
                text = "יותר זמן מסך היום מ-$samplePercentile% מהמשתמשים, עם ${formatMinutesHebrew(todayMinutes)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Full-width green CTA mirroring the screenshot's "הפעל פוקוס" button — no-op, sample UI only. */
@Composable
private fun FocusButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
        shape = RoundedCornerShape(50),
    ) {
        Text(text = "הפעל פוקוס", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StreakCard() {
    PlainCard {
        StreakCalendarView(
            currentStreak = sampleStreak,
            longestStreak = 9,
            metGoalDays = sampleMetGoalDays,
        )
    }
}

/**
 * "יעד יומי" daily-goal card: flame icon + title, used/remaining row, and a
 * two-tone bar (red = used, green = remaining) split at the goal point —
 * mirrors the screenshot's red-mostly bar with a thin marker near the end.
 */
@Composable
private fun DailyGoalCard(todayMinutes: Int, goalMinutes: Int) {
    val usedFraction = (todayMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
    val remainingMinutes = (goalMinutes - todayMinutes).coerceAtLeast(0)

    PlainCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥 ", fontSize = 16.sp)
            Text(text = "יעד יומי", style = CardTitle)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMinutesHebrew(todayMinutes),
                fontWeight = FontWeight.SemiBold,
                color = DangerRed,
            )
            Text(
                text = formatMinutesHebrew(remainingMinutes),
                fontWeight = FontWeight.SemiBold,
                color = SuccessGreen,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
        ) {
            when {
                usedFraction <= 0f -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(SuccessGreen),
                )
                usedFraction >= 1f -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DangerRed),
                )
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(usedFraction)
                            .fillMaxHeight()
                            .background(DangerRed),
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - usedFraction)
                            .fillMaxHeight()
                            .background(SuccessGreen),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AdKanSpacing.cardCornerRadius),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(AdKanSpacing.cardPadding),
            content = content,
        )
    }
}

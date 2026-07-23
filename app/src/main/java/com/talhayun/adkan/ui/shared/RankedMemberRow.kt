package com.talhayun.adkan.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.WarningOrange

// [SKILL-DECL] Ported from iOS App/DesignSystem/RankedMemberRow.swift line-by-line:
// medal emoji for ranks 1-3 else "#N", 42dp avatar circle (green tint+border if
// current user), name+badge row, streak/wins pill row (only shown when either >
// 0), optional staleLabel line, trailing minutes value or "---" placeholder,
// optional trailing composable slot (rank-change arrow / share icon on iOS —
// Kotlin doesn't have Swift's generic-View-with-EmptyView-extension pattern, so
// a default empty lambda serves the same purpose). Row background: brandGreen
// tint if current user, else a muted surface tint — mirrors
// Color(.secondarySystemBackground).opacity(0.6) via surfaceVariant.

private val rankedMemberRowMedals = listOf("🥇", "🥈", "🥉")

@Composable
fun RankedMemberRow(
    rank: Int?,
    avatarEmoji: String,
    displayName: String,
    formattedMinutes: String,
    hasMinutesData: Boolean,
    minutesColor: Color,
    streak: Int,
    badge: LeagueBadge,
    isCurrentUser: Boolean,
    flat: Boolean = false,
    modifier: Modifier = Modifier,
    wins: Int = 0,
    staleLabel: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (flat) it else it.clip(RoundedCornerShape(14.dp)) }
            .background(
                when {
                    isCurrentUser -> BrandGreen.copy(alpha = 0.08f)
                    flat -> Color.Transparent
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankView(rank = rank, isCurrentUser = isCurrentUser)

        Spacer(Modifier.width(10.dp))

        AvatarView(avatarEmoji = avatarEmoji, isCurrentUser = isCurrentUser)

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentUser) BrandGreen else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (badge.displayable) {
                    Spacer(Modifier.width(4.dp))
                    Text(text = badge.emoji, fontSize = 14.sp)
                }
            }

            if (streak > 0 || wins > 0) {
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (streak > 0) StreakPill(streak = streak)
                    if (wins > 0) WinsPill(wins = wins)
                }
            }

            if (staleLabel != null) {
                Text(
                    text = staleLabel,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        MinutesView(formattedMinutes = formattedMinutes, hasMinutesData = hasMinutesData, minutesColor = minutesColor)

        trailing()
    }
}

@Composable
private fun RankView(rank: Int?, isCurrentUser: Boolean) {
    Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
        when {
            rank != null && rank in 1..3 -> Text(text = rankedMemberRowMedals[rank - 1], fontSize = 22.sp)
            rank != null -> Text(
                text = "#$rank",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) BrandGreen.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {}
        }
    }
}

@Composable
private fun AvatarView(avatarEmoji: String, isCurrentUser: Boolean) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isCurrentUser) BrandGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = avatarEmoji, fontSize = 22.sp)
    }
}

@Composable
private fun StreakPill(streak: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(WarningOrange.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(text = "🔥", fontSize = 11.sp)
        Text(text = "$streak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
    }
}

@Composable
private fun WinsPill(wins: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandGreen.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(text = "🏆", fontSize = 11.sp)
        Text(text = "$wins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
    }
}

@Composable
private fun MinutesView(formattedMinutes: String, hasMinutesData: Boolean, minutesColor: Color) {
    if (hasMinutesData) {
        Text(text = formattedMinutes, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = minutesColor, maxLines = 1)
    } else {
        Text(
            text = "---",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
        )
    }
}

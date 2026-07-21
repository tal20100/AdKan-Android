package com.talhayun.adkan.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.ui.home.formatMinutesHebrew
import com.talhayun.adkan.ui.shared.LeagueBadge
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.HeroGradientDark
import com.talhayun.adkan.ui.theme.podiumBarColor
import com.talhayun.adkan.ui.theme.podiumBorderColor

// [SKILL-DECL] Ported from iOS App/Visualization/PodiumView.swift: 3-column
// layout (2nd, 1st, 3rd order, bottom-aligned), crown above #1 only, avatar
// sized 68dp for #1 / 52dp for #2-3 (green ring+fill if current user), colored
// bar (gold/silver/bronze via podiumBarColor/podiumBorderColor) sized
// 110/80/60dp with "#N" overlay. iOS always renders this card with the dark
// navy heroGradient + hardcoded white/white-opacity text regardless of system
// light/dark mode (no light-mode branch exists in the Swift source) — mirrored
// exactly rather than switching to HeroGradientLight like the Home hero card
// does. No entrance animation ported (plan explicitly scopes this out — no
// compiler available to verify AnimatedVisibility edge cases, static is safer).

data class PodiumEntry(
    val id: String,
    val displayName: String,
    val avatarEmoji: String,
    val minutes: Int,
    val streak: Int,
    val leagueBadge: LeagueBadge,
    val rank: Int,
    val isCurrentUser: Boolean,
)

@Composable
fun PodiumView(entries: List<PodiumEntry>, modifier: Modifier = Modifier) {
    val first = entries.firstOrNull { it.rank == 1 }
    val second = entries.firstOrNull { it.rank == 2 }
    val third = entries.firstOrNull { it.rank == 3 }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdKanSpacing.cardCornerRadius))
            .background(HeroGradientDark)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (second != null) PodiumColumn(entry = second, barHeight = 80.dp, modifier = Modifier.weight(1f))
        if (first != null) PodiumColumn(entry = first, barHeight = 110.dp, isFirst = true, modifier = Modifier.weight(1f))
        if (third != null) PodiumColumn(entry = third, barHeight = 60.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumColumn(entry: PodiumEntry, barHeight: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, isFirst: Boolean = false) {
    val avatarSize = if (isFirst) 68.dp else 52.dp

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isFirst) {
            Text(text = "👑", fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
        }

        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(if (entry.isCurrentUser) BrandGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = entry.avatarEmoji, fontSize = if (isFirst) 34.sp else 26.sp)
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = entry.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (entry.isCurrentUser) BrandGreen else Color.White.copy(alpha = 0.90f),
            maxLines = 1,
        )

        Spacer(Modifier.height(2.dp))

        if (entry.minutes > 0) {
            Text(
                text = formatMinutesHebrew(entry.minutes),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.60f),
                maxLines = 1,
            )
        } else {
            Text(text = "---", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.30f))
        }

        Spacer(Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (entry.streak > 0) {
                Text(text = "🔥${entry.streak}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.55f))
            }
            if (entry.leagueBadge.displayable) {
                Text(text = entry.leagueBadge.emoji, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(podiumBarColor(entry.rank))
                .border(width = 1.dp, color = podiumBorderColor(entry.rank), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = "#${entry.rank}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

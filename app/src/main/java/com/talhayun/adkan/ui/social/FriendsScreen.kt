package com.talhayun.adkan.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talhayun.adkan.ui.home.formatMinutesHebrew
import com.talhayun.adkan.ui.shared.LeagueBadge
import com.talhayun.adkan.ui.shared.RankedMemberRow
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.minutesColor

// [SKILL-DECL] Ported from iOS App/Social/FriendsListView.swift per plan
// section 6: title, RankedMemberRow list, top row (rank 1) gets the
// green-tinted highlight matching the screenshot — RankedMemberRow already
// applies that tint whenever isCurrentUser is true, so the sample data marks
// the top-ranked friend as the current user to get the same visual without a
// separate highlight parameter. Sample/fake data only, ~9 entries matching
// the screenshot's name set.

private data class FriendEntry(
    val rank: Int,
    val emoji: String,
    val name: String,
    val minutes: Int,
    val streak: Int,
    val badge: LeagueBadge,
    val isCurrentUser: Boolean,
)

private val sampleGoal = 120
private val sampleFriends = listOf(
    FriendEntry(1, "🐸", "טל", 40, 6, LeagueBadge.NONE, isCurrentUser = true),
    FriendEntry(2, "🍩", "עדי", 55, 3, LeagueBadge.SILVER, isCurrentUser = false),
    FriendEntry(3, "🤩", "נועה", 61, 2, LeagueBadge.BRONZE, isCurrentUser = false),
    FriendEntry(4, "😎", "ליאור", 75, 5, LeagueBadge.NONE, isCurrentUser = false),
    FriendEntry(5, "🦄", "עומר", 90, 0, LeagueBadge.NONE, isCurrentUser = false),
    FriendEntry(6, "🐻", "איתי", 0, 0, LeagueBadge.NONE, isCurrentUser = false),
    FriendEntry(7, "🦊", "דניאל", 0, 1, LeagueBadge.NONE, isCurrentUser = false),
    FriendEntry(8, "🥳", "רוני", 0, 4, LeagueBadge.NONE, isCurrentUser = false),
    FriendEntry(9, "😈", "יעל", 0, 0, LeagueBadge.NONE, isCurrentUser = false),
)

@Composable
fun FriendsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AdKanSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "חברים",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        sampleFriends.forEach { friend ->
            RankedMemberRow(
                rank = friend.rank,
                avatarEmoji = friend.emoji,
                displayName = friend.name,
                formattedMinutes = formatMinutesHebrew(friend.minutes),
                hasMinutesData = friend.minutes > 0,
                minutesColor = minutesColor(friend.minutes, sampleGoal),
                streak = friend.streak,
                badge = friend.badge,
                isCurrentUser = friend.isCurrentUser,
            )
        }
    }
}

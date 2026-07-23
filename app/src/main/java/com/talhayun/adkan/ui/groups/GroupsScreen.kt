package com.talhayun.adkan.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.ui.home.formatMinutesHebrew
import com.talhayun.adkan.ui.shared.LeagueBadge
import com.talhayun.adkan.ui.shared.RankedMemberRow
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.CardTitle
import com.talhayun.adkan.ui.theme.minutesColor
import kotlinx.coroutines.launch

// [SKILL-DECL] Ported from iOS App/Visualization/GroupsListView.swift +
// LeaderboardView.swift structure per plan/serialized-tinkering-pony-agent-
// a605d627b562c6fac.md section 5: title, group name+star sub-row, PodiumView,
// today/week toggle (week locked — non-functional, matches screenshot's lock
// icon gating), ranked list (RankedMemberRow, podium members reappear per
// screenshot), "כל הקבוצות" section. Sample/fake data only — not wired to
// Supabase/GroupService yet.

private data class GroupMember(
    val id: String,
    val rank: Int,
    val emoji: String,
    val name: String,
    val minutes: Int,
    val streak: Int,
    val badge: LeagueBadge,
    val wins: Int,
    val isCurrentUser: Boolean,
)

private val sampleGroupGoal = 120
private val sampleGroupMembers = listOf(
    GroupMember("1", 1, "🐸", "טל", 40, 6, LeagueBadge.NONE, 4, isCurrentUser = true),
    GroupMember("2", 2, "⭐", "רוני", 55, 3, LeagueBadge.SILVER, 2, isCurrentUser = false),
    GroupMember("3", 3, "💪", "איתי", 87, 1, LeagueBadge.BRONZE, 1, isCurrentUser = false),
    GroupMember("4", 4, "🍩", "עדי", 102, 0, LeagueBadge.NONE, 0, isCurrentUser = false),
    GroupMember("5", 5, "🤩", "ליאור", 0, 0, LeagueBadge.NONE, 0, isCurrentUser = false),
)

private data class OtherGroup(val name: String, val emoji: String, val memberCount: Int, val isFavorite: Boolean)

private val sampleOtherGroups = listOf(
    OtherGroup("המשפחה", "👨‍👩‍👧", 4, isFavorite = false),
    OtherGroup("עבודה", "💼", 6, isFavorite = false),
)

@Composable
fun GroupsScreen() {
    var selectedRange by remember { mutableStateOf(GroupsRange.TODAY) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(AdKanSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AdKanSpacing.cardSpacing),
        ) {
            Text(text = "קבוצות", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⭐ ", fontSize = 16.sp)
                Text(text = "החברי'ה", style = CardTitle)
            }

            PodiumView(
                entries = sampleGroupMembers.take(3).map {
                    PodiumEntry(
                        id = it.id,
                        displayName = it.name,
                        avatarEmoji = it.emoji,
                        minutes = it.minutes,
                        streak = it.streak,
                        leagueBadge = it.badge,
                        rank = it.rank,
                        isCurrentUser = it.isCurrentUser,
                    )
                },
            )

            RangeToggle(
                selected = selectedRange,
                onSelect = { tapped ->
                    val upsell = tapped.upsellMessage()
                    if (upsell != null) {
                        scope.launch { snackbarHostState.showSnackbar(upsell) }
                    } else {
                        selectedRange = tapped
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                sampleGroupMembers.forEachIndexed { index, member ->
                    RankedMemberRow(
                        rank = member.rank,
                        avatarEmoji = member.emoji,
                        displayName = member.name,
                        formattedMinutes = formatMinutesHebrew(member.minutes),
                        hasMinutesData = member.minutes > 0,
                        minutesColor = minutesColor(member.minutes, sampleGroupGoal),
                        streak = member.streak,
                        badge = member.badge,
                        isCurrentUser = member.isCurrentUser,
                        wins = member.wins,
                        flat = true,
                    )
                    if (index < sampleGroupMembers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 54.dp, end = 12.dp),
                            thickness = Dp.Hairline,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }

            Text(text = "כל הקבוצות", style = CardTitle)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                sampleOtherGroups.forEachIndexed { index, group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${group.emoji} ", fontSize = 18.sp)
                            Text(text = group.name, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${group.memberCount} חברים",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < sampleOtherGroups.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = Dp.Hairline,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeToggle(selected: GroupsRange, onSelect: (GroupsRange) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GroupsRange.entries.forEach { range ->
            val isSelected = range == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelect(range) }
                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (range.locked) {
                    Text(text = "🔒 ", fontSize = 12.sp)
                }
                Text(
                    text = range.label,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (range.locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

package com.talhayun.adkan.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.talhayun.adkan.backend.AuthService
import com.talhayun.adkan.backend.GoogleIdTokenProvider
import com.talhayun.adkan.backend.SupabaseConfig
import com.talhayun.adkan.onboarding.ProfilePrefs
import com.talhayun.adkan.permissions.UsageAccessPermission
import com.talhayun.adkan.ui.home.formatMinutesHebrew
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.AppearanceMode
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.BrandPurple
import com.talhayun.adkan.ui.theme.SuccessGreen
import com.talhayun.adkan.ui.theme.WarningOrange
import kotlinx.coroutines.launch

// [SKILL-DECL] Ported from iOS App/Settings/SettingsView.swift (section order:
// sign-in banner, profile, language, appearance, daily goal, permissions,
// notifications, premium, retake survey, legal links, sign out/delete,
// version) and App/Localizable.xcstrings for the exact Hebrew copy
// (settings.* / permission.* keys, read directly from source — not
// re-translated). Row styling (icon+label on the leading/right side under
// RTL, value+chevron on the trailing/left side) matches screenshots
// "WhatsApp Image 2026-07-11 at 20.14.31.jpeg" and "...20.333314.02.jpeg"
// exactly: dropdown-style value rows for language/appearance/daily-goal
// rather than the previous segmented-pill selectors, which the real app
// does not use. Founder direction (2026-07-17): screenshots are ground
// truth over the plan file where they disagree; this file follows the
// screenshots. Backend hooks from the prior pass (authService Google
// sign-in, UsageAccessPermission, sign-out) are preserved as-is — this is a
// copy/layout pass only, no interaction logic removed.

private val dailyGoalOptions = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)

private enum class AppLanguage(val label: String) {
    HEBREW("עברית"),
    ENGLISH("English"),
}

@Composable
fun SettingsScreen(
    authService: AuthService,
    appearanceMode: AppearanceMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignedIn by remember { mutableStateOf(authService.isAuthenticated) }
    var displayName by remember { mutableStateOf(ProfilePrefs.load(context).displayName) }
    var avatarEmoji by remember { mutableStateOf(ProfilePrefs.load(context).avatarEmoji) }
    var language by remember { mutableStateOf(AppLanguage.HEBREW) }
    var goalMinutes by remember { mutableStateOf(30) }
    var screenTimeAuthorized by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var eveningReminder by remember { mutableStateOf(true) }
    var weeklyCheckin by remember { mutableStateOf(true) }
    var goalCelebration by remember { mutableStateOf(true) }
    var inactivityReminder by remember { mutableStateOf(true) }
    var percentileAlert by remember { mutableStateOf(true) }
    var groupWinnerNotification by remember { mutableStateOf(true) }
    var isPremium by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var appearanceMenuExpanded by remember { mutableStateOf(false) }
    var goalMenuExpanded by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    // Re-check both permission state and auth state whenever the user comes
    // back to the app (e.g. returning from the Usage Access settings screen).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                screenTimeAuthorized = UsageAccessPermission.isGranted(context)
                isSignedIn = authService.isAuthenticated
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        screenTimeAuthorized = UsageAccessPermission.isGranted(context)
    }

    fun signInWithGoogle() {
        signInError = null
        scope.launch {
            try {
                val idToken = GoogleIdTokenProvider.requestIdToken(
                    context = context,
                    webClientId = SupabaseConfig.googleWebClientId,
                )
                authService.signInWithGoogle(idToken)
                isSignedIn = authService.isAuthenticated
            } catch (t: Throwable) {
                signInError = "ההתחברות נכשלה. נסו שוב."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AdKanSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AdKanSpacing.cardSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(text = "→  חזרה")
            }
        }

        Text(text = "הגדרות", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        if (!isSignedIn) {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { signInWithGoogle() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarBubble(emoji = "👤", background = BrandGreen.copy(alpha = 0.12f))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "התחברות לעד כאן", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = signInError ?: "הצטרפו לקבוצות והתחרו עם חברים",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (signInError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifEmpty { "הגדירו שם" },
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "עריכת פרופיל",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandGreen,
                    )
                }
                Spacer(Modifier.width(14.dp))
                AvatarBubble(emoji = avatarEmoji, background = BrandGreen.copy(alpha = 0.12f))
            }
        }

        SettingsCard {
            SettingsValueRow(
                icon = "🌐",
                title = "שפה",
                value = language.label,
                expanded = languageMenuExpanded,
                onClick = { languageMenuExpanded = true },
                menuContent = {
                    AppLanguage.entries.forEach { option ->
                        DropdownOption(text = option.label) {
                            language = option
                            languageMenuExpanded = false
                        }
                    }
                },
                onDismissMenu = { languageMenuExpanded = false },
            )
        }

        SettingsCard {
            SettingsValueRow(
                icon = "🎨",
                title = "מראה",
                value = appearanceMode.label,
                expanded = appearanceMenuExpanded,
                onClick = { appearanceMenuExpanded = true },
                menuContent = {
                    AppearanceMode.entries.forEach { option ->
                        DropdownOption(text = option.label) {
                            onAppearanceModeChange(option)
                            appearanceMenuExpanded = false
                        }
                    }
                },
                onDismissMenu = { appearanceMenuExpanded = false },
            )
        }

        SettingsCard {
            SettingsValueRow(
                icon = "🎯",
                title = "יעד יומי",
                value = formatMinutesHebrew(goalMinutes),
                expanded = goalMenuExpanded,
                onClick = { goalMenuExpanded = true },
                menuContent = {
                    dailyGoalOptions.forEach { minutes ->
                        DropdownOption(text = formatMinutesHebrew(minutes)) {
                            goalMinutes = minutes
                            goalMenuExpanded = false
                        }
                    }
                },
                onDismissMenu = { goalMenuExpanded = false },
            )
        }

        SettingsSectionHeader(text = "הרשאות")
        SettingsCard {
            if (screenTimeAuthorized) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✅ ", fontSize = 16.sp, color = SuccessGreen)
                    Text(text = "הרשאת זמן מסך פעילה")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsLauncher.launch(UsageAccessPermission.settingsIntent()) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "⏳ ", fontSize = 16.sp)
                    Column {
                        Text(text = "הפעלת הרשאת זמן מסך", fontWeight = FontWeight.Medium)
                        Text(
                            text = "נדרש כדי לעקוב אחרי זמן המסך היומי שלך",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SettingsSectionHeader(text = "התראות")
        SettingsCard {
            NotificationToggleRow(
                icon = "🌙",
                title = "תזכורת ערב",
                description = "תזכורת יומית בשעה 21:00 להוריד את הטלפון",
                checked = eveningReminder,
                onCheckedChange = { eveningReminder = it },
            )
            SettingsDivider()
            NotificationToggleRow(
                icon = "📅",
                title = "סיכום שבועי",
                description = "סיכום שבועי כל שבת בשעה 19:00",
                checked = weeklyCheckin,
                onCheckedChange = { weeklyCheckin = it },
            )
            SettingsDivider()
            NotificationToggleRow(
                icon = "🎉",
                title = "חגיגת יעד",
                description = "חגיגה כשנשארים מתחת ליעד היומי",
                checked = goalCelebration,
                onCheckedChange = { goalCelebration = it },
            )
            SettingsDivider()
            NotificationToggleRow(
                icon = "🔄",
                title = "תזכורת חזרה",
                description = "תזכורת אחרי יומיים בלי לפתוח את האפליקציה",
                checked = inactivityReminder,
                onCheckedChange = { inactivityReminder = it },
            )
            SettingsDivider()
            NotificationToggleRow(
                icon = "📊",
                title = "התראת דירוג בין המשתמשים",
                description = "התראת צהריים כשאתם ב-15% המובילים ומטה — עד פעמיים בשבוע",
                checked = percentileAlert,
                onCheckedChange = { percentileAlert = it },
            )
            SettingsDivider()
            NotificationToggleRow(
                icon = "🏆",
                title = "מנצח/ת הקבוצה",
                description = "סיכום בוקר על מי שניצח/ה אתמול בקבוצה המסומנת",
                checked = groupWinnerNotification,
                onCheckedChange = { groupWinnerNotification = it },
            )
        }

        SettingsSectionHeader(text = "פרימיום")
        SettingsCard {
            if (isPremium) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✨ ", fontSize = 16.sp)
                    Text(text = "פרימיום פעיל", color = BrandPurple, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPremium = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "👑 ", fontSize = 16.sp)
                    Text(text = "שדרגו לפרימיום", fontWeight = FontWeight.Medium)
                }
            }
        }

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* would restart onboarding — not wired up yet */ },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "🔁 ", fontSize = 16.sp)
                Text(text = "מילוי שאלון מחדש", color = BrandGreen)
            }
        }

        SettingsCard {
            LegalLinkRow(
                icon = "🔒",
                title = "מדיניות פרטיות",
                url = "https://adkan-landing-page-tal.vercel.app/legal/privacy",
                uriHandler = uriHandler,
            )
            SettingsDivider()
            LegalLinkRow(
                icon = "📄",
                title = "תנאי שימוש",
                url = "https://adkan-landing-page-tal.vercel.app/legal/terms",
                uriHandler = uriHandler,
            )
            SettingsDivider()
            LegalLinkRow(
                icon = "✉️",
                title = "יצירת קשר",
                url = "mailto:adkanapp@gmail.com",
                uriHandler = uriHandler,
            )
        }

        if (isSignedIn) {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutConfirm = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "🚪 ", fontSize = 16.sp)
                    Text(text = "התנתקות", color = WarningOrange)
                }
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirm = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "🗑️ ", fontSize = 16.sp)
                    Text(text = "מחיקת חשבון", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "גרסה", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "0.1", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(text = "להתנתק?") },
            confirmButton = {
                TextButton(onClick = {
                    authService.signOut()
                    isSignedIn = authService.isAuthenticated
                    showSignOutConfirm = false
                }) { Text(text = "התנתקות", color = WarningOrange) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text(text = "ביטול") }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = "למחוק את החשבון?") },
            text = { Text(text = "כל המידע יימחק. אי אפשר לשחזר.") },
            confirmButton = {
                TextButton(onClick = {
                    isSignedIn = false
                    showDeleteConfirm = false
                }) { Text(text = "מחיקת חשבון", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(text = "ביטול") }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        thickness = Dp.Hairline,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

@Composable
private fun AvatarBubble(emoji: String, background: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

/**
 * Dropdown-style value row matching screenshots "...20.14.31.jpeg" /
 * "...20.333314.02.jpeg": icon + label on the leading (right, under RTL)
 * side, current value + a small down-chevron on the trailing (left) side —
 * not the segmented-pill selector this file used before the visual-parity
 * pass. Tapping the value opens a dropdown menu built by [menuContent].
 */
@Composable
private fun SettingsValueRow(
    icon: String,
    title: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    onDismissMenu: () -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(text = icon, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
        ) {
            menuContent()
        }
    }
}

@Composable
private fun DropdownOption(text: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = text) },
        onClick = onClick,
    )
}

@Composable
private fun NotificationToggleRow(
    icon: String,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = icon, fontSize = 18.sp)
    }
}

@Composable
private fun LegalLinkRow(
    icon: String,
    title: String,
    url: String,
    uriHandler: UriHandler,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$icon ", fontSize = 16.sp)
        Text(text = title)
    }
}

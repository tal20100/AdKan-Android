package com.talhayun.adkan.ui.blocking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.permissions.AccessibilityServicePermission
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.BrandPurple
import com.talhayun.adkan.ui.theme.WarningOrange

// [SKILL-DECL] Ported from iOS App/ScreenTime/BlockingView.swift per plan
// section 7: green-gradient hero (shield icon, title, subtitle), focus-timer
// row, block-toggle (shows orange "30 דקות" caption + footer), app-selection
// row (real installed-app picker, see AppPickerDialog/InstalledApps), always-
// block toggle with lock icon, then 3 premium-gated sections rendered dimmed
// behind a purple "פרימיום" pill + lock icon (tap is a no-op, real premium
// gating not implemented), final warning-icon entitlement footer note.
// Toggle state and app selection now persist via BlockingPrefs
// (SharedPreferences) instead of resetting every app restart — still no real
// FamilyControls-equivalent enforcement wired up (that needs an accessibility
// service or usage-stats polling loop, out of scope for this pass).

private val greenHeroGradient = Brush.verticalGradient(listOf(Color(0xFF5FB85A), BrandGreen))

@Composable
fun BlockingScreen() {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityServicePermission.isEnabled(context)) }
    val accessibilitySettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        isAccessibilityEnabled = AccessibilityServicePermission.isEnabled(context)
    }
    var blockingEnabled by remember { mutableStateOf(BlockingPrefs.isBlockingEnabled(context)) }
    var alwaysBlockEnabled by remember { mutableStateOf(BlockingPrefs.isAlwaysBlockEnabled(context)) }
    var selectedApps by remember { mutableStateOf(BlockingPrefs.selectedApps(context)) }
    var showAppPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AdKanSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AdKanSpacing.cardSpacing),
    ) {
        Text(text = "חסימת אפליקציות", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        HeroCard()

        AccessibilityStatusRow(
            isEnabled = isAccessibilityEnabled,
            onEnableClick = { accessibilitySettingsLauncher.launch(AccessibilityServicePermission.settingsIntent()) },
        )

        FocusTimerRow()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ToggleRow(
                title = "הפעלת חסימה",
                checked = blockingEnabled,
                onCheckedChange = {
                    blockingEnabled = it
                    BlockingPrefs.setBlockingEnabled(context, it)
                },
            )
            if (blockingEnabled) {
                Text(text = "30 דקות", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarningOrange)
            }
            FooterNote(text = "כאשר החסימה פעילה, האפליקציות שנבחרו ייחסמו לאחר חצי שעה של שימוש מצטבר.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader(text = "חסימת אפליקציות")
            AppSelectionRow(selectedCount = selectedApps.size, onClick = { showAppPicker = true })
            FooterNote(text = "רק שם האפליקציה נחסם — הנתונים על השימוש בה לא עוזבים את המכשיר.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ToggleRow(
                title = "חסום תמיד את האפליקציות האלו",
                checked = alwaysBlockEnabled,
                onCheckedChange = {
                    alwaysBlockEnabled = it
                    BlockingPrefs.setAlwaysBlockEnabled(context, it)
                },
                leadingEmoji = "🔒",
            )
        }

        PremiumGatedSection(title = "חסימה לפי שעות", footer = "קבעו שעות קבועות בהן החסימה תהיה פעילה אוטומטית.")
        PremiumGatedSection(title = "מצב קשוח", footer = "מקשה על עקיפת החסימה — דורש המתנה ואתגר קצר.")
        PremiumGatedSection(title = "עיצוב מסך חסימה", footer = "התאימו אישית את מסך החסימה שחברים לא רואים.")

        FooterNote(text = "⚠️ חסימה בפועל דורשת הרשאת גישה לזמן מסך פעילה.")
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = remember { installedLaunchableApps(context) },
            initiallySelected = selectedApps,
            onDismiss = { showAppPicker = false },
            onConfirm = { newSelection ->
                selectedApps = newSelection
                BlockingPrefs.setSelectedApps(context, newSelection)
                showAppPicker = false
            },
        )
    }
}

@Composable
private fun HeroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdKanSpacing.cardCornerRadius))
            .background(greenHeroGradient)
            .padding(AdKanSpacing.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "🛡️", fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "חסימת אפליקציות מסיחות",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "מגבלות יומיות לאפליקציות שגונבות זמן",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun FocusTimerRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🧠 ", fontSize = 18.sp)
            Column {
                Text(text = "טיימר פוקוס", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "חסימת אפליקציות לזמן קצוב",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccessibilityStatusRow(isEnabled: Boolean, onEnableClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isEnabled) "✅ " else "⚠️ ", fontSize = 16.sp)
            Text(
                text = if (isEnabled) "החסימה פעילה במכשיר" else "החסימה דורשת הפעלת שירות נגישות",
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!isEnabled) {
            androidx.compose.material3.TextButton(onClick = onEnableClick) {
                Text(text = "הפעלה")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingEmoji: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingEmoji != null) {
                Text(text = "$leadingEmoji ", fontSize = 16.sp)
            }
            Text(text = title, fontWeight = FontWeight.SemiBold)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = BrandGreen, checkedTrackColor = BrandGreen.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun AppSelectionRow(selectedCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "בחירת אפליקציות לחסימה", fontWeight = FontWeight.SemiBold)
        Text(text = "$selectedCount נבחרו", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandGreen)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun FooterNote(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
    )
}

/**
 * Premium-gated section: dimmed row behind a purple "פרימיום" pill + lock —
 * tapping does nothing, matching the screenshots' locked treatment exactly
 * (no real entitlement check wired up, this is a visual gate only).
 */
@Composable
private fun PremiumGatedSection(title: String, footer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(BrandPurple.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "🔒 ", fontSize = 11.sp)
                Text(text = "פרימיום", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandPurple)
            }
        }
        FooterNote(text = footer)
    }
}

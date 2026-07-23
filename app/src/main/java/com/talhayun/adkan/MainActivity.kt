package com.talhayun.adkan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.backend.AuthService
import com.talhayun.adkan.backend.BackendServices
import com.talhayun.adkan.ui.blocking.BlockingScreen
import com.talhayun.adkan.ui.groups.GroupsScreen
import com.talhayun.adkan.ui.home.HomeScreen
import com.talhayun.adkan.ui.onboarding.OnboardingFlow
import com.talhayun.adkan.ui.settings.SettingsScreen
import com.talhayun.adkan.ui.social.FriendsScreen
import com.talhayun.adkan.ui.theme.AdKanTheme
import com.talhayun.adkan.ui.theme.AppearanceMode
import com.talhayun.adkan.ui.theme.AppearancePrefs
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.DarkSurface

// Top-level screen state for this first pass — no Navigation-Compose dependency
// added yet, just a simple in-memory state machine matching the existing
// single-module, sample-data-only scope. Onboarding shows once per process
// launch (not persisted) since there's no DataStore/Supabase wiring yet.
private enum class RootScreen { ONBOARDING, MAIN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [SKILL-DECL] plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md
        // — construct BackendServices once here, Android analog of iOS
        // ServiceContainer, and pass it down to the two screens that need it.
        val authService = BackendServices.auth(applicationContext)
        setContent {
            val context = LocalContext.current
            // [SKILL-DECL] Previously the Settings "מראה" (appearance) picker
            // was local-only Compose state with zero effect on the actual
            // theme — AdKanTheme always followed the system setting
            // regardless of what the user picked. Hoisted here (above
            // AdKanTheme, so it can actually control darkTheme) and
            // persisted via AppearancePrefs, mirroring BlockingPrefs' pattern.
            var appearanceMode by remember { mutableStateOf(AppearancePrefs.load(context)) }
            val isDark = when (appearanceMode) {
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }

            AdKanTheme(darkTheme = isDark) {
                // [SKILL-DECL] Ported from screenshots in
                // AdKan/.claude/images-for-reference/screenshots — every screen is
                // Hebrew-first RTL (tab bar reads home-rightmost, cards read
                // right-to-left). Forced explicitly rather than relying on device
                // locale, since the app content is Hebrew regardless of system
                // language until real i18n switching is wired up.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppRoot(
                            authService = authService,
                            appearanceMode = appearanceMode,
                            onAppearanceModeChange = {
                                appearanceMode = it
                                AppearancePrefs.save(context, it)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    authService: AuthService,
    appearanceMode: AppearanceMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
) {
    var screen by remember { mutableStateOf(RootScreen.ONBOARDING) }
    var tab by remember { mutableStateOf(MainTab.HOME) }

    when (screen) {
        RootScreen.ONBOARDING -> OnboardingFlow(
            authService = authService,
            onComplete = { screen = RootScreen.MAIN },
        )
        RootScreen.MAIN -> Scaffold(
            bottomBar = {
                AdKanBottomBar(selected = tab, onSelect = { tab = it })
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (tab) {
                    MainTab.HOME -> HomeScreen(onFocusClick = { tab = MainTab.BLOCKING })
                    MainTab.FRIENDS -> FriendsScreen()
                    MainTab.GROUPS -> GroupsScreen()
                    MainTab.BLOCKING -> BlockingScreen()
                    MainTab.SETTINGS -> SettingsScreen(
                        authService = authService,
                        appearanceMode = appearanceMode,
                        onAppearanceModeChange = onAppearanceModeChange,
                        onBack = { tab = MainTab.HOME },
                    )
                }
            }
        }
    }
}

// [SKILL-DECL] Custom floating capsule tab bar built from plain Compose
// primitives, matching the existing hand-rolled UI style in
// RankedMemberRow.kt and GroupsScreen.kt's RangeToggle, rather than a
// stock Material3 NavigationBar. This mirrors the iOS RootView.swift
// TabView's ultraThinMaterial floating capsule look (see the AdKan
// reference screenshots) instead of a full width, flush, stock bar.
@Composable
private fun AdKanBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(DarkSurface.copy(alpha = 0.94f))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { entry ->
            val isSelected = entry == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(entry) }
                    .background(if (isSelected) BrandGreen.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.label,
                    tint = if (isSelected) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = entry.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) BrandGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

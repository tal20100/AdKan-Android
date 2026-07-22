package com.talhayun.adkan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.talhayun.adkan.backend.AuthService
import com.talhayun.adkan.backend.BackendServices
import com.talhayun.adkan.ui.blocking.BlockingScreen
import com.talhayun.adkan.ui.groups.GroupsScreen
import com.talhayun.adkan.ui.home.HomeScreen
import com.talhayun.adkan.ui.onboarding.OnboardingFlow
import com.talhayun.adkan.ui.settings.SettingsScreen
import com.talhayun.adkan.ui.social.FriendsScreen
import com.talhayun.adkan.ui.theme.AdKanTheme

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
            AdKanTheme {
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
                        AppRoot(authService = authService)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(authService: AuthService) {
    var screen by remember { mutableStateOf(RootScreen.ONBOARDING) }
    var tab by remember { mutableStateOf(MainTab.HOME) }

    when (screen) {
        RootScreen.ONBOARDING -> OnboardingFlow(
            authService = authService,
            onComplete = { screen = RootScreen.MAIN },
        )
        RootScreen.MAIN -> Scaffold(
            bottomBar = {
                NavigationBar {
                    MainTab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(imageVector = entry.icon, contentDescription = entry.label) },
                            label = { Text(text = entry.label) },
                        )
                    }
                }
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
                        onBack = { tab = MainTab.HOME },
                    )
                }
            }
        }
    }
}

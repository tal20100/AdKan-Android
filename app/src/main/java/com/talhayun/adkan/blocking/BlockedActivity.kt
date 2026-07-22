package com.talhayun.adkan.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.AdKanTheme
import com.talhayun.adkan.ui.theme.BrandGreen

/**
 * Full-screen interruption shown over a blocked app. Standalone Activity
 * (not part of MainActivity's Compose tree), so it must explicitly force
 * RTL itself rather than inheriting it — see this plan's Global Constraints.
 * No PackageManager lookups here; the caller (AppBlockAccessibilityService)
 * resolves the blocked app's display name and passes it in via Intent extra.
 */
class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appLabel = intent.getStringExtra(EXTRA_BLOCKED_APP_LABEL) ?: ""

        setContent {
            AdKanTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AdKanSpacing.screenPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = "🛡️", fontSize = 56.sp)

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "עד כאן",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = if (appLabel.isNotBlank()) {
                                    "בחרתם להגביל את $appLabel. הגיע הזמן להתנתק להיום."
                                } else {
                                    "בחרתם להגביל את האפליקציה הזו. הגיע הזמן להתנתק להיום."
                                },
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            Spacer(Modifier.height(32.dp))

                            Button(
                                onClick = { goHome() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            ) {
                                Text(text = "חזרה למסך הבית")
                            }
                        }
                    }
                }
            }
        }
    }

    /** Sends the user to the device home screen — never back into the blocked app. */
    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onBackPressed() {
        // Deliberately does not call super.onBackPressed() / does not finish()
        // back into the blocked app — back button also goes home, matching
        // the primary button's behavior. There is no path from this screen
        // back into the app that triggered it.
        goHome()
    }

    companion object {
        const val EXTRA_BLOCKED_APP_LABEL = "blockedAppLabel"
    }
}

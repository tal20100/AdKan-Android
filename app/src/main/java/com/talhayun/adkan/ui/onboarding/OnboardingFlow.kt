package com.talhayun.adkan.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.R
import com.talhayun.adkan.backend.AuthService
import com.talhayun.adkan.backend.GoogleIdTokenProvider
import com.talhayun.adkan.backend.SupabaseConfig
import com.talhayun.adkan.onboarding.Profile
import com.talhayun.adkan.onboarding.ProfilePrefs
import com.talhayun.adkan.permissions.UsageAccessPermission
import com.talhayun.adkan.ui.theme.AdKanSpacing
import com.talhayun.adkan.ui.theme.BrandGreen
import kotlinx.coroutines.launch

// [SKILL-DECL] Ported from iOS App/Onboarding/OnboardingView.swift +
// App/Onboarding/ProfileSetupView.swift (page order: welcome -> permission ->
// survey questions -> sign-in -> profile setup) and App/Localizable.xcstrings
// for the exact Hebrew copy (onboarding.*, permission.* keys, read directly
// from source — not re-translated). Only 2 of the 5 iOS survey questions are
// ported here (q1 "hours per day" and q4 "who will you compete with"),
// matching the "a couple of survey-style question screens" scope for this
// pass. Per plan/serialized-tinkering-pony-agent-a78532b0c00bea6c9.md,
// PermissionStep and SignInStep do real work: Usage Access permission via
// UsageAccessPermission + Settings.ACTION_USAGE_ACCESS_SETTINGS, and Google
// Sign-In via GoogleIdTokenProvider (Credential Manager) -> AuthService ->
// Supabase auth/v1/token — untouched by this visual-parity pass. This pass:
// (1) replaced the emoji mascot placeholder with the real logo_no_bg.png
// asset per iOS OnboardingView.swift lines 91/124 (Image("logo_no_bg") on
// both welcome and question pages), (2) translated the remaining English
// leftover copy (Enable Screen Time / Continue / Sign in with Google / etc.)
// to Hebrew so the flow doesn't code-switch mid-onboarding, matching every
// real screenshot. Everything else in this file stays stub/local state,
// matching the file's original scope.

private enum class OnboardingStep {
    WELCOME,
    PERMISSION,
    QUESTION_HOURS,
    QUESTION_COMPETE_WITH,
    SIGN_IN,
    PROFILE,
}

private val stepOrder = OnboardingStep.entries

/**
 * Onboarding flow mirroring the conceptual iOS page order. The permission and
 * sign-in steps now do real work (Usage Access + Google Sign-In via
 * [authService]); the survey and profile steps remain fake/local state, per
 * this file's original scope. [onComplete] fires once the stubbed profile
 * save "succeeds".
 */
@Composable
fun OnboardingFlow(authService: AuthService, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }

    fun goTo(next: OnboardingStep) {
        step = next
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (step != OnboardingStep.WELCOME) {
            val progress = (stepOrder.indexOf(step)).toFloat() / (stepOrder.size - 1).toFloat()
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AdKanSpacing.screenPadding, vertical = 8.dp),
                color = BrandGreen,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(onContinue = { goTo(OnboardingStep.PERMISSION) })
                OnboardingStep.PERMISSION -> PermissionStep(
                    onAllow = { goTo(OnboardingStep.QUESTION_HOURS) },
                    onMaybeLater = { goTo(OnboardingStep.QUESTION_HOURS) },
                )
                OnboardingStep.QUESTION_HOURS -> QuestionStep(
                    prompt = "כמה שעות ביום על הטלפון?",
                    options = listOf("1-2 שעות", "3-4 שעות", "5-6 שעות", "7+ שעות"),
                    onAnswer = { goTo(OnboardingStep.QUESTION_COMPETE_WITH) },
                    onSkip = { goTo(OnboardingStep.QUESTION_COMPETE_WITH) },
                )
                OnboardingStep.QUESTION_COMPETE_WITH -> QuestionStep(
                    prompt = "עם מי רוצים להתחרות?",
                    options = listOf("חברים", "בני משפחה", "בן/בת זוג", "עמיתים לעבודה"),
                    onAnswer = { goTo(OnboardingStep.SIGN_IN) },
                    onSkip = { goTo(OnboardingStep.SIGN_IN) },
                )
                OnboardingStep.SIGN_IN -> SignInStep(
                    authService = authService,
                    onSignedIn = { goTo(OnboardingStep.PROFILE) },
                    onContinueAsGuest = { goTo(OnboardingStep.PROFILE) },
                )
                OnboardingStep.PROFILE -> ProfileSetupStep(onComplete = onComplete)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdKanSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.logo_no_bg),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(220.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "ברוכים הבאים לעד כאן!",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "הגיע הזמן לקחת שליטה על זמן המסך. התחרו עם חברים, בנו רצפים, ותחזרו לעצמכם את היום.",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(text = "יאללה", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionStep(onAllow: () -> Unit, onMaybeLater: () -> Unit) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }

    // No runtime dialog exists for PACKAGE_USAGE_STATS — the only path is
    // Settings.ACTION_USAGE_ACCESS_SETTINGS, so we re-check on return-to-app.
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        isGranted = UsageAccessPermission.isGranted(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdKanSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(BrandGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = if (isGranted) "✅" else "🔒", fontSize = 44.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "הפעלת זמן מסך", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isGranted) {
                "הרשאת זמן מסך פעילה."
            } else {
                "עד כאן צריך גישה לזמן מסך כדי להציג את הסך היומי. שום מידע לפי אפליקציה לא יוצא מהטלפון. " +
                    "תועברו להגדרות המערכת — מצאו את עד כאן ברשימה והפעילו."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isGranted) {
            Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                Text(text = "המשך")
            }
        } else {
            Button(
                onClick = { settingsLauncher.launch(UsageAccessPermission.settingsIntent()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "אישור גישה")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onMaybeLater) {
            Text(text = "אולי אחר כך", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionStep(
    prompt: String,
    options: List<String>,
    onAnswer: (String) -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdKanSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.logo_no_bg),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(80.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        options.forEach { option ->
            OutlinedButton(
                onClick = { onAnswer(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Text(text = option)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onSkip) {
            Text(text = "דילוג", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SignInStep(
    authService: AuthService,
    onSignedIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdKanSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(text = "התחברות לעד כאן", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "הצטרפו לקבוצות והתחרו עם חברים",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                errorMessage = null
                isSigningIn = true
                scope.launch {
                    try {
                        val idToken = GoogleIdTokenProvider.requestIdToken(
                            context = context,
                            webClientId = SupabaseConfig.googleWebClientId,
                        )
                        authService.signInWithGoogle(idToken)
                        onSignedIn()
                    } catch (t: Throwable) {
                        errorMessage = "ההתחברות נכשלה. נסו שוב."
                    } finally {
                        isSigningIn = false
                    }
                }
            },
            enabled = !isSigningIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(text = "התחברות עם Google")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onContinueAsGuest) {
            Text(text = "המשך כאורח/ת", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private val curatedEmojis = listOf(
    "😎", "🤓", "😊", "🥳", "😈", "🤩", "😤", "🧐",
    "🦄", "🐱", "🐶", "🦊", "🐻", "🐼", "🐸", "🦋",
)

@Composable
private fun ProfileSetupStep(onComplete: () -> Unit) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(curatedEmojis.first()) }

    val isValid = displayName.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AdKanSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "יצירת פרופיל", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "ככה החברים שלכם יראו אתכם",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(BrandGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = selectedEmoji, fontSize = 48.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= 20) displayName = it },
            label = { Text(text = "שם תצוגה") },
            placeholder = { Text(text = "השם שלכם") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "בחירת אווטאר",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        curatedEmojis.chunked(4).forEach { rowEmojis ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                rowEmojis.forEach { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) BrandGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                ProfilePrefs.save(context, Profile(displayName = displayName, avatarEmoji = selectedEmoji))
                onComplete()
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "המשך")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

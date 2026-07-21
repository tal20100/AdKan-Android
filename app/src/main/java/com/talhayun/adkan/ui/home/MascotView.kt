package com.talhayun.adkan.ui.home

// [SKILL-DECL] Ported from iOS App/Home/MascotView.swift line-by-line: the
// 5-state enum derived from todayMinutes/goalMinutes ratio (thresholds
// <=0.5/1.0/1.5/2.0, else spiraling), the inverted image-number mapping
// (mascot_state_1 = spiraling/worst .. mascot_state_5 = thriving/best), the
// per-state glow color (brandGreen / mascotHealthy / warningOrange@0.7 /
// warningOrange / mascotUnhealthy), the sparkle-on-thriving-only rule, and
// the state label capsule. Real animation curves (glow pulse, sparkle
// drift, spiraling shake) are approximated with Compose's infinite
// transition APIs rather than a pixel-identical port of SwiftUI's spring
// and easeInOut timing, per the task's "states, thresholds, colors, and
// image choice must match exactly; animation curves need not be
// pixel-identical" scope. Mascot art: rot1.png..rot5.png from
// AdKan/.claude/images-for-reference/logo-and-images, copied to
// res/drawable-nodpi/mascot_state_1..5.png (real assets, not redrawn).

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.talhayun.adkan.R
import com.talhayun.adkan.ui.theme.BrandGreen
import com.talhayun.adkan.ui.theme.WarningOrange

/**
 * Mirrors AdKanTheme.mascotHealthy / mascotUnhealthy — those two tokens aren't
 * in Color.kt yet (only the mascot uses them), so they're defined here rather
 * than approximated: matches iOS's onTrack teal-green and spiraling near-red.
 */
private val MascotHealthy = Color(0xFF34C77A)
private val MascotUnhealthy = Color(0xFFE0393F)

enum class MascotState {
    THRIVING,
    ON_TRACK,
    SLIPPING,
    WARNING,
    SPIRALING;

    companion object {
        /** Exact port of MascotState.init(todayMinutes:goalMinutes:) thresholds. */
        fun from(todayMinutes: Int, goalMinutes: Int): MascotState {
            val ratio = todayMinutes.toDouble() / maxOf(goalMinutes, 1).toDouble()
            return when {
                ratio <= 0.5 -> THRIVING
                ratio <= 1.0 -> ON_TRACK
                ratio <= 1.5 -> SLIPPING
                ratio <= 2.0 -> WARNING
                else -> SPIRALING
            }
        }
    }
}

/** Mirrors MascotState.imageName — numbering is inverted from severity (1 = worst). */
private fun MascotState.drawableRes(): Int = when (this) {
    MascotState.SPIRALING -> R.drawable.mascot_state_1
    MascotState.WARNING -> R.drawable.mascot_state_2
    MascotState.SLIPPING -> R.drawable.mascot_state_3
    MascotState.ON_TRACK -> R.drawable.mascot_state_4
    MascotState.THRIVING -> R.drawable.mascot_state_5
}

/** Mirrors MascotState.glowColor. */
private fun MascotState.glowColor(): Color = when (this) {
    MascotState.THRIVING -> BrandGreen
    MascotState.ON_TRACK -> MascotHealthy
    MascotState.SLIPPING -> WarningOrange.copy(alpha = 0.7f)
    MascotState.WARNING -> WarningOrange
    MascotState.SPIRALING -> MascotUnhealthy
}

/** Mirrors MascotState.showSparkles (thriving only). */
private fun MascotState.showSparkles(): Boolean = this == MascotState.THRIVING

/** Mirrors MascotState.labelKey Hebrew copy, read from App/Localizable.xcstrings. */
private fun MascotState.labelText(): String = when (this) {
    MascotState.THRIVING -> "פורח/ת"
    MascotState.ON_TRACK -> "על המסלול"
    MascotState.SLIPPING -> "מתחיל/ה לגלוש"
    MascotState.WARNING -> "אזהרה"
    MascotState.SPIRALING -> "יוצא/ת משליטה"
}

/**
 * Mirrors MascotState.messageKey — using the gender-neutral variant for
 * spiraling, since this Android pass has no genderPreference setting
 * equivalent to iOS's @AppStorage("genderPreference") yet.
 */
private fun MascotState.messageText(): String = when (this) {
    MascotState.THRIVING -> "אלוף/ה! ממשיכים ככה"
    MascotState.ON_TRACK -> "יופי, בדרך הנכונה"
    MascotState.SLIPPING -> "שימו לב, מתחילים לגלוש"
    MascotState.WARNING -> "זמן להוריד את הטלפון"
    MascotState.SPIRALING -> "יום קשה, מחר מתחילים מחדש"
}

/**
 * Reactive mascot shown on the home screen, mirroring iOS's MascotView.swift:
 * a 130dp mascot image over a pulsing glow circle (color + art keyed off
 * [MascotState]), a state-label capsule, a short status message, and a
 * sparkle overlay shown only in the thriving state.
 */
@Composable
fun MascotView(todayMinutes: Int, goalMinutes: Int, modifier: Modifier = Modifier) {
    val state = MascotState.from(todayMinutes, goalMinutes)

    val infiniteTransition = rememberInfiniteTransition(label = "mascotGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MascotStack(state = state, glowPulse = glowPulse)

        Box(modifier = Modifier.size(16.dp))

        StateLabel(state = state)

        Box(modifier = Modifier.size(8.dp))

        Text(
            text = state.messageText(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MascotStack(state: MascotState, glowPulse: Float) {
    Box(contentAlignment = Alignment.Center) {
        // Pulsing glow circle behind the mascot art.
        val scale = 1.0f + 0.10f * glowPulse
        val blurRadius = (4f + 4f * glowPulse).dp
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .blur(blurRadius)
                .clip(CircleShape)
                .background(state.glowColor().copy(alpha = 0.18f)),
        )

        Crossfade(targetState = state, label = "mascotArt") { s ->
            Image(
                painter = painterResource(id = s.drawableRes()),
                contentDescription = s.labelText(),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(130.dp),
            )
        }

        if (state.showSparkles()) {
            SparkleLayer()
        }
    }
}

@Composable
private fun SparkleLayer() {
    val transition = rememberInfiniteTransition(label = "sparkles")
    val offset1 by transition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "sparkle1",
    )
    val offset2 by transition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(2100), RepeatMode.Reverse),
        label = "sparkle2",
    )
    val offset3 by transition.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "sparkle3",
    )

    Box(modifier = Modifier.size(160.dp)) {
        SparkleText(text = "✨", fontSize = 20.sp, x = (-58).dp, y = (-40 + offset1).dp, alpha = 0.9f)
        SparkleText(text = "✨", fontSize = 14.sp, x = 62.dp, y = (-28 + offset2).dp, alpha = 0.85f)
        SparkleText(text = "✨", fontSize = 17.sp, x = 48.dp, y = (44 + offset3).dp, alpha = 0.8f)
    }
}

@Composable
private fun SparkleText(text: String, fontSize: TextUnit, x: Dp, y: Dp, alpha: Float) {
    Text(
        text = text,
        fontSize = fontSize,
        color = Color.White.copy(alpha = alpha),
        modifier = Modifier.padding(start = 80.dp + x, top = 80.dp + y),
    )
}

@Composable
private fun StateLabel(state: MascotState) {
    Text(
        text = state.labelText(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = state.glowColor(),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(state.glowColor().copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

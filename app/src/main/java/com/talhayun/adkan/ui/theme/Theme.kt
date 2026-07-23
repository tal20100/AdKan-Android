package com.talhayun.adkan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Mirrors AdKanTheme's spacing constants (App/DesignSystem/Theme.swift).
object AdKanSpacing {
    val screenPadding = 20.dp
    val cardPadding = 20.dp
    val cardCornerRadius = 20.dp
    val cardSpacing = 20.dp
}

// [SKILL-DECL] Ported from App/DesignSystem/Theme.swift + the dark-mode
// screenshots in AdKan/.claude/images-for-reference/screenshots (near-black
// background, dark-gray cards, off-white text) — light mode had zero tokens
// before this pass (cards/text fell back to Material defaults, the single
// biggest correctness gap flagged in the polish plan).
private val LightColors = lightColorScheme(
    primary = BrandGreen,
    secondary = BrandPurple,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = DangerRed,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DangerRed,
)

private val AppTypography = Typography().let { default ->
    Typography(
        displayLarge = default.displayLarge.copy(fontFamily = AppRoundedFont),
        displayMedium = default.displayMedium.copy(fontFamily = AppRoundedFont),
        displaySmall = default.displaySmall.copy(fontFamily = AppRoundedFont),
        headlineLarge = default.headlineLarge.copy(fontFamily = AppRoundedFont),
        headlineMedium = default.headlineMedium.copy(fontFamily = AppRoundedFont),
        headlineSmall = default.headlineSmall.copy(fontFamily = AppRoundedFont),
        titleLarge = default.titleLarge.copy(fontFamily = AppRoundedFont),
        titleMedium = default.titleMedium.copy(fontFamily = AppRoundedFont),
        titleSmall = default.titleSmall.copy(fontFamily = AppRoundedFont),
        bodyLarge = default.bodyLarge.copy(fontFamily = AppRoundedFont),
        bodyMedium = default.bodyMedium.copy(fontFamily = AppRoundedFont),
        bodySmall = default.bodySmall.copy(fontFamily = AppRoundedFont),
        labelLarge = default.labelLarge.copy(fontFamily = AppRoundedFont),
        labelMedium = default.labelMedium.copy(fontFamily = AppRoundedFont),
        labelSmall = default.labelSmall.copy(fontFamily = AppRoundedFont),
    )
}

@Composable
fun AdKanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}

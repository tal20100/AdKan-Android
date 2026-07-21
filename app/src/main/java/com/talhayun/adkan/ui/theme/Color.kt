package com.talhayun.adkan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Converted directly from App/DesignSystem/Theme.swift's RGB float values
// (0-1 range * 255, rounded) — not approximated, computed from the exact
// source values so the palette matches iOS precisely.

val BrandGreen = Color(0xFF78C96F)
val BrandGreenLight = Color(0xFFC5EDBA)
val BrandPurple = Color(0xFFA68BF7)
val BrandPurpleLight = Color(0xFFD6BAF1)
val SurfaceDark = Color(0xFF1E1F20)
val SurfaceGray = Color(0xFF666F72)
val BrandNavy = Color(0xFF1F4E6F)

val SuccessGreen = Color(0xFF33C773)
val WarningOrange = Color(0xFFFF9E0A)
val DangerRed = Color(0xFFF2404D)

// [SKILL-DECL] Ported from App/DesignSystem/Theme.swift's `heroGradient`
// (surfaceDark -> brandNavy, top-to-bottom) and screenshot
// "WhatsApp Image 2026-07-11 at 20.14.00.jpeg" (the dark navy usage-hero
// card). iOS has no light-mode override for heroGradient — .systemBackground
// dynamically flips instead — so a light equivalent is composed here from
// Material's neutral light surfaces tinted toward BrandNavy at low alpha,
// keeping the card legible without inventing an iOS-unseen color.
val HeroGradientDark = Brush.verticalGradient(listOf(SurfaceDark, BrandNavy))
val HeroGradientLight = Brush.verticalGradient(
    listOf(Color(0xFFE8EEF2), Color(0xFFCBDCE6)),
)

/** Mirrors AdKanTheme.premiumGradient (brandPurple -> brandPurpleLight, top-leading to bottom-trailing). */
val PremiumGradient = Brush.linearGradient(listOf(BrandPurple, BrandPurpleLight))

// Light/dark surface + text tokens — App/DesignSystem/Theme.swift leans on
// iOS system dynamic colors (.systemBackground, .secondarySystemBackground,
// .label, .secondaryLabel) which have no 1:1 Android resource; these are the
// standard Material dynamic-color equivalents for those same iOS system
// colors, picked to match the screenshots' near-black dark surfaces and to
// give light mode a real (currently-missing) definition.
val LightBackground = Color(0xFFF4F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9EBEE)
val LightOnSurface = Color(0xFF1C1C1E)
val LightOnSurfaceVariant = Color(0xFF6B6B6F)

val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF2C2C2E)
val DarkOnSurface = Color(0xFFF2F2F7)
val DarkOnSurfaceVariant = Color(0xFF9B9BA1)

/** Mirrors AdKanTheme.minutesColor(_:goal:) — green under goal, orange up to 2x, red beyond. */
fun minutesColor(minutes: Int, goal: Int = 120): Color = when {
    minutes <= goal -> SuccessGreen
    minutes <= goal * 2 -> WarningOrange
    else -> DangerRed
}

// [SKILL-DECL] Ported from App/Visualization/PodiumView.swift's private
// `Int.podiumBarColor`/`podiumBorderColor` extensions — exact RGB float values,
// not approximated. Muted rank-accent colors: gold / silver / bronze.
fun podiumBarColor(rank: Int): Color = when (rank) {
    1 -> Color(red = 0.88f, green = 0.72f, blue = 0.22f, alpha = 0.35f)
    2 -> Color(red = 0.62f, green = 0.68f, blue = 0.76f, alpha = 0.28f)
    else -> Color(red = 0.78f, green = 0.52f, blue = 0.28f, alpha = 0.28f)
}

fun podiumBorderColor(rank: Int): Color = when (rank) {
    1 -> Color(red = 0.90f, green = 0.76f, blue = 0.30f, alpha = 0.55f)
    2 -> Color(red = 0.65f, green = 0.72f, blue = 0.80f, alpha = 0.40f)
    else -> Color(red = 0.80f, green = 0.55f, blue = 0.30f, alpha = 0.40f)
}

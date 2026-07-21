package com.talhayun.adkan.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Mirrors AdKanTheme's typography scale. iOS uses `.rounded` design; Android's
// closest system equivalent is FontFamily.Default (no built-in rounded system
// font on Android the way SF Rounded exists on iOS) — flagged as a visual gap,
// not silently faked. A real rounded font (e.g. bundling Google's "Nunito" or
// similar) would need to be added later to fully match.
val HeroNumber = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Default)
val HeroLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Default)
val CardTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Default)
val CardBody = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Default)

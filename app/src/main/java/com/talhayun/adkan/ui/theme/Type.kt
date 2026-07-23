package com.talhayun.adkan.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Mirrors AdKanTheme's typography scale. iOS uses `.rounded` design; Android
// now matches via AppRoundedFont (Varela Round, bundled at
// res/font/varela_round.ttf) — see AppFont.kt for details.
val HeroNumber = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = AppRoundedFont)
val HeroLabel = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = AppRoundedFont)
val CardTitle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppRoundedFont)
val CardBody = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFamily = AppRoundedFont)

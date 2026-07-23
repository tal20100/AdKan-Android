package com.talhayun.adkan.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.talhayun.adkan.R

// [SKILL-DECL] Real rounded Hebrew-capable font closing the "iOS SF Rounded
// vs Android FontFamily.Default (Roboto)" gap flagged in the visual audit.
// Varela Round (OFL-licensed, bundled at res/font/varela_round.ttf) has a
// dedicated Hebrew glyph subset (verified against Google Fonts' METADATA.pb
// before downloading — subsets include "hebrew", not just "latin"). Only a
// Regular-weight file exists; bold/semibold text using this family relies on
// Compose's default FontSynthesis.All to synthesize weight, which is
// standard, well-supported behavior, not a workaround.
val AppRoundedFont = FontFamily(Font(R.font.varela_round))

package com.example.futurediary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// We use Serif for a classic "journal" feel that is highly readable
val HandwritingFont = FontFamily.Serif

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = HandwritingFont,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 32.sp, // Explicit line height to match notebook lines
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HandwritingFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

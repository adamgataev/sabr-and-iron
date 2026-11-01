package com.example.sabr_and_iron.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.sabr_and_iron.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun inter(
    weight: FontWeight,
    size: Float,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/** Full Material 3 type scale on Inter so every role stays consistent. */
val Typography = Typography(
    displayLarge = inter(FontWeight.Bold, 57f, 64f, (-0.25f)),
    displayMedium = inter(FontWeight.Bold, 45f, 52f),
    displaySmall = inter(FontWeight.Bold, 36f, 44f),
    headlineLarge = inter(FontWeight.SemiBold, 32f, 40f),
    headlineMedium = inter(FontWeight.SemiBold, 28f, 36f),
    headlineSmall = inter(FontWeight.SemiBold, 24f, 32f),
    titleLarge = inter(FontWeight.SemiBold, 22f, 28f),
    titleMedium = inter(FontWeight.SemiBold, 16f, 24f, 0.15f),
    titleSmall = inter(FontWeight.Medium, 14f, 20f, 0.1f),
    bodyLarge = inter(FontWeight.Normal, 16f, 24f, 0.5f),
    bodyMedium = inter(FontWeight.Normal, 14f, 20f, 0.25f),
    bodySmall = inter(FontWeight.Normal, 12f, 16f, 0.4f),
    labelLarge = inter(FontWeight.Medium, 14f, 20f, 0.1f),
    labelMedium = inter(FontWeight.Medium, 12f, 16f, 0.5f),
    labelSmall = inter(FontWeight.Medium, 11f, 16f, 0.5f),
)

package com.example.sabr_and_iron.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SabrIronDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueDim,
    onPrimaryContainer = AccentBlueBright,
    secondary = ChartSecondary,
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = TextMuted,
    onTertiary = CharcoalBg,
    background = CharcoalBg,
    onBackground = TextPrimary,
    surface = CharcoalBg,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalSurface,
    onSurfaceVariant = TextMuted,
    surfaceContainerHighest = CharcoalSurface,
    surfaceContainerHigh = CharcoalSurface,
    surfaceContainer = CharcoalBg,
    surfaceContainerLow = CharcoalBg,
    surfaceContainerLowest = CharcoalBg,
    outline = CharcoalBorder,
    outlineVariant = CharcoalBorder.copy(alpha = 0.7f),
    error = ErrorSoft,
    onError = Color(0xFF450A0A),
    inverseSurface = TextPrimary,
    inverseOnSurface = CharcoalBg,
    inversePrimary = AccentBlueDim,
)

@Composable
fun SabrIronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SabrIronDarkColorScheme,
        typography = Typography,
        content = content,
    )
}

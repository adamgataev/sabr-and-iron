package com.example.sabr_and_iron.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SabrIronDarkColorScheme = darkColorScheme(
    primary = IronAmber,
    onPrimary = Color(0xFF3F2E00),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = SteelBlue,
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF1A4D66),
    onSecondaryContainer = Color(0xFFC8E6FF),
    tertiary = Color(0xFFC5C0C7),
    onTertiary = Color(0xFF2E2B2F),
    background = IronSurface,
    onBackground = IronOnSurface,
    surface = IronSurface,
    onSurface = IronOnSurface,
    surfaceVariant = IronSurfaceVariant,
    onSurfaceVariant = IronOnSurfaceVariant,
    outline = IronOutline,
    error = IronError,
    onError = Color(0xFF690005),
)

@Composable
fun SabrIronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SabrIronDarkColorScheme,
        typography = Typography,
        content = content
    )
}

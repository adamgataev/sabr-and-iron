package com.example.sabr_and_iron.ui.theme

import androidx.compose.ui.graphics.Color

/** Deep charcoal canvas. */
val CharcoalBg = Color(0xFF0D0F14)

/** Slightly lighter elevated surfaces / cards. */
val CharcoalSurface = Color(0xFF171A22)

/** Subtle borders and dividers. */
val CharcoalBorder = Color(0xFF282D38)

/** Vibrant blue — primary accent. */
val AccentBlue = Color(0xFF3B82F6)

val AccentBlueBright = Color(0xFF60A5FA)
val AccentBlueDim = Color(0xFF1E3A8A)

/** Crisp white for primary content. */
val TextPrimary = Color(0xFFF8FAFC)

/** Muted grey for secondary information. */
val TextMuted = Color(0xFF94A3B8)

val TextFaint = Color(0xFF64748B)

val ErrorSoft = Color(0xFFFCA5A5)

/** High-contrast field error border / label. */
val ErrorVibrant = Color(0xFFFF1744)

/** Chart secondary series (body weight) — restrained cool grey-blue. */
val ChartSecondary = Color(0xFF7DD3FC)

/** @deprecated Prefer AccentBlue / Charcoal* tokens. Kept for any lingering refs. */
val IronAmber = AccentBlue
val SteelBlue = ChartSecondary
val IronSurface = CharcoalBg
val IronSurfaceVariant = CharcoalSurface
val IronOnSurface = TextPrimary
val IronOnSurfaceVariant = TextMuted
val IronOutline = CharcoalBorder
val IronError = ErrorSoft

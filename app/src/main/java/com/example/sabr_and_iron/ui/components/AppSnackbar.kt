package com.example.sabr_and_iron.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppSnackbar = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalAppSnackbar not provided")
}

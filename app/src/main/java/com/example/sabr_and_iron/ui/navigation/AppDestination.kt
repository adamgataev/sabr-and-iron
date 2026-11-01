package com.example.sabr_and_iron.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val title: String,
    val label: String,
    val icon: ImageVector,
) {
    Records(
        route = "records",
        title = "Records",
        label = "Records",
        icon = Icons.Filled.EmojiEvents,
    ),
    Exercises(
        route = "exercises",
        title = "Exercises",
        label = "Exercises",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
    ),
    Routines(
        route = "routines",
        title = "Routines",
        label = "Routines",
        icon = Icons.AutoMirrored.Filled.ListAlt,
    ),
    Workouts(
        route = "workouts",
        title = "Workouts",
        label = "Workouts",
        icon = Icons.Filled.FitnessCenter,
    ),
}

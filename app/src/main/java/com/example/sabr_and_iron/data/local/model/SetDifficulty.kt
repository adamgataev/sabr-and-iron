package com.example.sabr_and_iron.data.local.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sabr_and_iron.ui.theme.AccentBlue
import com.example.sabr_and_iron.ui.theme.DifficultyHard
import com.example.sabr_and_iron.ui.theme.DifficultyIdeal
import com.example.sabr_and_iron.ui.theme.DifficultyPain

enum class SetDifficulty(
    val label: String,
    val icon: ImageVector,
    val color: Color,
) {
    LightWork("Light Work!", Icons.Outlined.Air, AccentBlue),
    Ideal("Ideal", Icons.Outlined.Balance, DifficultyIdeal),
    Hard("Hard", Icons.Filled.LocalFireDepartment, DifficultyHard),
    InducingPain("Inducing Pain", Icons.Outlined.MedicalServices, DifficultyPain),
}

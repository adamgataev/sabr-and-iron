package com.example.sabr_and_iron.data.local.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.ui.graphics.vector.ImageVector

enum class SetDifficulty(
    val label: String,
    val icon: ImageVector,
) {
    LightWork("Light Work!", Icons.Outlined.Air),
    Ideal("Ideal", Icons.Outlined.Balance),
    Hard("Hard", Icons.Filled.LocalFireDepartment),
    InducingPain("Inducing Pain", Icons.Outlined.MedicalServices),
}

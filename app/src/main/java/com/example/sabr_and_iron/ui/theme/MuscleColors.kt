package com.example.sabr_and_iron.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.sabr_and_iron.data.local.model.MuscleGroup

/** Vibrant per-muscle accents (not app primary blue / grey / black). */
val MuscleGroup.color: Color
    get() = when (this) {
        MuscleGroup.Chest -> Color(0xFFFB7185)
        MuscleGroup.Back -> Color(0xFF34D399)
        MuscleGroup.Shoulders -> Color(0xFFFBBF24)
        MuscleGroup.Arms -> Color(0xFFC084FC)
        MuscleGroup.Legs -> Color(0xFF2DD4BF)
        MuscleGroup.Core -> Color(0xFFA3E635)
        MuscleGroup.Cardio -> Color(0xFFF472B6)
    }

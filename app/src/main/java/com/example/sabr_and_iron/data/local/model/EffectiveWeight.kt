package com.example.sabr_and_iron.data.local.model

fun effectiveWeightKg(startingResistanceKg: Double?, addedWeightKg: Double): Double =
    (startingResistanceKg ?: 0.0) + addedWeightKg

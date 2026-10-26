package com.example.sabr_and_iron.data.local.model

data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val effectiveWeightKg: Double,
    val reps: Int,
    val performedAt: Long,
)

package com.example.sabr_and_iron.data.local.model

data class WorkoutSummary(
    val id: Long,
    val name: String,
    val performedAt: Long,
    val bodyWeightKg: Double?,
    val exerciseCount: Int,
)

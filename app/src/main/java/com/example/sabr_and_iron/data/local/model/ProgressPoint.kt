package com.example.sabr_and_iron.data.local.model

data class ProgressPoint(
    val exerciseId: Long,
    val performedAt: Long,
    val liftWeightKg: Double,
    val bodyWeightKg: Double?,
)

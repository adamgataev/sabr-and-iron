package com.example.sabr_and_iron.data.local.model

/** Flat join row used to compute PRs and progress in the repository. */
data class SetMetricRow(
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseType: ExerciseType,
    val machine: String? = null,
    val workoutId: Long,
    val performedAt: Long,
    val bodyWeightKg: Double?,
    val startingResistanceKg: Double?,
    val addedWeightKg: Double,
    val reps: Int,
    val durationSeconds: Int?,
    val distanceMeters: Double?,
)

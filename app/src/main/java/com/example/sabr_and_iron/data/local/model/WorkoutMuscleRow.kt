package com.example.sabr_and_iron.data.local.model

/**
 * One exercise's targeted muscles for a workout occurrence.
 * Used to derive unique muscle dots per calendar day.
 */
data class WorkoutMuscleRow(
    val workoutId: Long,
    val performedAt: Long,
    val targetedMuscles: List<MuscleGroup>,
)

package com.example.sabr_and_iron.data.local.model

data class ExerciseHistoryEntry(
    val performedAt: Long,
    val workoutName: String,
    val bodyWeightKg: Double?,
    val addedWeightKg: Double,
    val startingResistanceKg: Double?,
    val reps: Int,
    val durationSeconds: Int?,
    val distanceMeters: Double?,
    val difficulty: SetDifficulty,
    val exerciseType: ExerciseType,
) {
    fun effectiveLoadOrNull(): Double? =
        effectiveLoadKg(exerciseType, startingResistanceKg, addedWeightKg, bodyWeightKg)
}

package com.example.sabr_and_iron.data.local.model

data class ExerciseHistoryEntry(
    val performedAt: Long,
    val workoutName: String,
    val addedWeightKg: Double,
    val startingResistanceKg: Double?,
    val reps: Int,
    val difficulty: SetDifficulty,
) {
    val totalWeightKg: Double
        get() = effectiveWeightKg(startingResistanceKg, addedWeightKg)
}

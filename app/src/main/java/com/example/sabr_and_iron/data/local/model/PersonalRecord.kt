package com.example.sabr_and_iron.data.local.model

data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseType: ExerciseType,
    val machine: String? = null,
    val performedAt: Long,
    /** Best load (kg) for Regular / Weighted / Assisted. */
    val effectiveWeightKg: Double? = null,
    val reps: Int? = null,
    /** Best hold / effort duration. */
    val durationSeconds: Int? = null,
    /** Best distance in meters. */
    val distanceMeters: Double? = null,
)

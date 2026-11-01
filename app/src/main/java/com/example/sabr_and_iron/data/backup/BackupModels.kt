package com.example.sabr_and_iron.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class AppBackup(
    val version: Int = 1,
    val exportedAt: Long? = null,
    val exercises: List<ExerciseBackup> = emptyList(),
    val routines: List<RoutineBackup> = emptyList(),
    val routineExercises: List<RoutineExerciseBackup> = emptyList(),
    val workouts: List<WorkoutBackup> = emptyList(),
    val workoutExercises: List<WorkoutExerciseBackup> = emptyList(),
    val sets: List<SetBackup> = emptyList(),
)

@Serializable
data class ExerciseBackup(
    val id: Long,
    val name: String,
    val targetedMuscles: List<String>,
    val exerciseType: String,
    val startingResistanceKg: Double? = null,
    val defaultRestSeconds: Int? = null,
)

@Serializable
data class RoutineBackup(
    val id: Long,
    val name: String,
    val inspiredBy: String? = null,
)

@Serializable
data class RoutineExerciseBackup(
    val id: Long,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
)

@Serializable
data class WorkoutBackup(
    val id: Long,
    val name: String,
    val performedAt: Long,
    val bodyWeightKg: Double? = null,
)

@Serializable
data class WorkoutExerciseBackup(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val startingResistanceKg: Double? = null,
)

@Serializable
data class SetBackup(
    val id: Long,
    val workoutExerciseId: Long,
    val addedWeightKg: Double,
    val reps: Int,
    val difficulty: String,
    val position: Int,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
)

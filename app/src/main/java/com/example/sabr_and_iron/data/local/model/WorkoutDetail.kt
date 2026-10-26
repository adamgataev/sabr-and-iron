package com.example.sabr_and_iron.data.local.model

import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity

data class WorkoutExerciseWithSets(
    val workoutExercise: WorkoutExerciseEntity,
    val exerciseName: String,
    val defaultRestSeconds: Int?,
    val sets: List<SetEntity>,
)

data class WorkoutDetail(
    val workout: WorkoutEntity,
    val exercises: List<WorkoutExerciseWithSets>,
)

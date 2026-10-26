package com.example.sabr_and_iron.data.local.model

import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.entity.RoutineEntity

data class RoutineExerciseItem(
    val position: Int,
    val exercise: ExerciseEntity,
)

data class RoutineDetail(
    val routine: RoutineEntity,
    val exercises: List<RoutineExerciseItem>,
)

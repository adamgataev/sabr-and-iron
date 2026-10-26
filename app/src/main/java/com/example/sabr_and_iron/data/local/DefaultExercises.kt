package com.example.sabr_and_iron.data.local

import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup

object DefaultExercises {
    private const val BARBELL_KG = 20.0
    private const val COMPOUND_REST_SECONDS = 180
    private const val ACCESSORY_REST_SECONDS = 90

    val seed: List<ExerciseEntity> = listOf(
        ExerciseEntity(
            name = "Barbell Back Squat",
            targetedMuscles = listOf(MuscleGroup.Legs),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = BARBELL_KG,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Conventional Deadlift",
            targetedMuscles = listOf(MuscleGroup.Back, MuscleGroup.Legs),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = BARBELL_KG,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Barbell Bench Press",
            targetedMuscles = listOf(MuscleGroup.Chest),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = BARBELL_KG,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Overhead Press",
            targetedMuscles = listOf(MuscleGroup.Shoulders),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = BARBELL_KG,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Barbell Row",
            targetedMuscles = listOf(MuscleGroup.Back),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = BARBELL_KG,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Pull-up",
            targetedMuscles = listOf(MuscleGroup.Back, MuscleGroup.Arms),
            exerciseType = ExerciseType.WeightedBodyweight,
            startingResistanceKg = null,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Lat Pulldown",
            targetedMuscles = listOf(MuscleGroup.Back),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Incline Dumbbell Press",
            targetedMuscles = listOf(MuscleGroup.Chest, MuscleGroup.Shoulders),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Dumbbell Lateral Raise",
            targetedMuscles = listOf(MuscleGroup.Shoulders),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Romanian Deadlift",
            targetedMuscles = listOf(MuscleGroup.Legs, MuscleGroup.Back),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = BARBELL_KG,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Bicep Curl",
            targetedMuscles = listOf(MuscleGroup.Arms),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Triceps Pushdown",
            targetedMuscles = listOf(MuscleGroup.Arms),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Seated Cable Row",
            targetedMuscles = listOf(MuscleGroup.Back),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Leg Press",
            targetedMuscles = listOf(MuscleGroup.Legs),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = COMPOUND_REST_SECONDS,
        ),
        ExerciseEntity(
            name = "Plank",
            targetedMuscles = listOf(MuscleGroup.Core),
            exerciseType = ExerciseType.Regular,
            startingResistanceKg = null,
            defaultRestSeconds = ACCESSORY_REST_SECONDS,
        ),
    )
}

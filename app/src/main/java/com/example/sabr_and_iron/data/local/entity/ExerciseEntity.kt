package com.example.sabr_and_iron.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetedMuscles: List<MuscleGroup>,
    val exerciseType: ExerciseType,
    val startingResistanceKg: Double? = null,
    val defaultRestSeconds: Int? = null,
)

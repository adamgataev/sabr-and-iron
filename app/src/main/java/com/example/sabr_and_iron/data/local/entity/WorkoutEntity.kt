package com.example.sabr_and_iron.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val performedAt: Long,
    val bodyWeightKg: Double? = null,
)

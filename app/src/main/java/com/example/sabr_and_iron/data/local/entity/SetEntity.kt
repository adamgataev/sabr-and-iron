package com.example.sabr_and_iron.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sabr_and_iron.data.local.model.SetDifficulty

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    /** Plates / added load / assistance kg. 0 when not used (time / distance sets). */
    val addedWeightKg: Double = 0.0,
    /** Reps for load types. 0 when not used. */
    val reps: Int = 0,
    val difficulty: SetDifficulty,
    val position: Int,
    /** Hold or cardio duration. */
    val durationSeconds: Int? = null,
    /** Distance in meters (e.g. run / walk). */
    val distanceMeters: Double? = null,
)

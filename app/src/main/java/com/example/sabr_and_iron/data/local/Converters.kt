package com.example.sabr_and_iron.data.local

import androidx.room.TypeConverter
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import com.example.sabr_and_iron.data.local.model.SetDifficulty

class Converters {
    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String =
        value.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> =
        if (value.isBlank()) {
            emptyList()
        } else {
            value.split(SEPARATOR).map { MuscleGroup.valueOf(it) }
        }

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)

    @TypeConverter
    fun fromSetDifficulty(value: SetDifficulty): String = value.name

    @TypeConverter
    fun toSetDifficulty(value: String): SetDifficulty = SetDifficulty.valueOf(value)

    companion object {
        private const val SEPARATOR = ","
    }
}

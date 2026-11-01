package com.example.sabr_and_iron.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity
import com.example.sabr_and_iron.data.local.model.ExerciseHistoryEntry
import com.example.sabr_and_iron.data.local.model.SetMetricRow
import com.example.sabr_and_iron.data.local.model.WorkoutMuscleRow
import com.example.sabr_and_iron.data.local.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY performedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun count(): Int

    @Query(
        """
        SELECT w.id AS id,
               w.name AS name,
               w.performedAt AS performedAt,
               w.bodyWeightKg AS bodyWeightKg,
               COUNT(we.id) AS exerciseCount
        FROM workouts w
        LEFT JOIN workout_exercises we ON we.workoutId = w.id
        GROUP BY w.id
        ORDER BY w.performedAt DESC
        """,
    )
    fun observeSummaries(): Flow<List<WorkoutSummary>>

    @Query(
        """
        SELECT w.id AS workoutId,
               w.performedAt AS performedAt,
               e.targetedMuscles AS targetedMuscles
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN exercises e ON e.id = we.exerciseId
        """,
    )
    fun observeWorkoutMuscles(): Flow<List<WorkoutMuscleRow>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutEntity?>

    @Query(
        """
        SELECT * FROM workout_exercises
        WHERE workoutId = :workoutId
        ORDER BY position ASC
        """,
    )
    fun observeWorkoutExercises(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query(
        """
        SELECT * FROM workout_exercises
        WHERE workoutId = :workoutId
        ORDER BY position ASC
        """,
    )
    suspend fun getWorkoutExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExercise(id: Long): WorkoutExerciseEntity?

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    fun observeWorkoutExercise(id: Long): Flow<WorkoutExerciseEntity?>

    @Query(
        """
        SELECT * FROM sets
        WHERE workoutExerciseId = :workoutExerciseId
        ORDER BY position ASC
        """,
    )
    fun observeSets(workoutExerciseId: Long): Flow<List<SetEntity>>

    @Query(
        """
        SELECT * FROM sets
        WHERE workoutExerciseId = :workoutExerciseId
        ORDER BY position ASC
        """,
    )
    suspend fun getSets(workoutExerciseId: Long): List<SetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(item: WorkoutExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<WorkoutExerciseEntity>): List<Long>

    @Update
    suspend fun updateExercise(item: WorkoutExerciseEntity)

    @Delete
    suspend fun deleteExercise(item: WorkoutExerciseEntity)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(item: SetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(items: List<SetEntity>)

    @Update
    suspend fun updateSet(item: SetEntity)

    @Delete
    suspend fun deleteSet(item: SetEntity)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllWorkoutExercises()

    @Query("DELETE FROM sets")
    suspend fun deleteAllSets()

    @Query(
        """
        SELECT e.id AS exerciseId,
               e.name AS exerciseName,
               e.exerciseType AS exerciseType,
               e.machine AS machine,
               w.id AS workoutId,
               w.performedAt AS performedAt,
               w.bodyWeightKg AS bodyWeightKg,
               we.startingResistanceKg AS startingResistanceKg,
               s.addedWeightKg AS addedWeightKg,
               s.reps AS reps,
               s.durationSeconds AS durationSeconds,
               s.distanceMeters AS distanceMeters
        FROM sets s
        INNER JOIN workout_exercises we ON we.id = s.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        INNER JOIN exercises e ON e.id = we.exerciseId
        ORDER BY e.name COLLATE NOCASE ASC, w.performedAt ASC
        """,
    )
    fun observeAllSetMetrics(): Flow<List<SetMetricRow>>

    @Query(
        """
        SELECT e.id AS exerciseId,
               e.name AS exerciseName,
               e.exerciseType AS exerciseType,
               e.machine AS machine,
               w.id AS workoutId,
               w.performedAt AS performedAt,
               w.bodyWeightKg AS bodyWeightKg,
               we.startingResistanceKg AS startingResistanceKg,
               s.addedWeightKg AS addedWeightKg,
               s.reps AS reps,
               s.durationSeconds AS durationSeconds,
               s.distanceMeters AS distanceMeters
        FROM sets s
        INNER JOIN workout_exercises we ON we.id = s.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE we.exerciseId = :exerciseId
        ORDER BY w.performedAt ASC, s.position ASC
        """,
    )
    fun observeSetMetricsForExercise(exerciseId: Long): Flow<List<SetMetricRow>>

    @Query(
        """
        SELECT w.performedAt AS performedAt,
               w.name AS workoutName,
               w.bodyWeightKg AS bodyWeightKg,
               s.addedWeightKg AS addedWeightKg,
               we.startingResistanceKg AS startingResistanceKg,
               s.reps AS reps,
               s.durationSeconds AS durationSeconds,
               s.distanceMeters AS distanceMeters,
               s.difficulty AS difficulty,
               e.exerciseType AS exerciseType
        FROM sets s
        INNER JOIN workout_exercises we ON we.id = s.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
        INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE we.exerciseId = :exerciseId
          AND we.id != :excludeWorkoutExerciseId
        ORDER BY w.performedAt DESC, s.position ASC
        """,
    )
    fun observeExerciseHistory(
        exerciseId: Long,
        excludeWorkoutExerciseId: Long,
    ): Flow<List<ExerciseHistoryEntry>>

    @Transaction
    suspend fun replaceWorkoutExercises(
        workoutId: Long,
        items: List<WorkoutExerciseEntity>,
    ) {
        deleteExercisesForWorkout(workoutId)
        if (items.isNotEmpty()) {
            insertExercises(items)
        }
    }
}

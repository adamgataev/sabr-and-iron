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
import com.example.sabr_and_iron.data.local.model.PersonalRecord
import com.example.sabr_and_iron.data.local.model.ProgressPoint
import com.example.sabr_and_iron.data.local.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY performedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

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
               MAX(COALESCE(we.startingResistanceKg, 0) + s.addedWeightKg) AS effectiveWeightKg,
               (
                   SELECT s2.reps FROM sets s2
                   INNER JOIN workout_exercises we2 ON we2.id = s2.workoutExerciseId
                   INNER JOIN workouts w2 ON w2.id = we2.workoutId
                   WHERE we2.exerciseId = e.id
                   ORDER BY (COALESCE(we2.startingResistanceKg, 0) + s2.addedWeightKg) DESC,
                            s2.reps DESC,
                            w2.performedAt DESC
                   LIMIT 1
               ) AS reps,
               (
                   SELECT w3.performedAt FROM sets s3
                   INNER JOIN workout_exercises we3 ON we3.id = s3.workoutExerciseId
                   INNER JOIN workouts w3 ON w3.id = we3.workoutId
                   WHERE we3.exerciseId = e.id
                   ORDER BY (COALESCE(we3.startingResistanceKg, 0) + s3.addedWeightKg) DESC,
                            s3.reps DESC,
                            w3.performedAt DESC
                   LIMIT 1
               ) AS performedAt
        FROM exercises e
        INNER JOIN workout_exercises we ON we.exerciseId = e.id
        INNER JOIN sets s ON s.workoutExerciseId = we.id
        GROUP BY e.id
        ORDER BY e.name COLLATE NOCASE ASC
        """,
    )
    fun observePersonalRecords(): Flow<List<PersonalRecord>>

    @Query(
        """
        SELECT we.exerciseId AS exerciseId,
               w.performedAt AS performedAt,
               MAX(COALESCE(we.startingResistanceKg, 0) + s.addedWeightKg) AS liftWeightKg,
               w.bodyWeightKg AS bodyWeightKg
        FROM workouts w
        INNER JOIN workout_exercises we ON we.workoutId = w.id
        INNER JOIN sets s ON s.workoutExerciseId = we.id
        WHERE we.exerciseId = :exerciseId
        GROUP BY w.id
        ORDER BY w.performedAt ASC
        """,
    )
    fun observeProgress(exerciseId: Long): Flow<List<ProgressPoint>>

    @Query(
        """
        SELECT w.performedAt AS performedAt,
               w.name AS workoutName,
               s.addedWeightKg AS addedWeightKg,
               we.startingResistanceKg AS startingResistanceKg,
               s.reps AS reps,
               s.difficulty AS difficulty
        FROM sets s
        INNER JOIN workout_exercises we ON we.id = s.workoutExerciseId
        INNER JOIN workouts w ON w.id = we.workoutId
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

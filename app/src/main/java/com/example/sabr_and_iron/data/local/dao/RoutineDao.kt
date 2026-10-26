package com.example.sabr_and_iron.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.sabr_and_iron.data.local.entity.RoutineEntity
import com.example.sabr_and_iron.data.local.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeById(id: Long): Flow<RoutineEntity?>

    @Query(
        """
        SELECT * FROM routine_exercises
        WHERE routineId = :routineId
        ORDER BY position ASC
        """,
    )
    suspend fun getExercises(routineId: Long): List<RoutineExerciseEntity>

    @Query(
        """
        SELECT * FROM routine_exercises
        WHERE routineId = :routineId
        ORDER BY position ASC
        """,
    )
    fun observeExercises(routineId: Long): Flow<List<RoutineExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercisesForRoutine(routineId: Long)

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()

    @Query("DELETE FROM routine_exercises")
    suspend fun deleteAllRoutineExercises()

    @Transaction
    suspend fun replaceExercises(routineId: Long, items: List<RoutineExerciseEntity>) {
        deleteExercisesForRoutine(routineId)
        if (items.isNotEmpty()) {
            insertExercises(items)
        }
    }
}

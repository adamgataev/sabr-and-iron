package com.example.sabr_and_iron.data.repository

import com.example.sabr_and_iron.data.local.dao.ExerciseDao
import com.example.sabr_and_iron.data.local.dao.RoutineDao
import com.example.sabr_and_iron.data.local.entity.RoutineEntity
import com.example.sabr_and_iron.data.local.entity.RoutineExerciseEntity
import com.example.sabr_and_iron.data.local.model.RoutineDetail
import com.example.sabr_and_iron.data.local.model.RoutineExerciseItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao,
) {
    fun observeRoutines(): Flow<List<RoutineEntity>> = routineDao.observeAll()

    fun observeDetail(routineId: Long): Flow<RoutineDetail?> =
        combine(
            routineDao.observeById(routineId),
            routineDao.observeExercises(routineId),
            exerciseDao.observeAll(),
        ) { routine, links, exercises ->
            if (routine == null) return@combine null
            val byId = exercises.associateBy { it.id }
            RoutineDetail(
                routine = routine,
                exercises = links.mapNotNull { link ->
                    val exercise = byId[link.exerciseId] ?: return@mapNotNull null
                    RoutineExerciseItem(position = link.position, exercise = exercise)
                },
            )
        }

    suspend fun getExerciseIds(routineId: Long): List<Long> =
        routineDao.getExercises(routineId).sortedBy { it.position }.map { it.exerciseId }

    suspend fun save(
        routine: RoutineEntity,
        exerciseIdsInOrder: List<Long>,
    ): Long {
        val id = if (routine.id == 0L) {
            routineDao.insert(routine)
        } else {
            routineDao.update(routine)
            routine.id
        }
        val links = exerciseIdsInOrder.mapIndexed { index, exerciseId ->
            RoutineExerciseEntity(
                routineId = id,
                exerciseId = exerciseId,
                position = index,
            )
        }
        routineDao.replaceExercises(id, links)
        return id
    }

    suspend fun delete(routine: RoutineEntity) = routineDao.delete(routine)
}

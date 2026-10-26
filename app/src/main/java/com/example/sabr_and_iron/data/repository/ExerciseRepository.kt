package com.example.sabr_and_iron.data.repository

import com.example.sabr_and_iron.data.local.dao.ExerciseDao
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
) {
    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    fun search(query: String): Flow<List<ExerciseEntity>> =
        if (query.isBlank()) exerciseDao.observeAll() else exerciseDao.search(query.trim())

    fun observeById(id: Long): Flow<ExerciseEntity?> = exerciseDao.observeById(id)

    suspend fun getById(id: Long): ExerciseEntity? = exerciseDao.getById(id)

    suspend fun upsert(exercise: ExerciseEntity): Long =
        if (exercise.id == 0L) {
            exerciseDao.insert(exercise)
        } else {
            exerciseDao.update(exercise)
            exercise.id
        }

    suspend fun delete(exercise: ExerciseEntity) = exerciseDao.delete(exercise)
}

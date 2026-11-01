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

    /**
     * Case-insensitive uniqueness on name + machine.
     * Blank/`null` machine is treated as empty. [excludingId] is ignored (current exercise when editing).
     */
    suspend fun isNameMachineTaken(
        name: String,
        machine: String?,
        excludingId: Long = 0L,
    ): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return false
        val normalizedMachine = machine?.trim().orEmpty()
        val existing = exerciseDao.findByNameAndMachine(trimmedName, normalizedMachine)
            ?: return false
        return existing.id != excludingId
    }

    suspend fun upsert(exercise: ExerciseEntity): Long {
        if (isNameMachineTaken(exercise.name, exercise.machine, excludingId = exercise.id)) {
            error("An exercise with this name and machine already exists")
        }
        return if (exercise.id == 0L) {
            exerciseDao.insert(exercise)
        } else {
            exerciseDao.update(exercise)
            exercise.id
        }
    }

    suspend fun delete(exercise: ExerciseEntity) = exerciseDao.delete(exercise)
}

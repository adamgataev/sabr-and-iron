package com.example.sabr_and_iron.data.repository

import com.example.sabr_and_iron.data.local.dao.ExerciseDao
import com.example.sabr_and_iron.data.local.dao.WorkoutDao
import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity
import com.example.sabr_and_iron.data.local.model.ExerciseHistoryEntry
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import com.example.sabr_and_iron.data.local.model.WorkoutDetail
import com.example.sabr_and_iron.data.local.model.WorkoutExerciseWithSets
import com.example.sabr_and_iron.data.local.model.WorkoutMuscleRow
import com.example.sabr_and_iron.data.local.model.WorkoutSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
) {
    fun observeSummaries(): Flow<List<WorkoutSummary>> = workoutDao.observeSummaries()

    fun observeWorkoutMuscles(): Flow<List<WorkoutMuscleRow>> = workoutDao.observeWorkoutMuscles()

    /** Unique muscles per calendar day (day-start millis → ordered muscle list). */
    fun observeMusclesByDay(dayStartOf: (Long) -> Long): Flow<Map<Long, List<MuscleGroup>>> =
        observeWorkoutMuscles().map { rows ->
            rows
                .groupBy { dayStartOf(it.performedAt) }
                .mapValues { (_, dayRows) ->
                    dayRows
                        .flatMap { it.targetedMuscles }
                        .distinct()
                        .sortedBy { it.ordinal }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDetail(workoutId: Long): Flow<WorkoutDetail?> =
        combine(
            workoutDao.observeById(workoutId),
            workoutDao.observeWorkoutExercises(workoutId),
            exerciseDao.observeAll(),
        ) { workout, workoutExercises, exercises ->
            Triple(workout, workoutExercises, exercises)
        }.flatMapLatest { (workout, workoutExercises, exercises) ->
            if (workout == null) {
                flowOf(null)
            } else if (workoutExercises.isEmpty()) {
                flowOf(WorkoutDetail(workout = workout, exercises = emptyList()))
            } else {
                val byId = exercises.associateBy { it.id }
                val setFlows = workoutExercises.map { we ->
                    workoutDao.observeSets(we.id).map { sets -> we to sets }
                }
                combine(setFlows) { rows ->
                    WorkoutDetail(
                        workout = workout,
                        exercises = rows.map { (we, sets) ->
                            val exercise = byId[we.exerciseId]
                            WorkoutExerciseWithSets(
                                workoutExercise = we,
                                exerciseName = exercise?.name ?: "Unknown",
                                defaultRestSeconds = exercise?.defaultRestSeconds,
                                sets = sets.sortedBy { it.position },
                            )
                        },
                    )
                }
            }
        }

    fun observeWorkoutExercise(workoutExerciseId: Long): Flow<WorkoutExerciseEntity?> =
        workoutDao.observeWorkoutExercise(workoutExerciseId)

    fun observeWorkout(workoutId: Long): Flow<WorkoutEntity?> =
        workoutDao.observeById(workoutId)

    fun observeSets(workoutExerciseId: Long): Flow<List<SetEntity>> =
        workoutDao.observeSets(workoutExerciseId)

    fun observeHistory(
        exerciseId: Long,
        excludeWorkoutExerciseId: Long,
    ): Flow<List<ExerciseHistoryEntry>> =
        workoutDao.observeExerciseHistory(exerciseId, excludeWorkoutExerciseId)

    suspend fun createWorkout(
        name: String,
        performedAt: Long,
        bodyWeightKg: Double?,
        exerciseIdsInOrder: List<Long>,
    ): Long {
        val workoutId = workoutDao.insert(
            WorkoutEntity(
                name = name,
                performedAt = performedAt,
                bodyWeightKg = bodyWeightKg,
            ),
        )
        val items = exerciseIdsInOrder.mapIndexed { index, exerciseId ->
            val exercise = exerciseDao.getById(exerciseId)
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = index,
                startingResistanceKg = exercise?.startingResistanceKg,
            )
        }
        if (items.isNotEmpty()) {
            workoutDao.insertExercises(items)
        }
        return workoutId
    }

    suspend fun updateWorkoutMeta(workout: WorkoutEntity) = workoutDao.update(workout)

    suspend fun replaceExercises(workoutId: Long, exerciseIdsInOrder: List<Long>) {
        val items = exerciseIdsInOrder.mapIndexed { index, exerciseId ->
            val exercise = exerciseDao.getById(exerciseId)
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = index,
                startingResistanceKg = exercise?.startingResistanceKg,
            )
        }
        workoutDao.replaceWorkoutExercises(workoutId, items)
    }

    suspend fun addExerciseToWorkout(workoutId: Long, exerciseId: Long): AddExerciseResult {
        val existing = workoutDao.getWorkoutExercises(workoutId)
        if (existing.any { it.exerciseId == exerciseId }) {
            val name = exerciseDao.getById(exerciseId)?.name ?: "Exercise"
            return AddExerciseResult.AlreadyExists(name)
        }
        val exercise = exerciseDao.getById(exerciseId)
        val id = workoutDao.insertExercise(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = existing.size,
                startingResistanceKg = exercise?.startingResistanceKg,
            ),
        )
        return AddExerciseResult.Added(id)
    }

    /**
     * Appends [exerciseIds] that are not already in the workout.
     * Returns names of exercises that were skipped because they were already present.
     */
    suspend fun addExercisesSkippingDuplicates(
        workoutId: Long,
        exerciseIds: List<Long>,
    ): AddExercisesResult {
        val existing = workoutDao.getWorkoutExercises(workoutId)
        val presentIds = existing.map { it.exerciseId }.toMutableSet()
        var nextPosition = existing.size
        val skippedNames = mutableListOf<String>()
        var addedCount = 0
        for (exerciseId in exerciseIds) {
            val exercise = exerciseDao.getById(exerciseId)
            val name = exercise?.name ?: "Exercise"
            if (exerciseId in presentIds) {
                skippedNames += name
                continue
            }
            workoutDao.insertExercise(
                WorkoutExerciseEntity(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = nextPosition,
                    startingResistanceKg = exercise?.startingResistanceKg,
                ),
            )
            presentIds += exerciseId
            nextPosition++
            addedCount++
        }
        return AddExercisesResult(addedCount = addedCount, skippedNames = skippedNames)
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) = workoutDao.delete(workout)

    suspend fun deleteWorkoutExercise(item: WorkoutExerciseEntity) =
        workoutDao.deleteExercise(item)

    suspend fun reorderWorkoutExercises(ordered: List<WorkoutExerciseEntity>) {
        ordered.forEachIndexed { index, item ->
            if (item.position != index) {
                workoutDao.updateExercise(item.copy(position = index))
            }
        }
    }

    suspend fun addSet(
        workoutExerciseId: Long,
        addedWeightKg: Double = 0.0,
        reps: Int = 0,
        difficulty: SetDifficulty,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
    ): Long {
        val position = workoutDao.getSets(workoutExerciseId).size
        return workoutDao.insertSet(
            SetEntity(
                workoutExerciseId = workoutExerciseId,
                addedWeightKg = addedWeightKg,
                reps = reps,
                difficulty = difficulty,
                position = position,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
            ),
        )
    }

    suspend fun updateSet(set: SetEntity) = workoutDao.updateSet(set)

    suspend fun deleteSet(set: SetEntity) {
        val workoutExerciseId = set.workoutExerciseId
        workoutDao.deleteSet(set)
        workoutDao.getSets(workoutExerciseId)
            .sortedBy { it.position }
            .forEachIndexed { index, remaining ->
                if (remaining.position != index) {
                    workoutDao.updateSet(remaining.copy(position = index))
                }
            }
    }

    suspend fun getWorkoutExercise(id: Long) = workoutDao.getWorkoutExercise(id)
}

sealed class AddExerciseResult {
    data class Added(val workoutExerciseId: Long) : AddExerciseResult()
    data class AlreadyExists(val exerciseName: String) : AddExerciseResult()
}

data class AddExercisesResult(
    val addedCount: Int,
    val skippedNames: List<String>,
)

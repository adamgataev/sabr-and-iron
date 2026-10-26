package com.example.sabr_and_iron.ui.workouts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sabr_and_iron.SabrIronApplication
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SabrIronApplication
    private val workoutRepository = app.workoutRepository
    val exerciseRepository = app.exerciseRepository
    val routineRepository = app.routineRepository

    val summaries = workoutRepository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeDetail(workoutId: Long) = workoutRepository.observeDetail(workoutId)

    fun observeExercises() = exerciseRepository.observeExercises()

    fun observeRoutines() = routineRepository.observeRoutines()

    fun createWorkout(
        name: String,
        performedAt: Long,
        bodyWeightKg: Double?,
        exerciseIds: List<Long>,
        onDone: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val id = workoutRepository.createWorkout(name, performedAt, bodyWeightKg, exerciseIds)
            onDone(id)
        }
    }

    fun updateMeta(workout: WorkoutEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            workoutRepository.updateWorkoutMeta(workout)
            onDone()
        }
    }

    fun replaceExercises(workoutId: Long, exerciseIds: List<Long>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            workoutRepository.replaceExercises(workoutId, exerciseIds)
            onDone()
        }
    }

    fun addExercise(workoutId: Long, exerciseId: Long, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            onDone(workoutRepository.addExerciseToWorkout(workoutId, exerciseId))
        }
    }

    fun deleteWorkout(workout: WorkoutEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workout)
            onDone()
        }
    }

    suspend fun routineExerciseIds(routineId: Long): List<Long> =
        routineRepository.getExerciseIds(routineId)

    fun observeWorkoutExercise(id: Long) = workoutRepository.observeWorkoutExercise(id)

    fun observeSets(workoutExerciseId: Long) = workoutRepository.observeSets(workoutExerciseId)

    fun observeHistory(exerciseId: Long, excludeId: Long) =
        workoutRepository.observeHistory(exerciseId, excludeId)

    fun addSet(
        workoutExerciseId: Long,
        addedWeightKg: Double,
        reps: Int,
        difficulty: SetDifficulty,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            workoutRepository.addSet(workoutExerciseId, addedWeightKg, reps, difficulty)
            onDone()
        }
    }

    fun updateSet(
        set: com.example.sabr_and_iron.data.local.entity.SetEntity,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            workoutRepository.updateSet(set)
            onDone()
        }
    }

    fun deleteSet(
        set: com.example.sabr_and_iron.data.local.entity.SetEntity,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            workoutRepository.deleteSet(set)
            onDone()
        }
    }

    fun deleteWorkoutExercise(
        item: com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            workoutRepository.deleteWorkoutExercise(item)
            onDone()
        }
    }

    suspend fun getExercise(id: Long) = exerciseRepository.getById(id)
}

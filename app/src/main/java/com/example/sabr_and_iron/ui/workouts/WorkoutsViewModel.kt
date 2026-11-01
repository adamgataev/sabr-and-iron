package com.example.sabr_and_iron.ui.workouts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sabr_and_iron.SabrIronApplication
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import com.example.sabr_and_iron.data.repository.AddExerciseResult
import com.example.sabr_and_iron.data.repository.AddExercisesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WorkoutsViewMode {
    List,
    Calendar,
}

class WorkoutsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SabrIronApplication
    private val workoutRepository = app.workoutRepository
    val exerciseRepository = app.exerciseRepository
    val routineRepository = app.routineRepository

    val summaries = workoutRepository.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _viewMode = MutableStateFlow(WorkoutsViewMode.Calendar)
    val viewMode: StateFlow<WorkoutsViewMode> = _viewMode.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            WorkoutsViewMode.List -> WorkoutsViewMode.Calendar
            WorkoutsViewMode.Calendar -> WorkoutsViewMode.List
        }
    }

    fun clearMessage() {
        _message.value = null
    }

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

    fun addExercise(workoutId: Long, exerciseId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = workoutRepository.addExerciseToWorkout(workoutId, exerciseId)) {
                is AddExerciseResult.Added -> onDone()
                is AddExerciseResult.AlreadyExists -> {
                    _message.value =
                        "${result.exerciseName} is already in this workout"
                }
            }
        }
    }

    fun addRoutineExercises(workoutId: Long, routineId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val ids = routineRepository.getExerciseIds(routineId)
            val result = workoutRepository.addExercisesSkippingDuplicates(workoutId, ids)
            showAddExercisesMessage(result)
            onDone()
        }
    }

    fun showRoutineDuplicates(skippedNames: List<String>) {
        if (skippedNames.isEmpty()) return
        _message.value = alreadyPresentMessage(skippedNames)
    }

    private fun showAddExercisesMessage(result: AddExercisesResult) {
        if (result.skippedNames.isEmpty()) return
        _message.value = alreadyPresentMessage(result.skippedNames)
    }

    private fun alreadyPresentMessage(names: List<String>): String =
        if (names.size == 1) {
            "${names.first()} is already in this workout"
        } else {
            "Already in this workout: ${names.joinToString()}"
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

    fun observeWorkout(id: Long) = workoutRepository.observeWorkout(id)

    fun observeSets(workoutExerciseId: Long) = workoutRepository.observeSets(workoutExerciseId)

    fun observeHistory(exerciseId: Long, excludeId: Long) =
        workoutRepository.observeHistory(exerciseId, excludeId)

    fun addSet(
        workoutExerciseId: Long,
        addedWeightKg: Double = 0.0,
        reps: Int = 0,
        difficulty: SetDifficulty,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            workoutRepository.addSet(
                workoutExerciseId = workoutExerciseId,
                addedWeightKg = addedWeightKg,
                reps = reps,
                difficulty = difficulty,
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
            )
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

    fun reorderWorkoutExercises(
        ordered: List<com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity>,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            workoutRepository.reorderWorkoutExercises(ordered)
            onDone()
        }
    }

    suspend fun getExercise(id: Long) = exerciseRepository.getById(id)
}

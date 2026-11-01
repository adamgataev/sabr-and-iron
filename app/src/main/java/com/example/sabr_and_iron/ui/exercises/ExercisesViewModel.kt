package com.example.sabr_and_iron.ui.exercises

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sabr_and_iron.SabrIronApplication
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExercisesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as SabrIronApplication).exerciseRepository

    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<ExerciseEntity>> = query
        .flatMapLatest { repository.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        query.value = value
    }

    suspend fun isNameTaken(name: String, excludingId: Long): Boolean =
        repository.isNameTaken(name, excludingId)

    fun save(
        id: Long,
        name: String,
        muscles: List<MuscleGroup>,
        type: ExerciseType,
        startingResistanceKg: Double?,
        defaultRestSeconds: Int?,
        onDone: (Long) -> Unit,
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                repository.upsert(
                    ExerciseEntity(
                        id = id,
                        name = name.trim(),
                        targetedMuscles = muscles,
                        exerciseType = type,
                        startingResistanceKg = startingResistanceKg,
                        defaultRestSeconds = defaultRestSeconds,
                    ),
                )
            }
                .onSuccess { onDone(it) }
                .onFailure { onError(it.message ?: "Could not save exercise") }
        }
    }

    fun delete(exercise: ExerciseEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(exercise)
            onDone()
        }
    }

    fun observeExercise(id: Long) = repository.observeById(id)
}

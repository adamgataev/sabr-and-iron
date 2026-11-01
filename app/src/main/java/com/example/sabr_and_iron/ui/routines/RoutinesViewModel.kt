package com.example.sabr_and_iron.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sabr_and_iron.SabrIronApplication
import com.example.sabr_and_iron.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SabrIronApplication
    private val routineRepository = app.routineRepository
    val exerciseRepository = app.exerciseRepository

    val routines = routineRepository.observeRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeDetail(id: Long) = routineRepository.observeDetail(id)

    fun observeExercises() = exerciseRepository.observeExercises()

    suspend fun isNameTaken(name: String, excludingId: Long = 0L): Boolean =
        routineRepository.isNameTaken(name, excludingId)

    fun save(
        id: Long,
        name: String,
        inspiredBy: String?,
        exerciseIds: List<Long>,
        onDone: (Long) -> Unit,
        onError: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                routineRepository.save(
                    RoutineEntity(
                        id = id,
                        name = name.trim(),
                        inspiredBy = inspiredBy?.trim()?.ifBlank { null },
                    ),
                    exerciseIds,
                )
            }.onSuccess { saved ->
                onDone(saved)
            }.onFailure {
                onError()
            }
        }
    }

    fun delete(routine: RoutineEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            routineRepository.delete(routine)
            onDone()
        }
    }
}

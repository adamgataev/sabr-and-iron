package com.example.sabr_and_iron.ui.records

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sabr_and_iron.SabrIronApplication
import com.example.sabr_and_iron.data.local.model.PersonalRecord
import com.example.sabr_and_iron.data.local.model.ProgressPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class RecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SabrIronApplication
    private val recordsRepository = app.recordsRepository
    private val backupRepository = app.backupRepository

    val records: StateFlow<List<PersonalRecord>> = recordsRepository.observePersonalRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedExerciseId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val progress: StateFlow<List<ProgressPoint>> = selectedExerciseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else recordsRepository.observeProgress(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedId: StateFlow<Long?> = selectedExerciseId

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun selectExercise(id: Long?) {
        selectedExerciseId.value = id
    }

    fun clearMessage() {
        _message.value = null
    }

    fun exportJson(onFile: (File) -> Unit) {
        viewModelScope.launch {
            runCatching { backupRepository.exportJson() }
                .onSuccess {
                    onFile(it)
                    _message.value = "JSON backup ready"
                }
                .onFailure { _message.value = it.message ?: "Export failed" }
        }
    }

    fun exportExcel(onFile: (File) -> Unit) {
        viewModelScope.launch {
            runCatching { backupRepository.exportExcel() }
                .onSuccess {
                    onFile(it)
                    _message.value = "Excel export ready"
                }
                .onFailure { _message.value = it.message ?: "Excel export failed" }
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read file")
                backupRepository.importJson(text)
            }
                .onSuccess { _message.value = "Backup restored" }
                .onFailure { _message.value = it.message ?: "Import failed" }
        }
    }
}

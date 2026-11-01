package com.example.sabr_and_iron.ui.records

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sabr_and_iron.SabrIronApplication
import com.example.sabr_and_iron.data.local.DummyDataSeeder
import com.example.sabr_and_iron.data.local.model.PersonalRecord
import com.example.sabr_and_iron.data.local.model.ProgressPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class RecordsSort {
    Alphabetical,
    LastUpdated,
}

class RecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SabrIronApplication
    private val recordsRepository = app.recordsRepository
    private val backupRepository = app.backupRepository
    private val dummyDataSeeder = app.dummyDataSeeder

    private val _sort = MutableStateFlow(RecordsSort.Alphabetical)
    val sort: StateFlow<RecordsSort> = _sort.asStateFlow()

    val records: StateFlow<List<PersonalRecord>> = combine(
        recordsRepository.observePersonalRecords(),
        _sort,
    ) { list, sortMode ->
        when (sortMode) {
            RecordsSort.Alphabetical -> list.sortedBy { it.exerciseName.lowercase() }
            RecordsSort.LastUpdated -> list.sortedByDescending { it.performedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setSort(sort: RecordsSort) {
        _sort.value = sort
    }

    fun observeProgress(exerciseId: Long): Flow<List<ProgressPoint>> =
        recordsRepository.observeProgress(exerciseId)

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

    fun loadSampleData() {
        viewModelScope.launch {
            runCatching { dummyDataSeeder.seedIfEmpty() }
                .onSuccess { result ->
                    _message.value = when (result) {
                        DummyDataSeeder.SeedResult.Inserted ->
                            "Sample data loaded (routines + workouts)"
                        DummyDataSeeder.SeedResult.AlreadyPopulated ->
                            "Workouts already exist — sample data skipped"
                    }
                }
                .onFailure { _message.value = it.message ?: "Sample data failed" }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            runCatching { dummyDataSeeder.clearAll() }
                .onSuccess {
                    _message.value = "All data cleared — exercise library restored"
                }
                .onFailure { _message.value = it.message ?: "Clear failed" }
        }
    }
}

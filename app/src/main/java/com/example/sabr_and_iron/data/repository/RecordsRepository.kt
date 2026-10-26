package com.example.sabr_and_iron.data.repository

import com.example.sabr_and_iron.data.local.dao.WorkoutDao
import com.example.sabr_and_iron.data.local.model.PersonalRecord
import com.example.sabr_and_iron.data.local.model.ProgressPoint
import kotlinx.coroutines.flow.Flow

class RecordsRepository(
    private val workoutDao: WorkoutDao,
) {
    fun observePersonalRecords(): Flow<List<PersonalRecord>> =
        workoutDao.observePersonalRecords()

    fun observeProgress(exerciseId: Long): Flow<List<ProgressPoint>> =
        workoutDao.observeProgress(exerciseId)
}

package com.example.sabr_and_iron

import android.app.Application
import com.example.sabr_and_iron.data.local.SabrIronDatabase
import com.example.sabr_and_iron.data.repository.BackupRepository
import com.example.sabr_and_iron.data.repository.ExerciseRepository
import com.example.sabr_and_iron.data.repository.RecordsRepository
import com.example.sabr_and_iron.data.repository.RoutineRepository
import com.example.sabr_and_iron.data.repository.WorkoutRepository
import com.example.sabr_and_iron.timer.RestTimerController
import com.example.sabr_and_iron.timer.RestTimerNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SabrIronApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val restTimer: RestTimerController by lazy {
        RestTimerController(applicationContext, applicationScope)
    }

    val database: SabrIronDatabase by lazy { SabrIronDatabase.getInstance(this) }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao())
    }

    val routineRepository: RoutineRepository by lazy {
        RoutineRepository(database.routineDao(), database.exerciseDao())
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.exerciseDao())
    }

    val recordsRepository: RecordsRepository by lazy {
        RecordsRepository(database.workoutDao())
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(this, database)
    }

    override fun onCreate() {
        super.onCreate()
        RestTimerNotifications.ensureChannel(this)
    }
}

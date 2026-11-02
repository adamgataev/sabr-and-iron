package com.example.sabr_and_iron.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.sabr_and_iron.data.backup.AppBackup
import com.example.sabr_and_iron.data.backup.CURRENT_BACKUP_VERSION
import com.example.sabr_and_iron.data.backup.ExcelBackupCodec
import com.example.sabr_and_iron.data.backup.ExerciseBackup
import com.example.sabr_and_iron.data.backup.RoutineBackup
import com.example.sabr_and_iron.data.backup.RoutineExerciseBackup
import com.example.sabr_and_iron.data.backup.SetBackup
import com.example.sabr_and_iron.data.backup.WorkoutBackup
import com.example.sabr_and_iron.data.backup.WorkoutExerciseBackup
import com.example.sabr_and_iron.data.local.SabrIronDatabase
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.entity.RoutineEntity
import com.example.sabr_and_iron.data.local.entity.RoutineExerciseEntity
import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val context: Context,
    private val database: SabrIronDatabase,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun exportJson(): File = withContext(Dispatchers.IO) {
        val backup = collectBackup()
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, "sabr_iron_backup_${timestamp()}.json")
        file.writeText(json.encodeToString(AppBackup.serializer(), backup))
        file
    }

    suspend fun importJson(text: String) = withContext(Dispatchers.IO) {
        val backup = json.decodeFromString(AppBackup.serializer(), text)
        restoreBackup(backup)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        database.withTransaction {
            val workoutDao = database.workoutDao()
            val routineDao = database.routineDao()
            val exerciseDao = database.exerciseDao()
            workoutDao.deleteAllSets()
            workoutDao.deleteAllWorkoutExercises()
            workoutDao.deleteAllWorkouts()
            routineDao.deleteAllRoutineExercises()
            routineDao.deleteAllRoutines()
            exerciseDao.deleteAll()
            syncSqliteSequence(
                "exercises" to null,
                "routines" to null,
                "routine_exercises" to null,
                "workouts" to null,
                "workout_exercises" to null,
                "sets" to null,
            )
        }
    }

    suspend fun exportExcel(): File = withContext(Dispatchers.IO) {
        val backup = collectBackup()
        val dir = File(context.filesDir, "exports").also { it.mkdirs() }
        val file = File(dir, "sabr_iron_export_${timestamp()}.xlsx")
        ExcelBackupCodec.write(backup, file)
        file
    }

    suspend fun importExcel(uri: Uri) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Could not read Excel file")
        stream.use { importExcel(it) }
    }

    suspend fun importExcel(inputStream: InputStream) = withContext(Dispatchers.IO) {
        restoreBackup(ExcelBackupCodec.read(inputStream))
    }

    private suspend fun restoreBackup(backup: AppBackup) {
        database.withTransaction {
            val workoutDao = database.workoutDao()
            val routineDao = database.routineDao()
            val exerciseDao = database.exerciseDao()

            workoutDao.deleteAllSets()
            workoutDao.deleteAllWorkoutExercises()
            workoutDao.deleteAllWorkouts()
            routineDao.deleteAllRoutineExercises()
            routineDao.deleteAllRoutines()
            exerciseDao.deleteAll()

            exerciseDao.insertAll(
                backup.exercises.map {
                    ExerciseEntity(
                        id = it.id,
                        name = it.name,
                        machine = it.machine?.trim()?.takeIf { value -> value.isNotEmpty() },
                        targetedMuscles = it.targetedMuscles.map { name ->
                            parseEnum(name, MuscleGroup::valueOf, "MuscleGroup")
                        },
                        exerciseType = parseEnum(
                            it.exerciseType,
                            ExerciseType::valueOf,
                            "ExerciseType",
                        ),
                        startingResistanceKg = it.startingResistanceKg,
                        defaultRestSeconds = it.defaultRestSeconds,
                    )
                },
            )
            backup.routines.forEach {
                routineDao.insert(
                    RoutineEntity(id = it.id, name = it.name, inspiredBy = it.inspiredBy),
                )
            }
            routineDao.insertExercises(
                backup.routineExercises.map {
                    RoutineExerciseEntity(
                        id = it.id,
                        routineId = it.routineId,
                        exerciseId = it.exerciseId,
                        position = it.position,
                    )
                },
            )
            backup.workouts.forEach {
                workoutDao.insert(
                    WorkoutEntity(
                        id = it.id,
                        name = it.name,
                        performedAt = it.performedAt,
                        bodyWeightKg = it.bodyWeightKg,
                    ),
                )
            }
            workoutDao.insertExercises(
                backup.workoutExercises.map {
                    WorkoutExerciseEntity(
                        id = it.id,
                        workoutId = it.workoutId,
                        exerciseId = it.exerciseId,
                        position = it.position,
                        startingResistanceKg = it.startingResistanceKg,
                    )
                },
            )
            workoutDao.insertSets(
                backup.sets.map {
                    SetEntity(
                        id = it.id,
                        workoutExerciseId = it.workoutExerciseId,
                        addedWeightKg = it.addedWeightKg,
                        reps = it.reps,
                        difficulty = parseEnum(
                            it.difficulty,
                            SetDifficulty::valueOf,
                            "SetDifficulty",
                        ),
                        position = it.position,
                        durationSeconds = it.durationSeconds,
                        distanceMeters = it.distanceMeters,
                    )
                },
            )

            syncSqliteSequence(
                "exercises" to backup.exercises.maxOfOrNull { it.id },
                "routines" to backup.routines.maxOfOrNull { it.id },
                "routine_exercises" to backup.routineExercises.maxOfOrNull { it.id },
                "workouts" to backup.workouts.maxOfOrNull { it.id },
                "workout_exercises" to backup.workoutExercises.maxOfOrNull { it.id },
                "sets" to backup.sets.maxOfOrNull { it.id },
            )
        }
    }

    private fun syncSqliteSequence(vararg tables: Pair<String, Long?>) {
        val db = database.openHelper.writableDatabase
        tables.forEach { (table, maxId) ->
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table))
            if (maxId != null && maxId > 0L) {
                db.execSQL(
                    "INSERT INTO sqlite_sequence(name, seq) VALUES(?, ?)",
                    arrayOf<Any>(table, maxId),
                )
            }
        }
    }

    private suspend fun collectBackup(): AppBackup {
        val exercises = database.exerciseDao().observeAll().first()
        val routines = database.routineDao().observeAll().first()
        val workouts = database.workoutDao().observeAll().first()
        val routineExercises = routines.flatMap { database.routineDao().getExercises(it.id) }
        val workoutExercises = workouts.flatMap { database.workoutDao().getWorkoutExercises(it.id) }
        val sets = workoutExercises.flatMap { database.workoutDao().getSets(it.id) }

        return AppBackup(
            version = CURRENT_BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            exercises = exercises.map {
                ExerciseBackup(
                    id = it.id,
                    name = it.name,
                    machine = it.machine,
                    targetedMuscles = it.targetedMuscles.map(MuscleGroup::name),
                    exerciseType = it.exerciseType.name,
                    startingResistanceKg = it.startingResistanceKg,
                    defaultRestSeconds = it.defaultRestSeconds,
                )
            },
            routines = routines.map {
                RoutineBackup(id = it.id, name = it.name, inspiredBy = it.inspiredBy)
            },
            routineExercises = routineExercises.map {
                RoutineExerciseBackup(
                    id = it.id,
                    routineId = it.routineId,
                    exerciseId = it.exerciseId,
                    position = it.position,
                )
            },
            workouts = workouts.map {
                WorkoutBackup(
                    id = it.id,
                    name = it.name,
                    performedAt = it.performedAt,
                    bodyWeightKg = it.bodyWeightKg,
                )
            },
            workoutExercises = workoutExercises.map {
                WorkoutExerciseBackup(
                    id = it.id,
                    workoutId = it.workoutId,
                    exerciseId = it.exerciseId,
                    position = it.position,
                    startingResistanceKg = it.startingResistanceKg,
                )
            },
            sets = sets.map {
                SetBackup(
                    id = it.id,
                    workoutExerciseId = it.workoutExerciseId,
                    addedWeightKg = it.addedWeightKg,
                    reps = it.reps,
                    difficulty = it.difficulty.name,
                    position = it.position,
                    durationSeconds = it.durationSeconds,
                    distanceMeters = it.distanceMeters,
                )
            },
        )
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private inline fun <reified T : Enum<T>> parseEnum(
        raw: String,
        crossinline valueOf: (String) -> T,
        typeName: String,
    ): T =
        try {
            valueOf(raw)
        } catch (_: IllegalArgumentException) {
            error("Unknown $typeName \"$raw\"")
        }

}

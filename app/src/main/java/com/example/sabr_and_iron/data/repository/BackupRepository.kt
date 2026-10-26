package com.example.sabr_and_iron.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.sabr_and_iron.data.backup.AppBackup
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
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
                        targetedMuscles = it.targetedMuscles.map(MuscleGroup::valueOf),
                        exerciseType = ExerciseType.valueOf(it.exerciseType),
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
                        difficulty = SetDifficulty.valueOf(it.difficulty),
                        position = it.position,
                    )
                },
            )
        }
    }

    suspend fun exportExcel(): File = withContext(Dispatchers.IO) {
        val backup = collectBackup()
        val exercisesById = backup.exercises.associateBy { it.id }
        val workbook = XSSFWorkbook()

        workbook.createSheet("Exercises").also { sheet ->
            val header = sheet.createRow(0)
            listOf("id", "name", "muscles", "type", "startingKg", "restSeconds").forEachIndexed { i, title ->
                header.createCell(i).setCellValue(title)
            }
            backup.exercises.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(item.id.toDouble())
                row.createCell(1).setCellValue(item.name)
                row.createCell(2).setCellValue(item.targetedMuscles.joinToString())
                row.createCell(3).setCellValue(item.exerciseType)
                item.startingResistanceKg?.let { row.createCell(4).setCellValue(it) }
                item.defaultRestSeconds?.let { row.createCell(5).setCellValue(it.toDouble()) }
            }
        }

        workbook.createSheet("Routines").also { sheet ->
            val header = sheet.createRow(0)
            listOf("id", "name", "inspiredBy", "exerciseNames").forEachIndexed { i, title ->
                header.createCell(i).setCellValue(title)
            }
            backup.routines.forEachIndexed { index, routine ->
                val names = backup.routineExercises
                    .filter { it.routineId == routine.id }
                    .sortedBy { it.position }
                    .mapNotNull { exercisesById[it.exerciseId]?.name }
                    .joinToString(" | ")
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(routine.id.toDouble())
                row.createCell(1).setCellValue(routine.name)
                row.createCell(2).setCellValue(routine.inspiredBy.orEmpty())
                row.createCell(3).setCellValue(names)
            }
        }

        workbook.createSheet("Workouts").also { sheet ->
            val header = sheet.createRow(0)
            listOf(
                "workoutId", "workoutName", "performedAt", "bodyWeightKg",
                "exercise", "set", "addedKg", "effectiveKg", "reps", "difficulty",
            ).forEachIndexed { i, title ->
                header.createCell(i).setCellValue(title)
            }
            var rowIndex = 1
            backup.workouts.sortedByDescending { it.performedAt }.forEach { workout ->
                val weList = backup.workoutExercises
                    .filter { it.workoutId == workout.id }
                    .sortedBy { it.position }
                if (weList.isEmpty()) {
                    val row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(workout.id.toDouble())
                    row.createCell(1).setCellValue(workout.name)
                    row.createCell(2).setCellValue(formatDate(workout.performedAt))
                    workout.bodyWeightKg?.let { row.createCell(3).setCellValue(it) }
                } else {
                    weList.forEach { we ->
                        val sets = backup.sets
                            .filter { it.workoutExerciseId == we.id }
                            .sortedBy { it.position }
                        val exerciseName = exercisesById[we.exerciseId]?.name.orEmpty()
                        if (sets.isEmpty()) {
                            val row = sheet.createRow(rowIndex++)
                            row.createCell(0).setCellValue(workout.id.toDouble())
                            row.createCell(1).setCellValue(workout.name)
                            row.createCell(2).setCellValue(formatDate(workout.performedAt))
                            workout.bodyWeightKg?.let { row.createCell(3).setCellValue(it) }
                            row.createCell(4).setCellValue(exerciseName)
                        } else {
                            sets.forEach { set ->
                                val effective =
                                    (we.startingResistanceKg ?: 0.0) + set.addedWeightKg
                                val row = sheet.createRow(rowIndex++)
                                row.createCell(0).setCellValue(workout.id.toDouble())
                                row.createCell(1).setCellValue(workout.name)
                                row.createCell(2).setCellValue(formatDate(workout.performedAt))
                                workout.bodyWeightKg?.let { row.createCell(3).setCellValue(it) }
                                row.createCell(4).setCellValue(exerciseName)
                                row.createCell(5).setCellValue((set.position + 1).toDouble())
                                row.createCell(6).setCellValue(set.addedWeightKg)
                                row.createCell(7).setCellValue(effective)
                                row.createCell(8).setCellValue(set.reps.toDouble())
                                row.createCell(9).setCellValue(set.difficulty)
                            }
                        }
                    }
                }
            }
        }

        val dir = File(context.filesDir, "exports").also { it.mkdirs() }
        val file = File(dir, "sabr_iron_export_${timestamp()}.xlsx")
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        file
    }

    private suspend fun collectBackup(): AppBackup {
        val exercises = database.exerciseDao().observeAll().first()
        val routines = database.routineDao().observeAll().first()
        val workouts = database.workoutDao().observeAll().first()
        val routineExercises = routines.flatMap { database.routineDao().getExercises(it.id) }
        val workoutExercises = workouts.flatMap { database.workoutDao().getWorkoutExercises(it.id) }
        val sets = workoutExercises.flatMap { database.workoutDao().getSets(it.id) }

        return AppBackup(
            exercises = exercises.map {
                ExerciseBackup(
                    id = it.id,
                    name = it.name,
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
                )
            },
        )
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(epochMillis))
}

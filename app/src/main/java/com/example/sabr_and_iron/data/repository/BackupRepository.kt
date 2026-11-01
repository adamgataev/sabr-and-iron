package com.example.sabr_and_iron.data.repository

import android.content.Context
import android.net.Uri
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
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
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

    private val dataFormatter = DataFormatter()

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
        }
    }

    suspend fun exportExcel(): File = withContext(Dispatchers.IO) {
        val backup = collectBackup()
        val exercisesById = backup.exercises.associateBy { it.id }
        val workbook = XSSFWorkbook()

        writeSheet(
            workbook = workbook,
            name = SHEET_EXERCISES,
            headers = listOf(
                "id", "name", "machine", "targetedMuscles", "exerciseType",
                "startingResistanceKg", "defaultRestSeconds",
            ),
        ) { sheet ->
            backup.exercises.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.setNumber(0, item.id.toDouble())
                row.setText(1, item.name)
                item.machine?.let { row.setText(2, it) }
                row.setText(3, item.targetedMuscles.joinToString(","))
                row.setText(4, item.exerciseType)
                item.startingResistanceKg?.let { row.setNumber(5, it) }
                item.defaultRestSeconds?.let { row.setNumber(6, it.toDouble()) }
            }
        }

        writeSheet(
            workbook = workbook,
            name = SHEET_ROUTINES,
            headers = listOf("id", "name", "inspiredBy"),
        ) { sheet ->
            backup.routines.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.setNumber(0, item.id.toDouble())
                row.setText(1, item.name)
                item.inspiredBy?.let { row.setText(2, it) }
            }
        }

        writeSheet(
            workbook = workbook,
            name = SHEET_ROUTINE_EXERCISES,
            headers = listOf("id", "routineId", "exerciseId", "position"),
        ) { sheet ->
            backup.routineExercises.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.setNumber(0, item.id.toDouble())
                row.setNumber(1, item.routineId.toDouble())
                row.setNumber(2, item.exerciseId.toDouble())
                row.setNumber(3, item.position.toDouble())
            }
        }

        writeSheet(
            workbook = workbook,
            name = SHEET_WORKOUTS,
            headers = listOf("id", "name", "performedAt", "bodyWeightKg"),
        ) { sheet ->
            backup.workouts.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.setNumber(0, item.id.toDouble())
                row.setText(1, item.name)
                row.setNumber(2, item.performedAt.toDouble())
                item.bodyWeightKg?.let { row.setNumber(3, it) }
            }
        }

        writeSheet(
            workbook = workbook,
            name = SHEET_WORKOUT_EXERCISES,
            headers = listOf(
                "id", "workoutId", "exerciseId", "position", "startingResistanceKg",
            ),
        ) { sheet ->
            backup.workoutExercises.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.setNumber(0, item.id.toDouble())
                row.setNumber(1, item.workoutId.toDouble())
                row.setNumber(2, item.exerciseId.toDouble())
                row.setNumber(3, item.position.toDouble())
                item.startingResistanceKg?.let { row.setNumber(4, it) }
            }
        }

        writeSheet(
            workbook = workbook,
            name = SHEET_SETS,
            headers = listOf(
                "id", "workoutExerciseId", "addedWeightKg", "reps", "difficulty",
                "position", "durationSeconds", "distanceMeters",
            ),
        ) { sheet ->
            backup.sets.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.setNumber(0, item.id.toDouble())
                row.setNumber(1, item.workoutExerciseId.toDouble())
                row.setNumber(2, item.addedWeightKg)
                row.setNumber(3, item.reps.toDouble())
                row.setText(4, item.difficulty)
                row.setNumber(5, item.position.toDouble())
                item.durationSeconds?.let { row.setNumber(6, it.toDouble()) }
                item.distanceMeters?.let { row.setNumber(7, it) }
            }
        }

        writeSheet(
            workbook = workbook,
            name = SHEET_WORKOUTS_READABLE,
            headers = listOf(
                "workoutId", "workoutName", "performedAt", "bodyWeightKg",
                "exercise", "set", "addedKg", "effectiveKg", "reps",
                "durationSec", "distanceM", "difficulty",
            ),
        ) { sheet ->
            var rowIndex = 1
            backup.workouts.sortedByDescending { it.performedAt }.forEach { workout ->
                val weList = backup.workoutExercises
                    .filter { it.workoutId == workout.id }
                    .sortedBy { it.position }
                if (weList.isEmpty()) {
                    val row = sheet.createRow(rowIndex++)
                    row.setNumber(0, workout.id.toDouble())
                    row.setText(1, workout.name)
                    row.setText(2, formatDate(workout.performedAt))
                    workout.bodyWeightKg?.let { row.setNumber(3, it) }
                } else {
                    weList.forEach { we ->
                        val sets = backup.sets
                            .filter { it.workoutExerciseId == we.id }
                            .sortedBy { it.position }
                        val exerciseName = exercisesById[we.exerciseId]?.name.orEmpty()
                        if (sets.isEmpty()) {
                            val row = sheet.createRow(rowIndex++)
                            row.setNumber(0, workout.id.toDouble())
                            row.setText(1, workout.name)
                            row.setText(2, formatDate(workout.performedAt))
                            workout.bodyWeightKg?.let { row.setNumber(3, it) }
                            row.setText(4, exerciseName)
                        } else {
                            sets.forEach { set ->
                                val effective =
                                    (we.startingResistanceKg ?: 0.0) + set.addedWeightKg
                                val row = sheet.createRow(rowIndex++)
                                row.setNumber(0, workout.id.toDouble())
                                row.setText(1, workout.name)
                                row.setText(2, formatDate(workout.performedAt))
                                workout.bodyWeightKg?.let { row.setNumber(3, it) }
                                row.setText(4, exerciseName)
                                row.setNumber(5, (set.position + 1).toDouble())
                                row.setNumber(6, set.addedWeightKg)
                                row.setNumber(7, effective)
                                row.setNumber(8, set.reps.toDouble())
                                set.durationSeconds?.let { row.setNumber(9, it.toDouble()) }
                                set.distanceMeters?.let { row.setNumber(10, it) }
                                row.setText(11, set.difficulty)
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

    suspend fun importExcel(uri: Uri) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Could not read Excel file")
        stream.use { importExcel(it) }
    }

    suspend fun importExcel(inputStream: InputStream) = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook(inputStream)
        try {
            val backup = AppBackup(
                version = CURRENT_BACKUP_VERSION,
                exportedAt = System.currentTimeMillis(),
                exercises = readExercises(requireSheet(workbook, SHEET_EXERCISES)),
                routines = readRoutines(requireSheet(workbook, SHEET_ROUTINES)),
                routineExercises = readRoutineExercises(
                    requireSheet(workbook, SHEET_ROUTINE_EXERCISES),
                ),
                workouts = readWorkouts(requireSheet(workbook, SHEET_WORKOUTS)),
                workoutExercises = readWorkoutExercises(
                    requireSheet(workbook, SHEET_WORKOUT_EXERCISES),
                ),
                sets = readSets(requireSheet(workbook, SHEET_SETS)),
            )
            restoreBackup(backup)
        } finally {
            workbook.close()
        }
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

    private fun writeSheet(
        workbook: XSSFWorkbook,
        name: String,
        headers: List<String>,
        fill: (Sheet) -> Unit,
    ) {
        val sheet = workbook.createSheet(name)
        val header = sheet.createRow(0)
        headers.forEachIndexed { i, title -> header.setText(i, title) }
        fill(sheet)
    }

    private fun requireSheet(workbook: XSSFWorkbook, name: String): Sheet =
        workbook.getSheet(name) ?: error("Excel backup is missing sheet \"$name\"")

    private fun headerIndex(sheet: Sheet): Map<String, Int> {
        val header = sheet.getRow(0) ?: error("Sheet \"${sheet.sheetName}\" has no header row")
        val map = linkedMapOf<String, Int>()
        for (cell in header) {
            val name = dataFormatter.formatCellValue(cell).trim()
            if (name.isNotEmpty()) map[name] = cell.columnIndex
        }
        return map
    }

    private fun requireColumn(headers: Map<String, Int>, name: String, sheet: String): Int =
        headers[name] ?: error("Sheet \"$sheet\" is missing column \"$name\"")

    private fun readExercises(sheet: Sheet): List<ExerciseBackup> {
        val headers = headerIndex(sheet)
        val idCol = requireColumn(headers, "id", sheet.sheetName)
        val nameCol = requireColumn(headers, "name", sheet.sheetName)
        val machineCol = headers["machine"]
        val musclesCol = requireColumn(headers, "targetedMuscles", sheet.sheetName)
        val typeCol = requireColumn(headers, "exerciseType", sheet.sheetName)
        val startingCol = headers["startingResistanceKg"]
        val restCol = headers["defaultRestSeconds"]
        return dataRows(sheet).map { row ->
            ExerciseBackup(
                id = row.requireLong(idCol, "id"),
                name = row.requireText(nameCol, "name"),
                machine = machineCol?.let { row.textOrNull(it) },
                targetedMuscles = row.textOrNull(musclesCol)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty(),
                exerciseType = row.requireText(typeCol, "exerciseType"),
                startingResistanceKg = startingCol?.let { row.doubleOrNull(it) },
                defaultRestSeconds = restCol?.let { row.intOrNull(it) },
            )
        }
    }

    private fun readRoutines(sheet: Sheet): List<RoutineBackup> {
        val headers = headerIndex(sheet)
        val idCol = requireColumn(headers, "id", sheet.sheetName)
        val nameCol = requireColumn(headers, "name", sheet.sheetName)
        val inspiredCol = headers["inspiredBy"]
        return dataRows(sheet).map { row ->
            RoutineBackup(
                id = row.requireLong(idCol, "id"),
                name = row.requireText(nameCol, "name"),
                inspiredBy = inspiredCol?.let { row.textOrNull(it) },
            )
        }
    }

    private fun readRoutineExercises(sheet: Sheet): List<RoutineExerciseBackup> {
        val headers = headerIndex(sheet)
        val idCol = requireColumn(headers, "id", sheet.sheetName)
        val routineCol = requireColumn(headers, "routineId", sheet.sheetName)
        val exerciseCol = requireColumn(headers, "exerciseId", sheet.sheetName)
        val positionCol = requireColumn(headers, "position", sheet.sheetName)
        return dataRows(sheet).map { row ->
            RoutineExerciseBackup(
                id = row.requireLong(idCol, "id"),
                routineId = row.requireLong(routineCol, "routineId"),
                exerciseId = row.requireLong(exerciseCol, "exerciseId"),
                position = row.requireInt(positionCol, "position"),
            )
        }
    }

    private fun readWorkouts(sheet: Sheet): List<WorkoutBackup> {
        val headers = headerIndex(sheet)
        val idCol = requireColumn(headers, "id", sheet.sheetName)
        val nameCol = requireColumn(headers, "name", sheet.sheetName)
        val performedCol = requireColumn(headers, "performedAt", sheet.sheetName)
        val bodyCol = headers["bodyWeightKg"]
        return dataRows(sheet).map { row ->
            WorkoutBackup(
                id = row.requireLong(idCol, "id"),
                name = row.requireText(nameCol, "name"),
                performedAt = row.requireLong(performedCol, "performedAt"),
                bodyWeightKg = bodyCol?.let { row.doubleOrNull(it) },
            )
        }
    }

    private fun readWorkoutExercises(sheet: Sheet): List<WorkoutExerciseBackup> {
        val headers = headerIndex(sheet)
        val idCol = requireColumn(headers, "id", sheet.sheetName)
        val workoutCol = requireColumn(headers, "workoutId", sheet.sheetName)
        val exerciseCol = requireColumn(headers, "exerciseId", sheet.sheetName)
        val positionCol = requireColumn(headers, "position", sheet.sheetName)
        val startingCol = headers["startingResistanceKg"]
        return dataRows(sheet).map { row ->
            WorkoutExerciseBackup(
                id = row.requireLong(idCol, "id"),
                workoutId = row.requireLong(workoutCol, "workoutId"),
                exerciseId = row.requireLong(exerciseCol, "exerciseId"),
                position = row.requireInt(positionCol, "position"),
                startingResistanceKg = startingCol?.let { row.doubleOrNull(it) },
            )
        }
    }

    private fun readSets(sheet: Sheet): List<SetBackup> {
        val headers = headerIndex(sheet)
        val idCol = requireColumn(headers, "id", sheet.sheetName)
        val weCol = requireColumn(headers, "workoutExerciseId", sheet.sheetName)
        val addedCol = requireColumn(headers, "addedWeightKg", sheet.sheetName)
        val repsCol = requireColumn(headers, "reps", sheet.sheetName)
        val difficultyCol = requireColumn(headers, "difficulty", sheet.sheetName)
        val positionCol = requireColumn(headers, "position", sheet.sheetName)
        val durationCol = headers["durationSeconds"]
        val distanceCol = headers["distanceMeters"]
        return dataRows(sheet).map { row ->
            SetBackup(
                id = row.requireLong(idCol, "id"),
                workoutExerciseId = row.requireLong(weCol, "workoutExerciseId"),
                addedWeightKg = row.requireDouble(addedCol, "addedWeightKg"),
                reps = row.requireInt(repsCol, "reps"),
                difficulty = row.requireText(difficultyCol, "difficulty"),
                position = row.requireInt(positionCol, "position"),
                durationSeconds = durationCol?.let { row.intOrNull(it) },
                distanceMeters = distanceCol?.let { row.doubleOrNull(it) },
            )
        }
    }

    private fun dataRows(sheet: Sheet): List<Row> =
        (1..sheet.lastRowNum)
            .mapNotNull { sheet.getRow(it) }
            .filter { row ->
                (0 until row.lastCellNum.coerceAtLeast(0)).any { index ->
                    val cell = row.getCell(index) ?: return@any false
                    when (cell.cellType) {
                        CellType.BLANK -> false
                        CellType.STRING -> cell.stringCellValue.isNotBlank()
                        else -> true
                    }
                }
            }

    private fun Row.setText(index: Int, value: String) {
        createCell(index).setCellValue(value)
    }

    private fun Row.setNumber(index: Int, value: Double) {
        createCell(index).setCellValue(value)
    }

    private fun Row.textOrNull(index: Int): String? {
        val cell = getCell(index) ?: return null
        val text = dataFormatter.formatCellValue(cell).trim()
        return text.takeIf { it.isNotEmpty() }
    }

    private fun Row.requireText(index: Int, column: String): String =
        textOrNull(index) ?: error("Missing text in column \"$column\"")

    private fun Row.doubleOrNull(index: Int): Double? {
        val cell = getCell(index) ?: return null
        return when (cell.cellType) {
            CellType.BLANK -> null
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            CellType.FORMULA -> runCatching { cell.numericCellValue }.getOrNull()
            else -> dataFormatter.formatCellValue(cell).trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        }
    }

    private fun Row.requireDouble(index: Int, column: String): Double =
        doubleOrNull(index) ?: error("Missing number in column \"$column\"")

    private fun Row.longOrNull(index: Int): Long? =
        doubleOrNull(index)?.toLong()

    private fun Row.requireLong(index: Int, column: String): Long =
        longOrNull(index) ?: error("Missing id/number in column \"$column\"")

    private fun Row.intOrNull(index: Int): Int? =
        doubleOrNull(index)?.toInt()

    private fun Row.requireInt(index: Int, column: String): Int =
        intOrNull(index) ?: error("Missing integer in column \"$column\"")

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(epochMillis))

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

    companion object {
        const val CURRENT_BACKUP_VERSION = 2

        private const val SHEET_EXERCISES = "Exercises"
        private const val SHEET_ROUTINES = "Routines"
        private const val SHEET_ROUTINE_EXERCISES = "RoutineExercises"
        private const val SHEET_WORKOUTS = "Workouts"
        private const val SHEET_WORKOUT_EXERCISES = "WorkoutExercises"
        private const val SHEET_SETS = "Sets"
        private const val SHEET_WORKOUTS_READABLE = "WorkoutsReadable"
    }
}

package com.example.sabr_and_iron.data.backup

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Excel backup codec. Export still uses Apache POI; import uses [SimpleXlsxReader]
 * so import does not depend on POI class init on Android/R8.
 */
internal object ExcelBackupCodec {
    fun write(backup: AppBackup, file: File) {
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

        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
    }

    fun read(inputStream: InputStream): AppBackup {
        SimpleXlsxReader(inputStream).use { reader ->
            return AppBackup(
                version = CURRENT_BACKUP_VERSION,
                exportedAt = System.currentTimeMillis(),
                exercises = readExercises(reader, SHEET_EXERCISES),
                routines = readRoutines(reader, SHEET_ROUTINES),
                routineExercises = readRoutineExercises(reader, SHEET_ROUTINE_EXERCISES),
                workouts = readWorkouts(reader, SHEET_WORKOUTS),
                workoutExercises = readWorkoutExercises(reader, SHEET_WORKOUT_EXERCISES),
                sets = readSets(reader, SHEET_SETS),
            )
        }
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

    private fun readExercises(reader: SimpleXlsxReader, sheetName: String): List<ExerciseBackup> {
        val rows = reader.readSheetRows(sheetName)
        val headers = headerIndex(rows, sheetName)
        val idCol = requireColumn(headers, "id", sheetName)
        val nameCol = requireColumn(headers, "name", sheetName)
        val machineCol = headers["machine"]
        val musclesCol = requireColumn(headers, "targetedMuscles", sheetName)
        val typeCol = requireColumn(headers, "exerciseType", sheetName)
        val startingCol = headers["startingResistanceKg"]
        val restCol = headers["defaultRestSeconds"]
        return dataRows(rows).map { row ->
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

    private fun readRoutines(reader: SimpleXlsxReader, sheetName: String): List<RoutineBackup> {
        val rows = reader.readSheetRows(sheetName)
        val headers = headerIndex(rows, sheetName)
        val idCol = requireColumn(headers, "id", sheetName)
        val nameCol = requireColumn(headers, "name", sheetName)
        val inspiredCol = headers["inspiredBy"]
        return dataRows(rows).map { row ->
            RoutineBackup(
                id = row.requireLong(idCol, "id"),
                name = row.requireText(nameCol, "name"),
                inspiredBy = inspiredCol?.let { row.textOrNull(it) },
            )
        }
    }

    private fun readRoutineExercises(
        reader: SimpleXlsxReader,
        sheetName: String,
    ): List<RoutineExerciseBackup> {
        val rows = reader.readSheetRows(sheetName)
        val headers = headerIndex(rows, sheetName)
        val idCol = requireColumn(headers, "id", sheetName)
        val routineCol = requireColumn(headers, "routineId", sheetName)
        val exerciseCol = requireColumn(headers, "exerciseId", sheetName)
        val positionCol = requireColumn(headers, "position", sheetName)
        return dataRows(rows).map { row ->
            RoutineExerciseBackup(
                id = row.requireLong(idCol, "id"),
                routineId = row.requireLong(routineCol, "routineId"),
                exerciseId = row.requireLong(exerciseCol, "exerciseId"),
                position = row.requireInt(positionCol, "position"),
            )
        }
    }

    private fun readWorkouts(reader: SimpleXlsxReader, sheetName: String): List<WorkoutBackup> {
        val rows = reader.readSheetRows(sheetName)
        val headers = headerIndex(rows, sheetName)
        val idCol = requireColumn(headers, "id", sheetName)
        val nameCol = requireColumn(headers, "name", sheetName)
        val performedCol = requireColumn(headers, "performedAt", sheetName)
        val bodyCol = headers["bodyWeightKg"]
        return dataRows(rows).map { row ->
            WorkoutBackup(
                id = row.requireLong(idCol, "id"),
                name = row.requireText(nameCol, "name"),
                performedAt = row.requireLong(performedCol, "performedAt"),
                bodyWeightKg = bodyCol?.let { row.doubleOrNull(it) },
            )
        }
    }

    private fun readWorkoutExercises(
        reader: SimpleXlsxReader,
        sheetName: String,
    ): List<WorkoutExerciseBackup> {
        val rows = reader.readSheetRows(sheetName)
        val headers = headerIndex(rows, sheetName)
        val idCol = requireColumn(headers, "id", sheetName)
        val workoutCol = requireColumn(headers, "workoutId", sheetName)
        val exerciseCol = requireColumn(headers, "exerciseId", sheetName)
        val positionCol = requireColumn(headers, "position", sheetName)
        val startingCol = headers["startingResistanceKg"]
        return dataRows(rows).map { row ->
            WorkoutExerciseBackup(
                id = row.requireLong(idCol, "id"),
                workoutId = row.requireLong(workoutCol, "workoutId"),
                exerciseId = row.requireLong(exerciseCol, "exerciseId"),
                position = row.requireInt(positionCol, "position"),
                startingResistanceKg = startingCol?.let { row.doubleOrNull(it) },
            )
        }
    }

    private fun readSets(reader: SimpleXlsxReader, sheetName: String): List<SetBackup> {
        val rows = reader.readSheetRows(sheetName)
        val headers = headerIndex(rows, sheetName)
        val idCol = requireColumn(headers, "id", sheetName)
        val weCol = requireColumn(headers, "workoutExerciseId", sheetName)
        val addedCol = requireColumn(headers, "addedWeightKg", sheetName)
        val repsCol = requireColumn(headers, "reps", sheetName)
        val difficultyCol = requireColumn(headers, "difficulty", sheetName)
        val positionCol = requireColumn(headers, "position", sheetName)
        val durationCol = headers["durationSeconds"]
        val distanceCol = headers["distanceMeters"]
        return dataRows(rows).map { row ->
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

    private fun headerIndex(
        rows: List<Map<Int, String>>,
        sheetName: String,
    ): Map<String, Int> {
        val header = rows.firstOrNull()
            ?: error("Sheet \"$sheetName\" has no header row")
        return header.entries
            .associate { (col, name) -> name.trim() to col }
            .filterKeys { it.isNotEmpty() }
    }

    private fun requireColumn(headers: Map<String, Int>, name: String, sheet: String): Int =
        headers[name] ?: error("Sheet \"$sheet\" is missing column \"$name\"")

    private fun dataRows(rows: List<Map<Int, String>>): List<Map<Int, String>> =
        if (rows.isEmpty()) emptyList() else rows.drop(1)

    private fun Map<Int, String>.textOrNull(index: Int): String? =
        this[index]?.trim()?.takeIf { it.isNotEmpty() }

    private fun Map<Int, String>.requireText(index: Int, column: String): String =
        textOrNull(index) ?: error("Missing text in column \"$column\"")

    private fun Map<Int, String>.doubleOrNull(index: Int): Double? =
        textOrNull(index)?.toDoubleOrNull()

    private fun Map<Int, String>.requireDouble(index: Int, column: String): Double =
        doubleOrNull(index) ?: error("Missing number in column \"$column\"")

    private fun Map<Int, String>.longOrNull(index: Int): Long? =
        doubleOrNull(index)?.toLong()

    private fun Map<Int, String>.requireLong(index: Int, column: String): Long =
        longOrNull(index) ?: error("Missing id/number in column \"$column\"")

    private fun Map<Int, String>.intOrNull(index: Int): Int? =
        doubleOrNull(index)?.toInt()

    private fun Map<Int, String>.requireInt(index: Int, column: String): Int =
        intOrNull(index) ?: error("Missing integer in column \"$column\"")

    private fun Row.setText(index: Int, value: String) {
        createCell(index).setCellValue(value)
    }

    private fun Row.setNumber(index: Int, value: Double) {
        createCell(index).setCellValue(value)
    }

    private fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(epochMillis))

    private const val SHEET_EXERCISES = "Exercises"
    private const val SHEET_ROUTINES = "Routines"
    private const val SHEET_ROUTINE_EXERCISES = "RoutineExercises"
    private const val SHEET_WORKOUTS = "Workouts"
    private const val SHEET_WORKOUT_EXERCISES = "WorkoutExercises"
    private const val SHEET_SETS = "Sets"
    private const val SHEET_WORKOUTS_READABLE = "WorkoutsReadable"
}

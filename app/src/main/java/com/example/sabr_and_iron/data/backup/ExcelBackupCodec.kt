package com.example.sabr_and_iron.data.backup

import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Excel backup codec using POI-free [SimpleXlsxReader] / [SimpleXlsxWriter]
 * so import/export work under Android R8.
 */
internal object ExcelBackupCodec {
    fun write(backup: AppBackup, file: File) {
        val exercisesById = backup.exercises.associateBy { it.id }
        val sheets = listOf(
            SimpleXlsxWriter.Sheet(
                name = SHEET_EXERCISES,
                rows = buildList {
                    add(
                        listOf(
                            "id", "name", "machine", "targetedMuscles", "exerciseType",
                            "startingResistanceKg", "defaultRestSeconds",
                        ),
                    )
                    backup.exercises.forEach { item ->
                        add(
                            listOf(
                                item.id,
                                item.name,
                                item.machine,
                                item.targetedMuscles.joinToString(","),
                                item.exerciseType,
                                item.startingResistanceKg,
                                item.defaultRestSeconds,
                            ),
                        )
                    }
                },
            ),
            SimpleXlsxWriter.Sheet(
                name = SHEET_ROUTINES,
                rows = buildList {
                    add(listOf("id", "name", "inspiredBy"))
                    backup.routines.forEach { item ->
                        add(listOf(item.id, item.name, item.inspiredBy))
                    }
                },
            ),
            SimpleXlsxWriter.Sheet(
                name = SHEET_ROUTINE_EXERCISES,
                rows = buildList {
                    add(listOf("id", "routineId", "exerciseId", "position"))
                    backup.routineExercises.forEach { item ->
                        add(listOf(item.id, item.routineId, item.exerciseId, item.position))
                    }
                },
            ),
            SimpleXlsxWriter.Sheet(
                name = SHEET_WORKOUTS,
                rows = buildList {
                    add(listOf("id", "name", "performedAt", "bodyWeightKg"))
                    backup.workouts.forEach { item ->
                        add(listOf(item.id, item.name, item.performedAt, item.bodyWeightKg))
                    }
                },
            ),
            SimpleXlsxWriter.Sheet(
                name = SHEET_WORKOUT_EXERCISES,
                rows = buildList {
                    add(
                        listOf(
                            "id", "workoutId", "exerciseId", "position", "startingResistanceKg",
                        ),
                    )
                    backup.workoutExercises.forEach { item ->
                        add(
                            listOf(
                                item.id,
                                item.workoutId,
                                item.exerciseId,
                                item.position,
                                item.startingResistanceKg,
                            ),
                        )
                    }
                },
            ),
            SimpleXlsxWriter.Sheet(
                name = SHEET_SETS,
                rows = buildList {
                    add(
                        listOf(
                            "id", "workoutExerciseId", "addedWeightKg", "reps", "difficulty",
                            "position", "durationSeconds", "distanceMeters",
                        ),
                    )
                    backup.sets.forEach { item ->
                        add(
                            listOf(
                                item.id,
                                item.workoutExerciseId,
                                item.addedWeightKg,
                                item.reps,
                                item.difficulty,
                                item.position,
                                item.durationSeconds,
                                item.distanceMeters,
                            ),
                        )
                    }
                },
            ),
            SimpleXlsxWriter.Sheet(
                name = SHEET_WORKOUTS_READABLE,
                rows = buildList {
                    add(
                        listOf(
                            "workoutId", "workoutName", "performedAt", "bodyWeightKg",
                            "exercise", "set", "addedKg", "effectiveKg", "reps",
                            "durationSec", "distanceM", "difficulty",
                        ),
                    )
                    backup.workouts.sortedByDescending { it.performedAt }.forEach { workout ->
                        val weList = backup.workoutExercises
                            .filter { it.workoutId == workout.id }
                            .sortedBy { it.position }
                        if (weList.isEmpty()) {
                            add(
                                listOf(
                                    workout.id,
                                    workout.name,
                                    formatDate(workout.performedAt),
                                    workout.bodyWeightKg,
                                    null, null, null, null, null, null, null, null,
                                ),
                            )
                        } else {
                            weList.forEach { we ->
                                val sets = backup.sets
                                    .filter { it.workoutExerciseId == we.id }
                                    .sortedBy { it.position }
                                val exerciseName = exercisesById[we.exerciseId]?.name.orEmpty()
                                if (sets.isEmpty()) {
                                    add(
                                        listOf(
                                            workout.id,
                                            workout.name,
                                            formatDate(workout.performedAt),
                                            workout.bodyWeightKg,
                                            exerciseName,
                                            null, null, null, null, null, null, null,
                                        ),
                                    )
                                } else {
                                    sets.forEach { set ->
                                        val effective =
                                            (we.startingResistanceKg ?: 0.0) + set.addedWeightKg
                                        add(
                                            listOf(
                                                workout.id,
                                                workout.name,
                                                formatDate(workout.performedAt),
                                                workout.bodyWeightKg,
                                                exerciseName,
                                                set.position + 1,
                                                set.addedWeightKg,
                                                effective,
                                                set.reps,
                                                set.durationSeconds,
                                                set.distanceMeters,
                                                set.difficulty,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ),
        )
        SimpleXlsxWriter.write(file, sheets)
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

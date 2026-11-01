package com.example.sabr_and_iron.data.local

import com.example.sabr_and_iron.data.local.dao.ExerciseDao
import com.example.sabr_and_iron.data.local.dao.RoutineDao
import com.example.sabr_and_iron.data.local.dao.WorkoutDao
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.entity.RoutineEntity
import com.example.sabr_and_iron.data.local.entity.RoutineExerciseEntity
import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutEntity
import com.example.sabr_and_iron.data.local.entity.WorkoutExerciseEntity
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import java.util.concurrent.TimeUnit

/**
 * Sample routines / workouts for demos. Also clears all user data and reseeds the library.
 */
class DummyDataSeeder(
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutDao: WorkoutDao,
) {
    suspend fun seedIfEmpty(): SeedResult {
        if (workoutDao.count() > 0) {
            return SeedResult.AlreadyPopulated
        }
        seed()
        return SeedResult.Inserted
    }

    suspend fun clearAll() {
        workoutDao.deleteAllSets()
        workoutDao.deleteAllWorkoutExercises()
        workoutDao.deleteAllWorkouts()
        routineDao.deleteAllRoutineExercises()
        routineDao.deleteAllRoutines()
        exerciseDao.deleteAll()
        exerciseDao.insertAll(DefaultExercises.seed)
    }

    suspend fun seed() {
        ensureDefaultExercises()
        val byName = exerciseDao.getAll().associateBy { it.name }
        fun id(name: String): Long =
            byName[name]?.id ?: error("Missing seed exercise: $name")

        if (routineDao.count() == 0) {
            seedRoutines(::id)
        }
        seedWorkouts(::id, byName)
    }

    private suspend fun ensureDefaultExercises() {
        val existing = exerciseDao.getAll().associateBy { it.name }
        DefaultExercises.seed.forEach { seedExercise ->
            if (seedExercise.name !in existing) {
                exerciseDao.insert(seedExercise)
            }
        }
        // Upgrade known type changes for demos (Plank → TimeOnly)
        existing["Plank"]?.let { plank ->
            if (plank.exerciseType != ExerciseType.TimeOnly) {
                exerciseDao.update(plank.copy(exerciseType = ExerciseType.TimeOnly))
            }
        }
    }

    private suspend fun seedRoutines(id: (String) -> Long) {
        val pushId = routineDao.insert(
            RoutineEntity(name = "Push Day", inspiredBy = "Sample data"),
        )
        routineDao.insertExercises(
            listOf(
                "Barbell Bench Press",
                "Overhead Press",
                "Incline Dumbbell Press",
                "Dumbbell Lateral Raise",
                "Triceps Pushdown",
            ).mapIndexed { index, name ->
                RoutineExerciseEntity(routineId = pushId, exerciseId = id(name), position = index)
            },
        )

        val pullId = routineDao.insert(
            RoutineEntity(name = "Pull Day", inspiredBy = "Sample data"),
        )
        routineDao.insertExercises(
            listOf(
                "Conventional Deadlift",
                "Barbell Row",
                "Pull-up",
                "Assisted Pull-up",
                "Lat Pulldown",
                "Bicep Curl",
            ).mapIndexed { index, name ->
                RoutineExerciseEntity(routineId = pullId, exerciseId = id(name), position = index)
            },
        )

        val legsId = routineDao.insert(
            RoutineEntity(name = "Legs Day", inspiredBy = "Sample data"),
        )
        routineDao.insertExercises(
            listOf(
                "Barbell Back Squat",
                "Romanian Deadlift",
                "Leg Press",
                "Plank",
            ).mapIndexed { index, name ->
                RoutineExerciseEntity(routineId = legsId, exerciseId = id(name), position = index)
            },
        )

        val cardioId = routineDao.insert(
            RoutineEntity(name = "Easy Cardio", inspiredBy = "Sample data"),
        )
        routineDao.insertExercises(
            listOf("Easy Run", "Plank").mapIndexed { index, name ->
                RoutineExerciseEntity(routineId = cardioId, exerciseId = id(name), position = index)
            },
        )
    }

    private suspend fun seedWorkouts(
        id: (String) -> Long,
        byName: Map<String, ExerciseEntity>,
    ) {
        val now = System.currentTimeMillis()
        val dayMs = TimeUnit.DAYS.toMillis(1)

        data class Lift(
            val exerciseName: String,
            val addedWeightKg: Double = 0.0,
            val reps: Int = 0,
            val durationSeconds: Int? = null,
            val distanceMeters: Double? = null,
        )

        data class SessionSpec(
            val name: String,
            val daysAgo: Long,
            val bodyWeightKg: Double,
            val lifts: List<Lift>,
        )

        fun plates(kg: Double) = kg

        val sessions = listOf(
            SessionSpec("Legs A", 56, 82.0, listOf(
                Lift("Barbell Back Squat", plates(60.0), 5),
                Lift("Romanian Deadlift", plates(50.0), 8),
                Lift("Leg Press", 120.0, 10),
                Lift("Plank", durationSeconds = 45),
            )),
            SessionSpec("Push A", 54, 82.2, listOf(
                Lift("Barbell Bench Press", plates(50.0), 5),
                Lift("Overhead Press", plates(30.0), 6),
                Lift("Incline Dumbbell Press", 22.0, 10),
                Lift("Dumbbell Lateral Raise", 8.0, 12),
                Lift("Triceps Pushdown", 25.0, 12),
            )),
            SessionSpec("Pull A", 52, 82.0, listOf(
                Lift("Conventional Deadlift", plates(80.0), 3),
                Lift("Barbell Row", plates(45.0), 8),
                Lift("Pull-up", 0.0, 6),
                Lift("Assisted Pull-up", 20.0, 8),
                Lift("Lat Pulldown", 50.0, 10),
                Lift("Bicep Curl", 12.0, 12),
            )),
            SessionSpec("Cardio A", 51, 82.0, listOf(
                Lift("Easy Run", durationSeconds = 28 * 60, distanceMeters = 4500.0),
                Lift("Plank", durationSeconds = 50),
            )),
            SessionSpec("Legs B", 49, 82.5, listOf(
                Lift("Barbell Back Squat", plates(65.0), 5),
                Lift("Romanian Deadlift", plates(55.0), 8),
                Lift("Leg Press", 130.0, 10),
                Lift("Plank", durationSeconds = 55),
            )),
            SessionSpec("Push B", 47, 82.4, listOf(
                Lift("Barbell Bench Press", plates(55.0), 5),
                Lift("Overhead Press", plates(32.5), 6),
                Lift("Incline Dumbbell Press", 24.0, 10),
                Lift("Dumbbell Lateral Raise", 8.0, 12),
                Lift("Triceps Pushdown", 27.5, 12),
            )),
            SessionSpec("Pull B", 45, 82.6, listOf(
                Lift("Conventional Deadlift", plates(90.0), 3),
                Lift("Barbell Row", plates(50.0), 8),
                Lift("Pull-up", 5.0, 6),
                Lift("Assisted Pull-up", 15.0, 8),
                Lift("Lat Pulldown", 55.0, 10),
                Lift("Bicep Curl", 14.0, 12),
            )),
            SessionSpec("Cardio B", 44, 82.6, listOf(
                Lift("Easy Run", durationSeconds = 30 * 60, distanceMeters = 5000.0),
                Lift("Plank", durationSeconds = 60),
            )),
            SessionSpec("Legs C", 42, 83.0, listOf(
                Lift("Barbell Back Squat", plates(70.0), 5),
                Lift("Romanian Deadlift", plates(60.0), 8),
                Lift("Leg Press", 140.0, 10),
                Lift("Plank", durationSeconds = 60),
            )),
            SessionSpec("Push C", 40, 82.8, listOf(
                Lift("Barbell Bench Press", plates(57.5), 5),
                Lift("Overhead Press", plates(35.0), 5),
                Lift("Incline Dumbbell Press", 24.0, 10),
                Lift("Dumbbell Lateral Raise", 10.0, 12),
                Lift("Triceps Pushdown", 30.0, 12),
            )),
            SessionSpec("Pull C", 38, 83.1, listOf(
                Lift("Conventional Deadlift", plates(100.0), 3),
                Lift("Barbell Row", plates(52.5), 8),
                Lift("Pull-up", 5.0, 7),
                Lift("Assisted Pull-up", 12.5, 8),
                Lift("Lat Pulldown", 57.5, 10),
                Lift("Bicep Curl", 14.0, 12),
            )),
            SessionSpec("Cardio C", 37, 83.1, listOf(
                Lift("Easy Run", durationSeconds = 32 * 60, distanceMeters = 5500.0),
                Lift("Plank", durationSeconds = 70),
            )),
            SessionSpec("Legs D", 28, 83.5, listOf(
                Lift("Barbell Back Squat", plates(80.0), 5),
                Lift("Romanian Deadlift", plates(70.0), 8),
                Lift("Leg Press", 160.0, 10),
                Lift("Plank", durationSeconds = 75),
            )),
            SessionSpec("Push D", 26, 83.3, listOf(
                Lift("Barbell Bench Press", plates(65.0), 5),
                Lift("Overhead Press", plates(40.0), 5),
                Lift("Incline Dumbbell Press", 26.0, 10),
                Lift("Dumbbell Lateral Raise", 10.0, 14),
                Lift("Triceps Pushdown", 35.0, 10),
            )),
            SessionSpec("Pull D", 24, 83.6, listOf(
                Lift("Conventional Deadlift", plates(120.0), 3),
                Lift("Barbell Row", plates(57.5), 8),
                Lift("Pull-up", 10.0, 7),
                Lift("Assisted Pull-up", 10.0, 10),
                Lift("Lat Pulldown", 62.5, 10),
                Lift("Bicep Curl", 16.0, 12),
            )),
            SessionSpec("Cardio D", 21, 83.6, listOf(
                Lift("Easy Run", durationSeconds = 35 * 60, distanceMeters = 6000.0),
                Lift("Plank", durationSeconds = 80),
            )),
            SessionSpec("Legs E", 14, 84.0, listOf(
                Lift("Barbell Back Squat", plates(85.0), 5),
                Lift("Romanian Deadlift", plates(75.0), 8),
                Lift("Leg Press", 170.0, 10),
                Lift("Plank", durationSeconds = 90),
            )),
            SessionSpec("Push E", 10, 83.8, listOf(
                Lift("Barbell Bench Press", plates(70.0), 5),
                Lift("Overhead Press", plates(42.5), 5),
                Lift("Incline Dumbbell Press", 28.0, 10),
                Lift("Dumbbell Lateral Raise", 12.0, 12),
                Lift("Triceps Pushdown", 37.5, 10),
            )),
            SessionSpec("Pull E", 7, 84.2, listOf(
                Lift("Conventional Deadlift", plates(130.0), 3),
                Lift("Barbell Row", plates(60.0), 8),
                Lift("Pull-up", 15.0, 6),
                Lift("Assisted Pull-up", 7.5, 10),
                Lift("Lat Pulldown", 65.0, 10),
                Lift("Bicep Curl", 18.0, 10),
            )),
            SessionSpec("Cardio E", 3, 84.0, listOf(
                Lift("Easy Run", durationSeconds = 40 * 60, distanceMeters = 7000.0),
                Lift("Plank", durationSeconds = 100),
            )),
            SessionSpec("Legs F", 1, 84.1, listOf(
                Lift("Barbell Back Squat", plates(90.0), 5),
                Lift("Romanian Deadlift", plates(80.0), 8),
                Lift("Leg Press", 180.0, 8),
                Lift("Plank", durationSeconds = 110),
            )),
        )

        sessions.forEach { session ->
            val performedAt = now - session.daysAgo * dayMs - TimeUnit.HOURS.toMillis(18)
            val workoutId = workoutDao.insert(
                WorkoutEntity(
                    name = session.name,
                    performedAt = performedAt,
                    bodyWeightKg = session.bodyWeightKg,
                ),
            )
            session.lifts.forEachIndexed { position, lift ->
                val exercise = byName[lift.exerciseName]
                    ?: error("Missing seed exercise: ${lift.exerciseName}")
                val weId = workoutDao.insertExercise(
                    WorkoutExerciseEntity(
                        workoutId = workoutId,
                        exerciseId = id(lift.exerciseName),
                        position = position,
                        startingResistanceKg = exercise.startingResistanceKg,
                    ),
                )
                val sets = when (exercise.exerciseType) {
                    ExerciseType.TimeOnly -> {
                        listOf(
                            SetEntity(
                                workoutExerciseId = weId,
                                difficulty = SetDifficulty.Ideal,
                                position = 0,
                                durationSeconds = lift.durationSeconds,
                            ),
                            SetEntity(
                                workoutExerciseId = weId,
                                difficulty = SetDifficulty.Hard,
                                position = 1,
                                durationSeconds = (lift.durationSeconds ?: 0) + 10,
                            ),
                        )
                    }
                    ExerciseType.DistanceTime -> {
                        listOf(
                            SetEntity(
                                workoutExerciseId = weId,
                                difficulty = SetDifficulty.Ideal,
                                position = 0,
                                durationSeconds = lift.durationSeconds,
                                distanceMeters = lift.distanceMeters,
                            ),
                        )
                    }
                    else -> {
                        val difficulties = listOf(
                            SetDifficulty.LightWork,
                            SetDifficulty.Ideal,
                            SetDifficulty.Ideal,
                            SetDifficulty.Hard,
                        )
                        val setCount = 4
                        (0 until setCount).map { setIndex ->
                            val taper = when (setIndex) {
                                0 -> 0.9
                                setCount - 1 -> 1.0
                                else -> 0.95 + (setIndex * 0.02)
                            }
                            SetEntity(
                                workoutExerciseId = weId,
                                addedWeightKg = (lift.addedWeightKg * taper).let {
                                    (it / 2.5).toInt() * 2.5
                                },
                                reps = when {
                                    setIndex == setCount - 1 -> (lift.reps - 1).coerceAtLeast(1)
                                    else -> lift.reps
                                },
                                difficulty = difficulties[setIndex.coerceAtMost(difficulties.lastIndex)],
                                position = setIndex,
                            )
                        }
                    }
                }
                workoutDao.insertSets(sets)
            }
        }
    }

    enum class SeedResult {
        Inserted,
        AlreadyPopulated,
    }
}

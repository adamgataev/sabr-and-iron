package com.example.sabr_and_iron.data.repository

import com.example.sabr_and_iron.data.local.dao.WorkoutDao
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.PersonalRecord
import com.example.sabr_and_iron.data.local.model.ProgressMetric
import com.example.sabr_and_iron.data.local.model.ProgressPoint
import com.example.sabr_and_iron.data.local.model.SetMetricRow
import com.example.sabr_and_iron.data.local.model.effectiveLoadKg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecordsRepository(
    private val workoutDao: WorkoutDao,
) {
    fun observePersonalRecords(): Flow<List<PersonalRecord>> =
        workoutDao.observeAllSetMetrics().map { rows -> computePersonalRecords(rows) }

    fun observeProgress(exerciseId: Long): Flow<List<ProgressPoint>> =
        workoutDao.observeSetMetricsForExercise(exerciseId).map { rows ->
            computeProgress(rows)
        }

    private fun computePersonalRecords(rows: List<SetMetricRow>): List<PersonalRecord> {
        return rows
            .groupBy { it.exerciseId }
            .mapNotNull { (_, exerciseRows) ->
                val sample = exerciseRows.first()
                when (sample.exerciseType) {
                    ExerciseType.Regular,
                    ExerciseType.WeightedBodyweight,
                    ExerciseType.Assisted,
                    -> bestLoadRecord(exerciseRows)

                    ExerciseType.TimeOnly -> bestDurationRecord(exerciseRows)
                    ExerciseType.DistanceTime -> bestDistanceRecord(exerciseRows)
                }
            }
            .sortedBy { it.exerciseName.lowercase() }
    }

    private fun bestLoadRecord(rows: List<SetMetricRow>): PersonalRecord? {
        val scored = rows.mapNotNull { row ->
            val load = effectiveLoadKg(
                row.exerciseType,
                row.startingResistanceKg,
                row.addedWeightKg,
                row.bodyWeightKg,
            ) ?: return@mapNotNull null
            Triple(load, row.reps, row)
        }
        val best = scored.maxWithOrNull(
            compareBy<Triple<Double, Int, SetMetricRow>> { it.first }
                .thenBy { it.second }
                .thenBy { it.third.performedAt },
        ) ?: return null
        val row = best.third
        return PersonalRecord(
            exerciseId = row.exerciseId,
            exerciseName = row.exerciseName,
            exerciseType = row.exerciseType,
            performedAt = row.performedAt,
            effectiveWeightKg = best.first,
            reps = best.second,
        )
    }

    private fun bestDurationRecord(rows: List<SetMetricRow>): PersonalRecord? {
        val best = rows
            .mapNotNull { row -> row.durationSeconds?.let { it to row } }
            .maxWithOrNull(
                compareBy<Pair<Int, SetMetricRow>> { it.first }
                    .thenBy { it.second.performedAt },
            ) ?: return null
        val row = best.second
        return PersonalRecord(
            exerciseId = row.exerciseId,
            exerciseName = row.exerciseName,
            exerciseType = row.exerciseType,
            performedAt = row.performedAt,
            durationSeconds = best.first,
        )
    }

    private fun bestDistanceRecord(rows: List<SetMetricRow>): PersonalRecord? {
        val best = rows
            .mapNotNull { row -> row.distanceMeters?.let { it to row } }
            .maxWithOrNull(
                compareBy<Pair<Double, SetMetricRow>> { it.first }
                    .thenByDescending { it.second.durationSeconds ?: Int.MAX_VALUE }
                    .thenBy { it.second.performedAt },
            ) ?: return null
        val row = best.second
        return PersonalRecord(
            exerciseId = row.exerciseId,
            exerciseName = row.exerciseName,
            exerciseType = row.exerciseType,
            performedAt = row.performedAt,
            distanceMeters = best.first,
            durationSeconds = row.durationSeconds,
        )
    }

    private fun computeProgress(rows: List<SetMetricRow>): List<ProgressPoint> {
        if (rows.isEmpty()) return emptyList()
        val type = rows.first().exerciseType
        return rows
            .groupBy { it.workoutId }
            .values
            .mapNotNull { workoutRows ->
                val sample = workoutRows.first()
                when (type) {
                    ExerciseType.Regular,
                    ExerciseType.WeightedBodyweight,
                    ExerciseType.Assisted,
                    -> {
                        val best = workoutRows.mapNotNull { row ->
                            effectiveLoadKg(
                                row.exerciseType,
                                row.startingResistanceKg,
                                row.addedWeightKg,
                                row.bodyWeightKg,
                            )
                        }.maxOrNull() ?: return@mapNotNull null
                        ProgressPoint(
                            exerciseId = sample.exerciseId,
                            performedAt = sample.performedAt,
                            metricValue = best,
                            metric = ProgressMetric.LoadKg,
                            bodyWeightKg = sample.bodyWeightKg,
                        )
                    }
                    ExerciseType.TimeOnly -> {
                        val best = workoutRows.mapNotNull { it.durationSeconds }.maxOrNull()
                            ?: return@mapNotNull null
                        ProgressPoint(
                            exerciseId = sample.exerciseId,
                            performedAt = sample.performedAt,
                            metricValue = best.toDouble(),
                            metric = ProgressMetric.DurationSeconds,
                            bodyWeightKg = sample.bodyWeightKg,
                        )
                    }
                    ExerciseType.DistanceTime -> {
                        val best = workoutRows.mapNotNull { it.distanceMeters }.maxOrNull()
                            ?: return@mapNotNull null
                        ProgressPoint(
                            exerciseId = sample.exerciseId,
                            performedAt = sample.performedAt,
                            metricValue = best,
                            metric = ProgressMetric.DistanceMeters,
                            bodyWeightKg = sample.bodyWeightKg,
                        )
                    }
                }
            }
            .sortedBy { it.performedAt }
    }
}

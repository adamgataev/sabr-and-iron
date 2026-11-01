package com.example.sabr_and_iron.data.local.model

/** Legacy helper: regular bar + plates. Prefer [effectiveLoadKg] with an [ExerciseType]. */
fun effectiveWeightKg(startingResistanceKg: Double?, addedWeightKg: Double): Double =
    (startingResistanceKg ?: 0.0) + addedWeightKg

/**
 * Load used for PRs / charts.
 * - Regular: starting resistance + added
 * - Weighted bodyweight: body weight + added
 * - Assisted: body weight − assistance (added), floored at 0
 * Returns null when body weight is required but missing, or type is not a load type.
 */
fun effectiveLoadKg(
    type: ExerciseType,
    startingResistanceKg: Double?,
    addedWeightKg: Double,
    bodyWeightKg: Double?,
): Double? =
    when (type) {
        ExerciseType.Regular -> (startingResistanceKg ?: 0.0) + addedWeightKg
        ExerciseType.WeightedBodyweight -> {
            val body = bodyWeightKg ?: return null
            body + addedWeightKg
        }
        ExerciseType.Assisted -> {
            val body = bodyWeightKg ?: return null
            (body - addedWeightKg).coerceAtLeast(0.0)
        }
        ExerciseType.DistanceTime, ExerciseType.TimeOnly -> null
    }

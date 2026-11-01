package com.example.sabr_and_iron.data.local.model

data class ProgressPoint(
    val exerciseId: Long,
    val performedAt: Long,
    /** Chart Y: kg, seconds, or meters depending on [metric]. */
    val metricValue: Double,
    val metric: ProgressMetric,
    val bodyWeightKg: Double?,
)

enum class ProgressMetric {
    LoadKg,
    DurationSeconds,
    DistanceMeters,
}

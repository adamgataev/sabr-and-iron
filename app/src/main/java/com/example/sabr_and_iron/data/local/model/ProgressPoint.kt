package com.example.sabr_and_iron.data.local.model

data class ProgressPoint(
    val exerciseId: Long,
    val performedAt: Long,
    /** Chart Y: kg, seconds, or meters depending on [metric]. */
    val metricValue: Double,
    val metric: ProgressMetric,
    val bodyWeightKg: Double?,
    /**
     * For [ProgressMetric.DistanceMeters] (Distance+Time): duration seconds of the same set,
     * so a second chart can share X indices with the distance series.
     */
    val secondaryMetricValue: Double? = null,
)

enum class ProgressMetric {
    LoadKg,
    DurationSeconds,
    DistanceMeters,
}

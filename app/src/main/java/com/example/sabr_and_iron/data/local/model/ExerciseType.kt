package com.example.sabr_and_iron.data.local.model

enum class ExerciseType(val label: String) {
    Regular("Regular"),
    WeightedBodyweight("Weighted Bodyweight"),
    Assisted("Assisted"),
    DistanceTime("Distance + Time"),
    TimeOnly("Time only"),
    ;

    val usesLoad: Boolean
        get() = this == Regular || this == WeightedBodyweight || this == Assisted

    val usesBodyWeight: Boolean
        get() = this == WeightedBodyweight || this == Assisted

    val usesStartingResistance: Boolean
        get() = this == Regular

    val usesDistance: Boolean
        get() = this == DistanceTime

    val usesDuration: Boolean
        get() = this == DistanceTime || this == TimeOnly

    val usesReps: Boolean
        get() = usesLoad
}

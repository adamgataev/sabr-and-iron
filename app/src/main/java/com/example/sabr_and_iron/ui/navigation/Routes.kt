package com.example.sabr_and_iron.ui.navigation

object Routes {
    const val WorkoutNew = "workout/new"
    const val WorkoutDetail = "workout/{workoutId}"
    const val ActiveExercise = "workout/{workoutId}/exercise/{workoutExerciseId}"
    const val ActiveExerciseDeepLink =
        "sabrandiron://workout/{workoutId}/exercise/{workoutExerciseId}"
    const val RoutineNew = "routine/new"
    const val RoutineEdit = "routine/{routineId}"
    const val ExerciseNew = "exercise/new"
    const val ExerciseDetail = "exercise/{exerciseId}"
    const val ExerciseEdit = "exercise/{exerciseId}/edit"
    const val RecordDetail = "records/{exerciseId}"
    const val Settings = "settings"

    fun workoutDetail(workoutId: Long) = "workout/$workoutId"
    fun activeExercise(workoutId: Long, workoutExerciseId: Long) =
        "workout/$workoutId/exercise/$workoutExerciseId"

    fun activeExerciseDeepLink(workoutId: Long, workoutExerciseId: Long) =
        "sabrandiron://workout/$workoutId/exercise/$workoutExerciseId"

    fun routineEdit(routineId: Long) = "routine/$routineId"
    fun exerciseDetail(exerciseId: Long) = "exercise/$exerciseId"
    fun exerciseEdit(exerciseId: Long) = "exercise/$exerciseId/edit"
    fun recordDetail(exerciseId: Long) = "records/$exerciseId"
}

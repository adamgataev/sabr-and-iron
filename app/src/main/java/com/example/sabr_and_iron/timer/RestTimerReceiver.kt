package com.example.sabr_and_iron.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty().ifBlank { "your set" }
        val workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, 0L)
        val workoutExerciseId = intent.getLongExtra(EXTRA_WORKOUT_EXERCISE_ID, 0L)
        RestTimerNotifications.vibrateFiveSeconds(context)
        RestTimerNotifications.showRestComplete(
            context = context,
            exerciseName = name,
            workoutId = workoutId,
            workoutExerciseId = workoutExerciseId,
        )
    }

    companion object {
        const val ACTION_REST_COMPLETE = "com.example.sabr_and_iron.REST_COMPLETE"
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_WORKOUT_ID = "workout_id"
        const val EXTRA_WORKOUT_EXERCISE_ID = "workout_exercise_id"
    }
}

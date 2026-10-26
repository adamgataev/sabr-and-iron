package com.example.sabr_and_iron.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_EXERCISE_NAME).orEmpty().ifBlank { "your set" }
        RestTimerNotifications.vibrateFiveSeconds(context)
        RestTimerNotifications.showRestComplete(context, name)
    }

    companion object {
        const val ACTION_REST_COMPLETE = "com.example.sabr_and_iron.REST_COMPLETE"
        const val EXTRA_EXERCISE_NAME = "exercise_name"
    }
}

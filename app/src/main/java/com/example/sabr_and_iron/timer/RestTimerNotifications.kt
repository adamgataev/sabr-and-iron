package com.example.sabr_and_iron.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sabr_and_iron.MainActivity
import com.example.sabr_and_iron.R
import com.example.sabr_and_iron.ui.navigation.Routes

object RestTimerNotifications {
    const val CHANNEL_ID = "rest_timer"
    const val COMPLETE_CHANNEL_ID = "rest_timer_complete"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val running = NotificationChannel(
            CHANNEL_ID,
            "Rest timer",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Ongoing rest countdown"
            enableVibration(false)
            setSound(null, null)
        }
        val completeSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val complete = NotificationChannel(
            COMPLETE_CHANNEL_ID,
            "Rest complete",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alert when rest time is over"
            enableVibration(true)
            setSound(
                completeSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(running)
        manager.createNotificationChannel(complete)
    }

    fun showRestRunning(
        context: Context,
        exerciseName: String,
        endsAtElapsedRealtime: Long,
        workoutId: Long,
        workoutExerciseId: Long,
    ) {
        ensureChannel(context)
        val content = restTimerRemoteViews(context, exerciseName, endsAtElapsedRealtime)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(exerciseName)
            .setContentText("Rest timer")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activeExerciseIntent(context, workoutId, workoutExerciseId))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showRestComplete(
        context: Context,
        exerciseName: String,
        workoutId: Long,
        workoutExerciseId: Long,
    ) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, COMPLETE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Rest complete")
            .setContentText("Back to $exerciseName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(activeExerciseIntent(context, workoutId, workoutExerciseId))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancelRestNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun vibrateFiveSeconds(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(
            VibrationEffect.createOneShot(5_000L, VibrationEffect.DEFAULT_AMPLITUDE),
        )
    }

    private fun restTimerRemoteViews(
        context: Context,
        exerciseName: String,
        endsAtElapsedRealtime: Long,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.notification_rest_timer).apply {
            setTextViewText(R.id.rest_timer_title, exerciseName)
            setTextViewText(R.id.rest_timer_exercise, "Rest timer")
            setChronometerCountDown(R.id.rest_timer_chrono, true)
            setChronometer(R.id.rest_timer_chrono, endsAtElapsedRealtime, null, true)
        }

    private fun activeExerciseIntent(
        context: Context,
        workoutId: Long,
        workoutExerciseId: Long,
    ): PendingIntent {
        val intent = if (workoutId > 0L && workoutExerciseId > 0L) {
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Routes.activeExerciseDeepLink(workoutId, workoutExerciseId)),
                context,
                MainActivity::class.java,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val REQUEST_CODE_CONTENT = 1001
}

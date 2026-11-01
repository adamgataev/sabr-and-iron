package com.example.sabr_and_iron.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RestTimerController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _remainingSeconds = MutableStateFlow(90)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var tickJob: Job? = null
    private var exerciseName: String = "exercise"
    private var workoutId: Long = 0L
    private var workoutExerciseId: Long = 0L
    private var endsAtElapsedRealtime: Long = 0L

    fun configureDefault(
        seconds: Int?,
        name: String,
        workoutId: Long,
        workoutExerciseId: Long,
    ) {
        exerciseName = name
        this.workoutId = workoutId
        this.workoutExerciseId = workoutExerciseId
        if (!_running.value) {
            _remainingSeconds.value = (seconds ?: 90).coerceAtLeast(0)
        } else {
            // Keep UI in sync if screen remounts while still counting down.
            _remainingSeconds.value = remainingFromEnd()
        }
    }

    fun setSeconds(seconds: Int) {
        if (!_running.value) {
            _remainingSeconds.value = seconds.coerceAtLeast(0)
        }
    }

    fun adjust(delta: Int) {
        if (!_running.value) {
            _remainingSeconds.value = (_remainingSeconds.value + delta).coerceAtLeast(0)
        }
    }

    fun start() {
        if (_running.value || _remainingSeconds.value <= 0) return
        val seconds = _remainingSeconds.value
        endsAtElapsedRealtime = SystemClock.elapsedRealtime() + seconds * 1_000L
        _running.value = true
        scheduleAlarm(seconds)
        RestTimerNotifications.showRestRunning(
            context = context,
            exerciseName = exerciseName,
            endsAtElapsedRealtime = endsAtElapsedRealtime,
            workoutId = workoutId,
            workoutExerciseId = workoutExerciseId,
        )
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = remainingFromEnd()
                _remainingSeconds.value = remaining
                if (remaining <= 0) break
                delay(250)
            }
            if (_remainingSeconds.value <= 0 && _running.value) {
                _running.value = false
                cancelAlarm()
                RestTimerNotifications.vibrateFiveSeconds(context)
                RestTimerNotifications.showRestComplete(
                    context = context,
                    exerciseName = exerciseName,
                    workoutId = workoutId,
                    workoutExerciseId = workoutExerciseId,
                )
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        val wasRunning = _running.value
        _running.value = false
        endsAtElapsedRealtime = 0L
        cancelAlarm()
        if (wasRunning) {
            RestTimerNotifications.cancelRestNotification(context)
        }
    }

    private fun remainingFromEnd(): Int {
        if (endsAtElapsedRealtime <= 0L) return _remainingSeconds.value
        val msLeft = endsAtElapsedRealtime - SystemClock.elapsedRealtime()
        return ((msLeft + 999L) / 1_000L).toInt().coerceAtLeast(0)
    }

    private fun scheduleAlarm(seconds: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            action = RestTimerReceiver.ACTION_REST_COMPLETE
            putExtra(RestTimerReceiver.EXTRA_EXERCISE_NAME, exerciseName)
            putExtra(RestTimerReceiver.EXTRA_WORKOUT_ID, workoutId)
            putExtra(RestTimerReceiver.EXTRA_WORKOUT_EXERCISE_ID, workoutExerciseId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + seconds * 1_000L,
            pending,
        )
    }

    private fun cancelAlarm() {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            action = RestTimerReceiver.ACTION_REST_COMPLETE
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    companion object {
        private const val REQUEST_CODE = 42
    }
}

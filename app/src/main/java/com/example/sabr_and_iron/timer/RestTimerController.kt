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

    fun configureDefault(seconds: Int?, name: String) {
        exerciseName = name
        if (!_running.value) {
            _remainingSeconds.value = (seconds ?: 90).coerceAtLeast(0)
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
        _running.value = true
        scheduleAlarm(_remainingSeconds.value)
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1_000)
                _remainingSeconds.value = _remainingSeconds.value - 1
            }
            if (_remainingSeconds.value <= 0) {
                _running.value = false
                cancelAlarm()
                RestTimerNotifications.vibrateFiveSeconds(context)
                RestTimerNotifications.showRestComplete(context, exerciseName)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
        _running.value = false
        cancelAlarm()
    }

    private fun scheduleAlarm(seconds: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, RestTimerReceiver::class.java).apply {
            action = RestTimerReceiver.ACTION_REST_COMPLETE
            putExtra(RestTimerReceiver.EXTRA_EXERCISE_NAME, exerciseName)
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

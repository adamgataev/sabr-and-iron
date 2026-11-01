package com.example.sabr_and_iron.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatKg(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(epochMillis))

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

fun formatYearMonth(epochMillis: Long): String =
    SimpleDateFormat("yyyy MMMM", Locale.getDefault()).format(Date(epochMillis))

fun formatChartAxisDate(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochMillis))

fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

fun formatDistanceKm(meters: Double): String {
    val km = meters / 1000.0
    return if (km >= 10.0 || km == km.toInt().toDouble()) {
        String.format(Locale.US, "%.1f km", km)
    } else {
        String.format(Locale.US, "%.2f km", km)
    }
}

fun formatPace(meters: Double, durationSeconds: Int): String? {
    if (meters <= 0.0 || durationSeconds <= 0) return null
    val secPerKm = durationSeconds / (meters / 1000.0)
    val whole = secPerKm.toInt()
    return "%d:%02d /km".format(whole / 60, whole % 60)
}

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

fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

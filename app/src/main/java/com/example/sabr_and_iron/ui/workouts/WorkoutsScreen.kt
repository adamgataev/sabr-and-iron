package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.WorkoutSummary
import com.example.sabr_and_iron.ui.components.AppCard
import com.example.sabr_and_iron.ui.components.EmptyState
import com.example.sabr_and_iron.ui.components.FloatingIslandListBottomPadding
import com.example.sabr_and_iron.ui.util.formatDateTime
import com.example.sabr_and_iron.ui.util.formatKg
import com.example.sabr_and_iron.ui.util.formatYearMonth
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@Composable
fun WorkoutsScreen(
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val monthGroups = remember(summaries) { groupWorkoutsByYearMonth(summaries) }

    when {
        summaries.isEmpty() -> {
            EmptyState(
                title = "No workouts yet. Say Bismillah and start!",
                subtitle = "Use Start New Workout when you're ready.",
                modifier = modifier,
            )
        }
        viewMode == WorkoutsViewMode.Calendar -> {
            WorkoutsCalendarView(
                summaries = summaries,
                onOpenWorkout = onOpenWorkout,
                modifier = modifier,
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = FloatingIslandListBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                monthGroups.forEach { group ->
                    stickyHeader(key = "month-${group.key}") {
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(group.workouts, key = { it.id }) { summary ->
                        WorkoutSummaryCard(
                            summary = summary,
                            onClick = { onOpenWorkout(summary.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutsCalendarView(
    summaries: List<WorkoutSummary>,
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayStart = remember { startOfDayMillis(System.currentTimeMillis()) }
    var visibleMonthStart by remember {
        mutableLongStateOf(startOfMonthMillis(System.currentTimeMillis()))
    }
    var selectedDayStart by remember { mutableLongStateOf(todayStart) }

    val workoutsByDay = remember(summaries) {
        summaries.groupBy { startOfDayMillis(it.performedAt) }
    }

    LaunchedEffect(visibleMonthStart, workoutsByDay, todayStart) {
        selectedDayStart = defaultSelectedDay(
            visibleMonthStart = visibleMonthStart,
            todayStart = todayStart,
            workoutsByDay = workoutsByDay,
        )
    }

    val selectedWorkouts = workoutsByDay[selectedDayStart]
        ?.sortedByDescending { it.performedAt }
        .orEmpty()

    val weekdayLabels = remember {
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
        val short = symbols.shortWeekdays
        val first = Calendar.getInstance().firstDayOfWeek
        (0 until 7).map { offset ->
            val calDay = ((first - Calendar.SUNDAY + offset) % 7) + Calendar.SUNDAY
            short[calDay]
        }
    }
    val days = remember(visibleMonthStart) { monthDayCells(visibleMonthStart) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = FloatingIslandListBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "calendar-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        visibleMonthStart = shiftMonth(visibleMonthStart, -1)
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                    )
                }
                Text(
                    text = formatYearMonth(visibleMonthStart),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Go to today" }
                        .clickable(role = Role.Button) {
                            visibleMonthStart = startOfMonthMillis(todayStart)
                            selectedDayStart = todayStart
                        },
                )
                IconButton(
                    onClick = {
                        visibleMonthStart = shiftMonth(visibleMonthStart, 1)
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month",
                    )
                }
            }
        }
        item(key = "weekday-labels") {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item(key = "day-grid") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                days.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        week.forEach { day ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (day != null) {
                                    CalendarDayCell(
                                        dayOfMonth = day.dayOfMonth,
                                        selected = day.dayStart == selectedDayStart,
                                        isToday = day.dayStart == todayStart,
                                        hasWorkout = workoutsByDay.containsKey(day.dayStart),
                                        onClick = { selectedDayStart = day.dayStart },
                                    )
                                } else {
                                    Spacer(modifier = Modifier.aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "day-section-label") {
            Text(
                text = "Workouts",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (selectedWorkouts.isEmpty()) {
            item(key = "empty-day") {
                Text(
                    text = "No workouts this day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(selectedWorkouts, key = { it.id }) { summary ->
                WorkoutSummaryCard(
                    summary = summary,
                    onClick = { onOpenWorkout(summary.id) },
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayOfMonth: Int,
    selected: Boolean,
    isToday: Boolean,
    hasWorkout: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.background
    }
    val fg = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (isToday && !selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .background(bg)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !hasWorkout -> MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        selected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.primary
                    },
                ),
        )
    }
}

@Composable
private fun WorkoutSummaryCard(
    summary: WorkoutSummary,
    onClick: () -> Unit,
) {
    AppCard(onClick = onClick) {
        Text(
            text = formatDateTime(summary.performedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = summary.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "${summary.exerciseCount} exercises",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        summary.bodyWeightKg?.let { bw ->
            Text(
                text = "Body weight: ${formatKg(bw)} kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private data class WorkoutMonthGroup(
    val key: String,
    val label: String,
    val workouts: List<WorkoutSummary>,
)

private data class MonthDayCell(
    val dayOfMonth: Int,
    val dayStart: Long,
)

private fun groupWorkoutsByYearMonth(summaries: List<WorkoutSummary>): List<WorkoutMonthGroup> {
    val calendar = Calendar.getInstance()
    return summaries
        .groupBy { summary ->
            calendar.timeInMillis = summary.performedAt
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            "$year-$month"
        }
        .entries
        .sortedByDescending { (_, workouts) -> workouts.maxOf { it.performedAt } }
        .map { (key, workouts) ->
            val ordered = workouts.sortedByDescending { it.performedAt }
            WorkoutMonthGroup(
                key = key,
                label = formatYearMonth(ordered.first().performedAt),
                workouts = ordered,
            )
        }
}

private fun startOfDayMillis(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun startOfMonthMillis(epochMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun shiftMonth(monthStart: Long, delta: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = monthStart
        add(Calendar.MONTH, delta)
    }
    return startOfMonthMillis(cal.timeInMillis)
}

private fun monthDayCells(monthStart: Long): List<MonthDayCell?> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val firstDayOfWeek = cal.firstDayOfWeek
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK)
    val leadingBlanks = (firstWeekday - firstDayOfWeek + 7) % 7
    val cells = MutableList<MonthDayCell?>(leadingBlanks) { null }
    repeat(daysInMonth) { index ->
        cal.timeInMillis = monthStart
        cal.set(Calendar.DAY_OF_MONTH, index + 1)
        cells += MonthDayCell(
            dayOfMonth = index + 1,
            dayStart = startOfDayMillis(cal.timeInMillis),
        )
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    return cells
}

private fun defaultSelectedDay(
    visibleMonthStart: Long,
    todayStart: Long,
    workoutsByDay: Map<Long, List<WorkoutSummary>>,
): Long {
    val monthEnd = shiftMonth(visibleMonthStart, 1)
    val todayInMonth = todayStart in visibleMonthStart until monthEnd
    if (todayInMonth) return todayStart
    val daysWithWorkouts = workoutsByDay.keys
        .filter { it in visibleMonthStart until monthEnd }
        .sortedDescending()
    return daysWithWorkouts.firstOrNull() ?: todayStart
}

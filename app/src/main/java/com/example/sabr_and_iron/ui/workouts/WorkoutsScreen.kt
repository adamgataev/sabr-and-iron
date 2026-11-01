package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import java.util.Calendar

@Composable
fun WorkoutsScreen(
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    val monthGroups = remember(summaries) { groupWorkoutsByYearMonth(summaries) }

    if (summaries.isEmpty()) {
        EmptyState(
            title = "No workouts yet. Say Bismillah and start!",
            subtitle = "Use Start New Workout when you're ready.",
            modifier = modifier,
        )
    } else {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(group.workouts, key = { it.id }) { summary ->
                    AppCard(onClick = { onOpenWorkout(summary.id) }) {
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
            }
        }
    }
}

private data class WorkoutMonthGroup(
    val key: String,
    val label: String,
    val workouts: List<WorkoutSummary>,
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

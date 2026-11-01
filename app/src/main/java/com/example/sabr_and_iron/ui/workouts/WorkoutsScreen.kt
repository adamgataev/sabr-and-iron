package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.ui.components.AppCard
import com.example.sabr_and_iron.ui.components.EmptyState
import com.example.sabr_and_iron.ui.components.FloatingIslandListBottomPadding
import com.example.sabr_and_iron.ui.util.formatDateTime
import com.example.sabr_and_iron.ui.util.formatKg

@Composable
fun WorkoutsScreen(
    onOpenWorkout: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

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
            items(summaries, key = { it.id }) { summary ->
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

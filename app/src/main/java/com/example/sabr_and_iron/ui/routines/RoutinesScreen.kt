package com.example.sabr_and_iron.ui.routines

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

@Composable
fun RoutinesScreen(
    onOpenRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = viewModel(),
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()

    if (routines.isEmpty()) {
        EmptyState(
            title = "No routines yet.",
            subtitle = "Reusable templates will live here.",
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
            items(routines, key = { it.id }) { routine ->
                AppCard(onClick = { onOpenRoutine(routine.id) }) {
                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!routine.inspiredBy.isNullOrBlank()) {
                        Text(
                            text = "Inspired by ${routine.inspiredBy}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

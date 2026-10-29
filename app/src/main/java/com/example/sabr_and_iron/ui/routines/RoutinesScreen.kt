package com.example.sabr_and_iron.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.ui.components.EmptyState

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
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(routines, key = { it.id }) { routine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRoutine(routine.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium)
                        if (!routine.inspiredBy.isNullOrBlank()) {
                            Text(
                                text = "Inspired by ${routine.inspiredBy}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

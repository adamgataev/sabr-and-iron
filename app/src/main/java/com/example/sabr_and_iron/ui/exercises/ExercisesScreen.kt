package com.example.sabr_and_iron.ui.exercises

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
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.ui.components.AppCard
import com.example.sabr_and_iron.ui.components.FloatingIslandListBottomPadding
import com.example.sabr_and_iron.ui.util.formatKg

@Composable
fun ExercisesScreen(
    onOpenExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(),
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()

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
        items(exercises, key = { it.id }) { exercise ->
            ExerciseListItem(
                exercise = exercise,
                onClick = { onOpenExercise(exercise.id) },
            )
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = exercise.targetedMuscles.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = exercise.exerciseType.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        val resistance = exercise.startingResistanceKg
        if (resistance != null && exercise.exerciseType.usesStartingResistance) {
            Text(
                text = "Starting resistance: ${formatKg(resistance)} kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

package com.example.sabr_and_iron.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.theme.color
import com.example.sabr_and_iron.ui.util.formatKg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onDeleted: () -> Unit,
    deleteTick: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(),
) {
    val exerciseFlow = remember(exerciseId) { viewModel.observeExercise(exerciseId) }
    val exercise by exerciseFlow.collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleteTick) {
        if (deleteTick > 0 && exercise != null) {
            confirmDelete = true
        }
    }

    val current = exercise
    if (current == null) {
        Text(
            text = "Loading…",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = current.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val machine = current.machine?.takeIf { it.isNotBlank() }
        if (machine != null) {
            Text(
                text = machine,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (current.targetedMuscles.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                current.targetedMuscles.forEach { muscle ->
                    Text(
                        text = muscle.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = muscle.color,
                    )
                }
            }
        }
        Text(
            text = current.exerciseType.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val resistance = current.startingResistanceKg
        if (resistance != null && current.exerciseType.usesStartingResistance) {
            Text(
                text = "Starting resistance: ${formatKg(resistance)} kg",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        current.defaultRestSeconds?.let { seconds ->
            Text(
                text = "Default rest: ${seconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete exercise?",
            body = "Remove \"${current.name}\"? This cannot be undone.",
            onConfirm = {
                confirmDelete = false
                viewModel.delete(current, onDeleted)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

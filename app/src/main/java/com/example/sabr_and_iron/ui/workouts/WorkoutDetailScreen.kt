package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.ui.util.formatDateTime
import com.example.sabr_and_iron.ui.util.formatKg

@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    onOpenExercise: (workoutExerciseId: Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val detail by viewModel.observeDetail(workoutId).collectAsStateWithLifecycle(initialValue = null)
    val library by viewModel.observeExercises().collectAsStateWithLifecycle(emptyList())
    var showPicker by remember { mutableStateOf(false) }
    var editingMeta by remember { mutableStateOf(false) }

    val workout = detail?.workout
    if (workout == null) {
        Text("Loading…", modifier = modifier.padding(16.dp))
        return
    }

    var name by remember(workout.id, workout.name) { mutableStateOf(workout.name) }
    var bodyWeight by remember(workout.id, workout.bodyWeightKg) {
        mutableStateOf(workout.bodyWeightKg?.toString().orEmpty())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(formatDateTime(workout.performedAt), style = MaterialTheme.typography.labelLarge)
        if (editingMeta) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bodyWeight,
                onValueChange = { bodyWeight = it },
                label = { Text("Body weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Button(
                onClick = {
                    viewModel.updateMeta(
                        workout.copy(
                            name = name.trim(),
                            bodyWeightKg = bodyWeight.toDoubleOrNull(),
                        ),
                    ) { editingMeta = false }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save details") }
        } else {
            Text(workout.name, style = MaterialTheme.typography.headlineSmall)
            workout.bodyWeightKg?.let {
                Text(
                    "Body weight: ${formatKg(it)} kg",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            OutlinedButton(onClick = { editingMeta = true }) { Text("Edit details") }
        }

        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add exercise") }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(detail?.exercises.orEmpty(), key = { it.workoutExercise.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenExercise(item.workoutExercise.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(item.exerciseName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${item.sets.size} sets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = { viewModel.deleteWorkout(workout, onDeleted) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Delete workout", color = MaterialTheme.colorScheme.error)
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Add exercise") },
            text = {
                LazyColumn {
                    items(library, key = { it.id }) { exercise ->
                        TextButton(
                            onClick = {
                                viewModel.addExercise(workoutId, exercise.id) {
                                    showPicker = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(exercise.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Close") }
            },
        )
    }
}

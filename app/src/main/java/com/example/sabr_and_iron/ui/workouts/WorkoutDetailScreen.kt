package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.WorkoutExerciseWithSets
import com.example.sabr_and_iron.ui.components.AppCard
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.components.ReorderableExerciseRow
import com.example.sabr_and_iron.ui.util.formatDateTime
import com.example.sabr_and_iron.ui.util.formatKg
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    editTick: Int,
    deleteTick: Int = 0,
    onEditingChange: (Boolean) -> Unit,
    onOpenExercise: (workoutExerciseId: Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val detail by viewModel.observeDetail(workoutId).collectAsStateWithLifecycle(initialValue = null)
    val library by viewModel.observeExercises().collectAsStateWithLifecycle(emptyList())
    val routines by viewModel.observeRoutines().collectAsStateWithLifecycle(emptyList())
    val message by viewModel.message.collectAsStateWithLifecycle()
    var showExercisePicker by remember { mutableStateOf(false) }
    var showRoutinePicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf<WorkoutExerciseWithSets?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val editOrder = remember { mutableStateListOf<WorkoutExerciseWithSets>() }
    val presentExerciseIds = remember(detail?.exercises) {
        detail?.exercises.orEmpty().map { it.workoutExercise.exerciseId }.toSet()
    }
    val availableExercises = remember(library, presentExerciseIds) {
        library.filter { it.id !in presentExerciseIds }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(deleteTick) {
        if (deleteTick > 0) {
            confirmDelete = true
        }
    }

    val workout = detail?.workout
    if (workout == null) {
        Text("Loading…", modifier = modifier.padding(16.dp))
        return
    }

    var name by remember(workout.id, workout.name) { mutableStateOf(workout.name) }
    var bodyWeight by remember(workout.id, workout.bodyWeightKg) {
        mutableStateOf(workout.bodyWeightKg?.toString().orEmpty())
    }

    LaunchedEffect(detail?.exercises, editing) {
        if (editing) {
            val currentIds = editOrder.map { it.workoutExercise.id }.toSet()
            val incoming = detail?.exercises.orEmpty()
            if (currentIds != incoming.map { it.workoutExercise.id }.toSet()) {
                editOrder.clear()
                editOrder.addAll(incoming)
            }
        }
    }

    LaunchedEffect(editTick) {
        if (editTick == 0) return@LaunchedEffect
        if (!editing) {
            name = workout.name
            bodyWeight = workout.bodyWeightKg?.toString().orEmpty()
            editOrder.clear()
            editOrder.addAll(detail?.exercises.orEmpty())
            editing = true
            onEditingChange(true)
        } else {
            viewModel.updateMeta(
                workout.copy(
                    name = name.trim().ifBlank { workout.name },
                    bodyWeightKg = bodyWeight.toDoubleOrNull(),
                ),
            )
            viewModel.reorderWorkoutExercises(editOrder.map { it.workoutExercise })
            editing = false
            onEditingChange(false)
        }
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        editOrder.add(to.index, editOrder.removeAt(from.index))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            Text(formatDateTime(workout.performedAt), style = MaterialTheme.typography.labelLarge)
            if (editing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = bodyWeight,
                    onValueChange = { bodyWeight = it },
                    label = { Text("Body weight (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            } else {
                Text(workout.name, style = MaterialTheme.typography.headlineSmall)
                workout.bodyWeightKg?.let {
                    Text(
                        "Body weight: ${formatKg(it)} kg",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (!editing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showRoutinePicker = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Add routine") }
                    OutlinedButton(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier.weight(1f),
                        enabled = availableExercises.isNotEmpty(),
                    ) {
                        Text(
                            if (availableExercises.isEmpty() && library.isNotEmpty()) {
                                "All added"
                            } else {
                                "Add exercise"
                            },
                        )
                    }
                }
            }

            if (editing) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(editOrder, key = { it.workoutExercise.id }) { item ->
                        ReorderableItem(
                            reorderableState,
                            key = item.workoutExercise.id,
                        ) { isDragging ->
                            SideEffect {
                                if (isDragging) draggingId = item.workoutExercise.id
                                else if (draggingId == item.workoutExercise.id) draggingId = null
                            }
                            val canRemove = item.sets.isEmpty()
                            ReorderableExerciseRow(
                                name = item.exerciseName,
                                isDragging = isDragging,
                                dimmed = draggingId != null && !isDragging,
                                onRemove = {
                                    if (canRemove) {
                                        confirmRemove = item
                                    } else {
                                        scope.launch {
                                            snackbar.showSnackbar(
                                                "Remove all sets from this exercise before removing it from the workout.",
                                            )
                                        }
                                    }
                                },
                                removeEnabled = canRemove,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(detail?.exercises.orEmpty(), key = { it.workoutExercise.id }) { item ->
                        AppCard(onClick = { onOpenExercise(item.workoutExercise.id) }) {
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

    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete workout?",
            body = "This permanently deletes the workout and all of its sets. Personal records update automatically.",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteWorkout(workout, onDeleted)
            },
            onDismiss = { confirmDelete = false },
        )
    }

    confirmRemove?.let { item ->
        ConfirmDeleteDialog(
            title = "Remove exercise?",
            body = "Remove ${item.exerciseName} from this workout?",
            confirmLabel = "Remove",
            onConfirm = {
                confirmRemove = null
                viewModel.deleteWorkoutExercise(item.workoutExercise) {
                    editOrder.removeAll { it.workoutExercise.id == item.workoutExercise.id }
                }
            },
            onDismiss = { confirmRemove = null },
        )
    }

    if (showExercisePicker) {
        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            title = { Text("Add exercise") },
            text = {
                if (availableExercises.isEmpty()) {
                    Text(
                        "Every exercise is already in this workout.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(availableExercises, key = { it.id }) { exercise ->
                            TextButton(
                                onClick = {
                                    viewModel.addExercise(workoutId, exercise.id) {
                                        showExercisePicker = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(exercise.name) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExercisePicker = false }) { Text("Close") }
            },
        )
    }

    if (showRoutinePicker) {
        AlertDialog(
            onDismissRequest = { showRoutinePicker = false },
            title = { Text("Add routine") },
            text = {
                LazyColumn {
                    items(routines, key = { it.id }) { routine ->
                        TextButton(
                            onClick = {
                                viewModel.addRoutineExercises(workoutId, routine.id) {
                                    showRoutinePicker = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(routine.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoutinePicker = false }) { Text("Close") }
            },
        )
    }
}

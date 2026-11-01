package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.ui.components.FloatingIslandTopClearance
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.components.ReorderableExerciseRow
import com.example.sabr_and_iron.ui.util.formatDateTime
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFormScreen(
    onSaved: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val library by viewModel.observeExercises().collectAsStateWithLifecycle(emptyList())
    val routines by viewModel.observeRoutines().collectAsStateWithLifecycle(emptyList())
    val message by viewModel.message.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = LocalAppSnackbar.current

    var name by remember { mutableStateOf("") }
    var performedAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var bodyWeight by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<ExerciseEntity>() }
    var showExercisePicker by remember { mutableStateOf(false) }
    var showRoutinePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    val presentIds = selected.map { it.id }.toSet()
    val availableExercises = library.filter { it.id !in presentIds }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        selected.add(to.index, selected.removeAt(from.index))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Workout name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(formatDateTime(performedAt))
        }
        OutlinedTextField(
            value = bodyWeight,
            onValueChange = { bodyWeight = it },
            label = { Text("Body weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            supportingText = {
                Text("Used for weighted / assisted bodyweight exercises")
            },
        )
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
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(selected, key = { it.id }) { exercise ->
                ReorderableItem(reorderableState, key = exercise.id) { isDragging ->
                    SideEffect {
                        if (isDragging) draggingId = exercise.id
                        else if (draggingId == exercise.id) draggingId = null
                    }
                    ReorderableExerciseRow(
                        name = exercise.name,
                        isDragging = isDragging,
                        dimmed = draggingId != null && !isDragging,
                        onRemove = { selected.remove(exercise) },
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingIslandTopClearance),
        ) {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    viewModel.createWorkout(
                        name = name.trim(),
                        performedAt = performedAt,
                        bodyWeightKg = bodyWeight.toDoubleOrNull(),
                        exerciseIds = selected.map { it.id },
                        onDone = onSaved,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                enabled = name.isNotBlank(),
            ) {
                Text("Save workout")
            }
        }
    }

    if (showExercisePicker) {
        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            title = { Text("Select exercise") },
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
                                    selected.add(exercise)
                                    showExercisePicker = false
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
            title = { Text("Select routine") },
            text = {
                LazyColumn {
                    items(routines, key = { it.id }) { routine ->
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val ids = viewModel.routineExerciseIds(routine.id)
                                    val byId = library.associateBy { it.id }
                                    val presentIds = selected.map { it.id }.toSet()
                                    val fromRoutine = ids.mapNotNull { byId[it] }
                                    val skipped = fromRoutine.filter { it.id in presentIds }
                                    val toAdd = fromRoutine.filter { it.id !in presentIds }
                                    toAdd.forEach { selected.add(it) }
                                    showRoutinePicker = false
                                    viewModel.showRoutineDuplicates(skipped.map { it.name })
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

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = performedAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { dateMillis ->
                            val cal = Calendar.getInstance().apply { timeInMillis = performedAt }
                            val picked = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            cal.set(Calendar.YEAR, picked.get(Calendar.YEAR))
                            cal.set(Calendar.MONTH, picked.get(Calendar.MONTH))
                            cal.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
                            performedAt = cal.timeInMillis
                        }
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = performedAt }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        cal.set(Calendar.HOUR_OF_DAY, state.hour)
                        cal.set(Calendar.MINUTE, state.minute)
                        performedAt = cal.timeInMillis
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = state) },
        )
    }
}

package com.example.sabr_and_iron.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.components.FloatingIslandTopClearance
import com.example.sabr_and_iron.ui.components.ReorderableExerciseRow
import com.example.sabr_and_iron.ui.theme.ErrorVibrant
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun RoutineEditorScreen(
    routineId: Long?,
    onDone: () -> Unit,
    deleteTick: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = viewModel(),
) {
    val detailFlow = remember(routineId) {
        if (routineId != null && routineId > 0) viewModel.observeDetail(routineId) else null
    }
    val detail by detailFlow?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf(null) }
    val library by viewModel.observeExercises().collectAsStateWithLifecycle(emptyList())

    var name by remember { mutableStateOf("") }
    var inspiredBy by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<ExerciseEntity>() }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var nameTaken by remember { mutableStateOf(false) }
    var checkingName by remember { mutableStateOf(false) }
    val presentIds = selected.map { it.id }.toSet()
    val availableExercises = library.filter { it.id !in presentIds }

    LaunchedEffect(detail) {
        detail?.let {
            name = it.routine.name
            inspiredBy = it.routine.inspiredBy.orEmpty()
            selected.clear()
            selected.addAll(it.exercises.map { item -> item.exercise })
            nameTaken = false
        }
    }

    LaunchedEffect(deleteTick) {
        if (deleteTick > 0 && detail != null) {
            confirmDelete = true
        }
    }

    LaunchedEffect(name, detail?.routine?.id, routineId) {
        val trimmed = name.trim()
        nameTaken = false
        if (trimmed.isEmpty()) {
            checkingName = false
            return@LaunchedEffect
        }
        checkingName = true
        delay(450)
        val excludeId = detail?.routine?.id ?: routineId?.takeIf { it > 0 } ?: 0L
        nameTaken = viewModel.isNameTaken(trimmed, excludingId = excludeId)
        checkingName = false
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
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameTaken,
            supportingText = if (nameTaken) {
                {
                    Text(
                        text = "You've already used this name for a routine",
                        color = ErrorVibrant,
                    )
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                errorBorderColor = ErrorVibrant,
                errorLabelColor = ErrorVibrant,
                errorCursorColor = ErrorVibrant,
                errorSupportingTextColor = ErrorVibrant,
            ),
        )
        OutlinedTextField(
            value = inspiredBy,
            onValueChange = { inspiredBy = it },
            label = { Text("Inspired by (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = availableExercises.isNotEmpty(),
        ) {
            Text(
                if (availableExercises.isEmpty() && library.isNotEmpty()) {
                    "All exercises added"
                } else {
                    "Add exercise"
                },
            )
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = {
                        if (name.isBlank() || nameTaken || checkingName) return@Button
                        viewModel.save(
                            id = routineId ?: 0L,
                            name = name,
                            inspiredBy = inspiredBy,
                            exerciseIds = selected.map { it.id },
                            onDone = { onDone() },
                            onError = { nameTaken = true },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && !nameTaken && !checkingName,
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete routine?",
            body = "This permanently removes the routine. Workouts already logged are not affected.",
            onConfirm = {
                confirmDelete = false
                detail?.routine?.let { viewModel.delete(it, onDone) }
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select exercise") },
            text = {
                if (availableExercises.isEmpty()) {
                    Text(
                        "Every exercise is already in this routine.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn {
                        items(availableExercises, key = { it.id }) { exercise ->
                            TextButton(
                                onClick = {
                                    selected.add(exercise)
                                    showPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(exercise.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Close") }
            },
        )
    }
}

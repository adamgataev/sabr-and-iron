package com.example.sabr_and_iron.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun RoutineEditorScreen(
    routineId: Long?,
    onDone: () -> Unit,
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

    LaunchedEffect(detail) {
        detail?.let {
            name = it.routine.name
            inspiredBy = it.routine.inspiredBy.orEmpty()
            selected.clear()
            selected.addAll(it.exercises.map { item -> item.exercise })
        }
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        selected.add(to.index, selected.removeAt(from.index))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
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
        ) {
            Text("Add exercise")
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(selected, key = { it.id to selected.indexOf(it) }) { exercise ->
                ReorderableItem(reorderableState, key = exercise.id to selected.indexOf(exercise)) { _ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = "Reorder",
                            modifier = Modifier.draggableHandle(),
                        )
                        Text(
                            text = exercise.name,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        IconButton(onClick = { selected.remove(exercise) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                if (name.isBlank()) return@Button
                viewModel.save(
                    id = routineId ?: 0L,
                    name = name,
                    inspiredBy = inspiredBy,
                    exerciseIds = selected.map { it.id },
                    onDone = { onDone() },
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank(),
        ) {
            Text("Save")
        }
        if (detail != null) {
            TextButton(
                onClick = { detail?.routine?.let { viewModel.delete(it, onDone) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select exercise") },
            text = {
                LazyColumn {
                    items(library, key = { it.id }) { exercise ->
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
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Close") }
            },
        )
    }
}

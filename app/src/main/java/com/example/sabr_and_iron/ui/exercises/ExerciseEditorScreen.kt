package com.example.sabr_and_iron.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorScreen(
    exerciseId: Long?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(),
) {
    val existingFlow = remember(exerciseId) {
        if (exerciseId != null && exerciseId > 0) {
            viewModel.observeExercise(exerciseId)
        } else {
            flowOf(null)
        }
    }
    val existing by existingFlow.collectAsStateWithLifecycle(initialValue = null)

    var name by remember { mutableStateOf("") }
    var muscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var type by remember { mutableStateOf(ExerciseType.Regular) }
    var starting by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        existing?.let { exercise ->
            name = exercise.name
            muscles = exercise.targetedMuscles.toSet()
            type = exercise.exerciseType
            starting = exercise.startingResistanceKg?.toString().orEmpty()
            rest = exercise.defaultRestSeconds?.toString().orEmpty()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
        Text("Targeted muscles", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuscleGroup.entries.forEach { group ->
                FilterChip(
                    selected = group in muscles,
                    onClick = {
                        muscles = if (group in muscles) muscles - group else muscles + group
                    },
                    label = { Text(group.name) },
                )
            }
        }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it },
        ) {
            OutlinedTextField(
                value = type.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Exercise type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false },
            ) {
                ExerciseType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            type = option
                            typeExpanded = false
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = starting,
            onValueChange = { starting = it },
            label = { Text("Starting resistance (kg)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        OutlinedTextField(
            value = rest,
            onValueChange = { rest = it },
            label = { Text("Default rest (seconds)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Button(
            onClick = {
                if (name.isBlank() || muscles.isEmpty()) return@Button
                viewModel.save(
                    id = exerciseId ?: 0L,
                    name = name,
                    muscles = muscles.toList(),
                    type = type,
                    startingResistanceKg = starting.toDoubleOrNull(),
                    defaultRestSeconds = rest.toIntOrNull(),
                    onDone = { onDone() },
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && muscles.isNotEmpty(),
        ) {
            Text("Save")
        }
        if (existing != null) {
            TextButton(
                onClick = {
                    existing?.let { viewModel.delete(it, onDone) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

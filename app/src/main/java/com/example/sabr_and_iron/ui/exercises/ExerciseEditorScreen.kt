package com.example.sabr_and_iron.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.theme.ErrorVibrant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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
    val typeLocked = existing != null

    var name by remember { mutableStateOf("") }
    var muscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var type by remember { mutableStateOf(ExerciseType.Regular) }
    var starting by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var nameTaken by remember { mutableStateOf(false) }
    var checkingName by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(existing) {
        existing?.let { exercise ->
            name = exercise.name
            muscles = exercise.targetedMuscles.toSet()
            type = exercise.exerciseType
            starting = exercise.startingResistanceKg?.toString().orEmpty()
            rest = exercise.defaultRestSeconds?.toString().orEmpty()
            nameTaken = false
        }
    }

    LaunchedEffect(name, existing?.id, exerciseId) {
        val trimmed = name.trim()
        nameTaken = false
        if (trimmed.isEmpty()) {
            checkingName = false
            return@LaunchedEffect
        }
        checkingName = true
        delay(450)
        val excludeId = existing?.id ?: exerciseId?.takeIf { it > 0 } ?: 0L
        nameTaken = viewModel.isNameTaken(trimmed, excludingId = excludeId)
        checkingName = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
            isError = nameTaken,
            supportingText = if (nameTaken) {
                {
                    Text(
                        text = "You've already used this name for an exercise",
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
                errorTrailingIconColor = ErrorVibrant,
            ),
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
            expanded = typeExpanded && !typeLocked,
            onExpandedChange = { expanded ->
                if (typeLocked) {
                    scope.launch {
                        snackbar.showSnackbar("This field cannot be changed after creation.")
                    }
                } else {
                    typeExpanded = expanded
                }
            },
        ) {
            OutlinedTextField(
                value = type.label,
                onValueChange = {},
                readOnly = true,
                enabled = !typeLocked,
                label = { Text("Exercise type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                supportingText = if (typeLocked) {
                    { Text("This field cannot be changed after creation.") }
                } else {
                    null
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            if (!typeLocked) {
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
        }
        if (type.usesStartingResistance) {
            OutlinedTextField(
                value = starting,
                onValueChange = { starting = it },
                label = { Text("Starting resistance (kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                supportingText = {
                    Text("e.g. barbell weight before plates")
                },
            )
        } else if (type.usesBodyWeight) {
            Text(
                text = when (type) {
                    ExerciseType.WeightedBodyweight ->
                        "Effective load = workout body weight + added weight."
                    ExerciseType.Assisted ->
                        "Effective load = workout body weight − assistance."
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (type.usesDistance) {
            Text(
                text = "Log distance (km) and duration in workouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (type.usesDuration) {
            Text(
                text = "Log hold / effort duration in workouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                if (name.isBlank() || muscles.isEmpty() || nameTaken || checkingName) return@Button
                viewModel.save(
                    id = exerciseId ?: 0L,
                    name = name,
                    muscles = muscles.toList(),
                    type = type,
                    startingResistanceKg = starting.toDoubleOrNull()
                        .takeIf { type.usesStartingResistance },
                    defaultRestSeconds = rest.toIntOrNull(),
                    onDone = { onDone() },
                    onError = { nameTaken = true },
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && muscles.isNotEmpty() && !nameTaken && !checkingName,
        ) {
            Text("Save")
        }
        if (existing != null) {
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete exercise?",
            body = "This removes the exercise from routines and workout history references may be affected.",
            onConfirm = {
                confirmDelete = false
                existing?.let { viewModel.delete(it, onDone) }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

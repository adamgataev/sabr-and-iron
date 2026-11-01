package com.example.sabr_and_iron.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.MuscleGroup
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.components.FloatingIslandTopClearance
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.theme.ErrorVibrant
import com.example.sabr_and_iron.ui.theme.color
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorScreen(
    exerciseId: Long?,
    onDone: () -> Unit,
    deleteTick: Int = 0,
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
    var machine by remember { mutableStateOf("") }
    var muscles by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var type by remember { mutableStateOf(ExerciseType.Regular) }
    var starting by remember { mutableStateOf("") }
    var rest by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var duplicateKey by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(existing) {
        existing?.let { exercise ->
            name = exercise.name
            machine = exercise.machine.orEmpty()
            muscles = exercise.targetedMuscles.toSet()
            type = exercise.exerciseType
            starting = exercise.startingResistanceKg?.toString().orEmpty()
            rest = exercise.defaultRestSeconds?.toString().orEmpty()
            duplicateKey = false
        }
    }

    LaunchedEffect(deleteTick) {
        if (deleteTick > 0 && existing != null) {
            confirmDelete = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    duplicateKey = false
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = duplicateKey,
                supportingText = if (duplicateKey) {
                    {
                        Text(
                            text = "You've already used this name and machine for an exercise",
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
            OutlinedTextField(
                value = machine,
                onValueChange = {
                    machine = it
                    duplicateKey = false
                },
                label = { Text("Machine (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = duplicateKey,
                colors = OutlinedTextFieldDefaults.colors(
                    errorBorderColor = ErrorVibrant,
                    errorLabelColor = ErrorVibrant,
                    errorCursorColor = ErrorVibrant,
                    errorTrailingIconColor = ErrorVibrant,
                ),
            )
            Text("Targeted muscles", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MuscleGroup.entries.forEach { group ->
                    val selected = group in muscles
                    val muscleColor = group.color
                    FilterChip(
                        selected = selected,
                        onClick = {
                            muscles = if (selected) muscles - group else muscles + group
                        },
                        label = { Text(group.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = muscleColor.copy(alpha = 0.28f),
                            selectedLabelColor = muscleColor,
                            selectedLeadingIconColor = muscleColor,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = muscleColor,
                            disabledBorderColor = Color.Transparent,
                            disabledSelectedBorderColor = Color.Transparent,
                            selectedBorderWidth = 1.5.dp,
                        ),
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
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingIslandTopClearance),
        ) {
            Button(
                onClick = {
                    if (name.isBlank() || muscles.isEmpty()) return@Button
                    viewModel.save(
                        id = exerciseId ?: 0L,
                        name = name,
                        machine = machine,
                        muscles = muscles.toList(),
                        type = type,
                        startingResistanceKg = starting.toDoubleOrNull()
                            .takeIf { type.usesStartingResistance },
                        defaultRestSeconds = rest.toIntOrNull(),
                        onDone = { onDone() },
                        onError = { duplicateKey = true },
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                enabled = name.isNotBlank() && muscles.isNotEmpty(),
            ) {
                Text("Save")
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

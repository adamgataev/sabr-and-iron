package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import com.example.sabr_and_iron.data.local.model.effectiveWeightKg
import com.example.sabr_and_iron.timer.RestTimerController
import com.example.sabr_and_iron.ui.util.formatDateTime
import com.example.sabr_and_iron.ui.util.formatKg
import com.example.sabr_and_iron.ui.util.formatTimer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveExerciseScreen(
    workoutExerciseId: Long,
    modifier: Modifier = Modifier,
    viewModel: WorkoutsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workoutExercise by viewModel.observeWorkoutExercise(workoutExerciseId)
        .collectAsStateWithLifecycle(initialValue = null)
    val sets by viewModel.observeSets(workoutExerciseId)
        .collectAsStateWithLifecycle(emptyList())

    var exerciseName by remember { mutableStateOf("Exercise") }
    var defaultRest by remember { mutableStateOf<Int?>(90) }

    LaunchedEffect(workoutExercise?.exerciseId) {
        val exerciseId = workoutExercise?.exerciseId ?: return@LaunchedEffect
        val exercise = viewModel.getExercise(exerciseId)
        exerciseName = exercise?.name ?: "Exercise"
        defaultRest = exercise?.defaultRestSeconds
    }

    val historyExerciseId = workoutExercise?.exerciseId ?: -1L
    val history by viewModel.observeHistory(historyExerciseId, workoutExerciseId)
        .collectAsStateWithLifecycle(emptyList())

    val timer = remember {
        RestTimerController(context.applicationContext, scope)
    }
    DisposableEffect(Unit) {
        onDispose { timer.stop() }
    }
    LaunchedEffect(exerciseName, defaultRest) {
        timer.configureDefault(defaultRest, exerciseName)
    }
    val remaining by timer.remainingSeconds.collectAsStateWithLifecycle()
    val running by timer.running.collectAsStateWithLifecycle()

    var addedWeight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(SetDifficulty.Ideal) }
    var difficultyExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Log") },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("History") },
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            if (page == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(exerciseName, style = MaterialTheme.typography.headlineSmall)
                    val start = workoutExercise?.startingResistanceKg
                    Text(
                        text = if (start != null) {
                            "Starting resistance: ${formatKg(start)} kg"
                        } else {
                            "Starting resistance: —"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = addedWeight,
                        onValueChange = { addedWeight = it },
                        label = { Text("Added weight / plates (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    val preview = addedWeight.toDoubleOrNull()?.let {
                        effectiveWeightKg(start, it)
                    }
                    if (preview != null) {
                        Text(
                            "Effective total: ${formatKg(preview)} kg",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    ExposedDropdownMenuBox(
                        expanded = difficultyExpanded,
                        onExpandedChange = { difficultyExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = difficulty.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Difficulty") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(difficultyExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = difficultyExpanded,
                            onDismissRequest = { difficultyExpanded = false },
                        ) {
                            SetDifficulty.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        difficulty = option
                                        difficultyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            val weight = addedWeight.toDoubleOrNull() ?: return@Button
                            val repCount = reps.toIntOrNull() ?: return@Button
                            viewModel.addSet(
                                workoutExerciseId = workoutExerciseId,
                                addedWeightKg = weight,
                                reps = repCount,
                                difficulty = difficulty,
                            ) {
                                addedWeight = ""
                                reps = ""
                                if (!running) timer.start()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = addedWeight.toDoubleOrNull() != null && reps.toIntOrNull() != null,
                    ) { Text("Add set") }

                    Text("Rest timer", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatTimer(remaining),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { timer.adjust(-15) }, enabled = !running) {
                            Text("-15s")
                        }
                        OutlinedButton(onClick = { timer.adjust(15) }, enabled = !running) {
                            Text("+15s")
                        }
                        if (running) {
                            Button(onClick = { timer.stop() }) { Text("Stop") }
                        } else {
                            Button(onClick = { timer.start() }, enabled = remaining > 0) {
                                Text("Start")
                            }
                        }
                    }

                    Text("Sets", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(sets, key = { it.id }) { set ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val effective = effectiveWeightKg(
                                    workoutExercise?.startingResistanceKg,
                                    set.addedWeightKg,
                                )
                                Text(
                                    "${formatKg(set.addedWeightKg)} kg (+${formatKg(effective)} total) × ${set.reps} · ${set.difficulty.label}",
                                )
                                TextButton(onClick = { viewModel.deleteSet(set) }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (history.isEmpty()) {
                        item {
                            Text(
                                "No previous sets for this exercise yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(history.size) { index ->
                        val entry = history[index]
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                formatDateTime(entry.performedAt),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                "${entry.workoutName}: ${formatKg(entry.totalWeightKg)} kg × ${entry.reps} (${entry.difficulty.label})",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

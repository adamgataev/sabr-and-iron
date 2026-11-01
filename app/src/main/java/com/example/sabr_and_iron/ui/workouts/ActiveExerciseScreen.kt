package com.example.sabr_and_iron.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sabr_and_iron.data.local.entity.ExerciseEntity
import com.example.sabr_and_iron.data.local.entity.SetEntity
import com.example.sabr_and_iron.data.local.model.ExerciseHistoryEntry
import com.example.sabr_and_iron.data.local.model.ExerciseType
import com.example.sabr_and_iron.data.local.model.SetDifficulty
import com.example.sabr_and_iron.data.local.model.effectiveLoadKg
import com.example.sabr_and_iron.timer.RestTimerController
import com.example.sabr_and_iron.ui.components.AppCard
import com.example.sabr_and_iron.ui.components.ConfirmDeleteDialog
import com.example.sabr_and_iron.ui.util.formatDate
import com.example.sabr_and_iron.ui.util.formatDistanceKm
import com.example.sabr_and_iron.ui.util.formatDuration
import com.example.sabr_and_iron.ui.util.formatKg
import com.example.sabr_and_iron.ui.util.formatPace
import com.example.sabr_and_iron.ui.util.formatTimer
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private data class HistoryWorkoutGroup(
    val performedAt: Long,
    val workoutName: String,
    val entries: List<ExerciseHistoryEntry>,
)

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

    val workoutId = workoutExercise?.workoutId
    val workoutFlow = remember(workoutId) {
        if (workoutId != null) viewModel.observeWorkout(workoutId) else flowOf(null)
    }
    val workout by workoutFlow.collectAsStateWithLifecycle(initialValue = null)

    var exercise by remember { mutableStateOf<ExerciseEntity?>(null) }

    LaunchedEffect(workoutExercise?.exerciseId) {
        val exerciseId = workoutExercise?.exerciseId ?: return@LaunchedEffect
        exercise = viewModel.getExercise(exerciseId)
    }

    val exerciseType = exercise?.exerciseType ?: ExerciseType.Regular
    val exerciseName = exercise?.name ?: "Exercise"
    val defaultRest = exercise?.defaultRestSeconds

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
    var durationMinutes by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(SetDifficulty.Ideal) }
    var showDifficultyPicker by remember { mutableStateOf(false) }
    var setToDelete by remember { mutableStateOf<SetEntity?>(null) }
    var setToEdit by remember { mutableStateOf<SetEntity?>(null) }
    var didPrefill by remember(workoutExerciseId) { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    val bodyWeight = workout?.bodyWeightKg
    val needsBodyWeight = exerciseType.usesBodyWeight
    val missingBodyWeight = needsBodyWeight && bodyWeight == null

    fun applyPrefillFromSet(set: SetEntity) {
        when (exerciseType) {
            ExerciseType.Regular,
            ExerciseType.WeightedBodyweight,
            ExerciseType.Assisted,
            -> {
                addedWeight = formatKg(set.addedWeightKg)
                reps = set.reps.toString()
            }
            ExerciseType.TimeOnly -> {
                val total = set.durationSeconds ?: return
                durationMinutes = (total / 60).toString()
                durationSeconds = (total % 60).toString()
            }
            ExerciseType.DistanceTime -> {
                set.distanceMeters?.let { distanceKm = formatKg(it / 1000.0) }
                val total = set.durationSeconds ?: return
                durationMinutes = (total / 60).toString()
                durationSeconds = (total % 60).toString()
            }
        }
        difficulty = set.difficulty
    }

    fun applyPrefillFromHistory(entry: ExerciseHistoryEntry) {
        when (exerciseType) {
            ExerciseType.Regular,
            ExerciseType.WeightedBodyweight,
            ExerciseType.Assisted,
            -> {
                addedWeight = formatKg(entry.addedWeightKg)
                reps = entry.reps.toString()
            }
            ExerciseType.TimeOnly -> {
                val total = entry.durationSeconds ?: return
                durationMinutes = (total / 60).toString()
                durationSeconds = (total % 60).toString()
            }
            ExerciseType.DistanceTime -> {
                entry.distanceMeters?.let { distanceKm = formatKg(it / 1000.0) }
                val total = entry.durationSeconds ?: return
                durationMinutes = (total / 60).toString()
                durationSeconds = (total % 60).toString()
            }
        }
        difficulty = entry.difficulty
    }

    LaunchedEffect(sets, history, exercise) {
        if (didPrefill || exercise == null) return@LaunchedEffect
        val lastSet = sets.lastOrNull()
        if (lastSet != null) {
            applyPrefillFromSet(lastSet)
            didPrefill = true
        } else if (history.isNotEmpty()) {
            applyPrefillFromHistory(history.first())
            didPrefill = true
        }
    }

    fun parsedDurationSeconds(): Int? {
        val minutes = durationMinutes.toIntOrNull() ?: 0
        val seconds = durationSeconds.toIntOrNull() ?: 0
        val total = minutes * 60 + seconds
        return total.takeIf { it > 0 }
    }

    fun canSubmit(): Boolean =
        when (exerciseType) {
            ExerciseType.Regular,
            ExerciseType.WeightedBodyweight,
            ExerciseType.Assisted,
            -> {
                addedWeight.toDoubleOrNull() != null &&
                    reps.toIntOrNull() != null &&
                    !missingBodyWeight
            }
            ExerciseType.TimeOnly -> parsedDurationSeconds() != null
            ExerciseType.DistanceTime -> {
                distanceKm.toDoubleOrNull()?.let { it > 0 } == true &&
                    parsedDurationSeconds() != null
            }
        }

    fun submitSet() {
        when (exerciseType) {
            ExerciseType.Regular,
            ExerciseType.WeightedBodyweight,
            ExerciseType.Assisted,
            -> {
                val weight = addedWeight.toDoubleOrNull() ?: return
                val repCount = reps.toIntOrNull() ?: return
                viewModel.addSet(
                    workoutExerciseId = workoutExerciseId,
                    addedWeightKg = weight,
                    reps = repCount,
                    difficulty = difficulty,
                ) {
                    addedWeight = formatKg(weight)
                    reps = repCount.toString()
                    if (!running) timer.start()
                }
            }
            ExerciseType.TimeOnly -> {
                val duration = parsedDurationSeconds() ?: return
                viewModel.addSet(
                    workoutExerciseId = workoutExerciseId,
                    difficulty = difficulty,
                    durationSeconds = duration,
                ) {
                    durationMinutes = (duration / 60).toString()
                    durationSeconds = (duration % 60).toString()
                    if (!running) timer.start()
                }
            }
            ExerciseType.DistanceTime -> {
                val km = distanceKm.toDoubleOrNull() ?: return
                val duration = parsedDurationSeconds() ?: return
                viewModel.addSet(
                    workoutExerciseId = workoutExerciseId,
                    difficulty = difficulty,
                    durationSeconds = duration,
                    distanceMeters = km * 1000.0,
                ) {
                    distanceKm = formatKg(km)
                    durationMinutes = (duration / 60).toString()
                    durationSeconds = (duration % 60).toString()
                    if (!running) timer.start()
                }
            }
        }
    }

    val historyGroups = remember(history) {
        history
            .groupBy { it.performedAt to it.workoutName }
            .map { (key, entries) ->
                HistoryWorkoutGroup(
                    performedAt = key.first,
                    workoutName = key.second,
                    entries = entries,
                )
            }
            .sortedByDescending { it.performedAt }
    }

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
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(exerciseName, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        exerciseType.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MetaLine(
                        bodyWeight = bodyWeight,
                        needsBodyWeight = needsBodyWeight,
                        startingResistanceKg = workoutExercise?.startingResistanceKg
                            ?.takeIf { exerciseType.usesStartingResistance },
                    )
                    if (missingBodyWeight) {
                        Text(
                            "Set body weight on the workout to log this exercise.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Text("Sets", style = MaterialTheme.typography.titleMedium)
                    if (sets.isEmpty()) {
                        Text(
                            "No sets logged yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sets.forEach { set ->
                                SetCard(
                                    set = set,
                                    type = exerciseType,
                                    startingResistanceKg = workoutExercise?.startingResistanceKg,
                                    bodyWeightKg = bodyWeight,
                                    onEdit = { setToEdit = set },
                                    onDelete = { setToDelete = set },
                                )
                            }
                        }
                    }

                    Text("Rest timer", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatTimer(remaining),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
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

                    Text("Add set", style = MaterialTheme.typography.titleMedium)
                    SetInputFields(
                        exerciseType = exerciseType,
                        addedWeight = addedWeight,
                        onAddedWeightChange = { addedWeight = it },
                        reps = reps,
                        onRepsChange = { reps = it },
                        durationMinutes = durationMinutes,
                        onDurationMinutesChange = { durationMinutes = it },
                        durationSeconds = durationSeconds,
                        onDurationSecondsChange = { durationSeconds = it },
                        distanceKm = distanceKm,
                        onDistanceKmChange = { distanceKm = it },
                        startingResistanceKg = workoutExercise?.startingResistanceKg,
                        bodyWeightKg = bodyWeight,
                        inlineDifficulty = true,
                        difficultyContent = {
                            DifficultyIconButton(
                                difficulty = difficulty,
                                onClick = { showDifficultyPicker = true },
                            )
                        },
                    )
                    Button(
                        onClick = { submitSet() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canSubmit(),
                    ) { Text("Add set") }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (historyGroups.isEmpty()) {
                        item {
                            Text(
                                "No previous sets for this exercise yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(
                        items = historyGroups,
                        key = { "${it.performedAt}-${it.workoutName}" },
                    ) { group ->
                        HistoryWorkoutCard(group = group)
                    }
                }
            }
        }
    }

    if (showDifficultyPicker) {
        DifficultyPickerDialog(
            selected = difficulty,
            onSelect = {
                difficulty = it
                showDifficultyPicker = false
            },
            onDismiss = { showDifficultyPicker = false },
        )
    }

    setToEdit?.let { set ->
        EditSetDialog(
            set = set,
            exerciseType = exerciseType,
            startingResistanceKg = workoutExercise?.startingResistanceKg,
            bodyWeightKg = bodyWeight,
            missingBodyWeight = missingBodyWeight,
            onDismiss = { setToEdit = null },
            onSave = { updated ->
                viewModel.updateSet(updated) {
                    setToEdit = null
                }
            },
        )
    }

    setToDelete?.let { set ->
        ConfirmDeleteDialog(
            title = "Delete set?",
            body = "Remove set ${set.position + 1} from this exercise?",
            onConfirm = {
                setToDelete = null
                viewModel.deleteSet(set)
            },
            onDismiss = { setToDelete = null },
        )
    }
}

@Composable
private fun DifficultyPickerDialog(
    selected: SetDifficulty,
    onSelect: (SetDifficulty) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(32.dp)
                    .clickable(enabled = false) {},
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Difficulty",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SetDifficulty.entries.forEach { option ->
                        val isSelected = option == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(option) }
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaLine(
    bodyWeight: Double?,
    needsBodyWeight: Boolean,
    startingResistanceKg: Double?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (bodyWeight != null) {
            Text(
                "Body weight: ${formatKg(bodyWeight)} kg",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                "Body weight: not set on this workout",
                color = if (needsBodyWeight) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (startingResistanceKg != null) {
            Text(
                "Starting resistance: ${formatKg(startingResistanceKg)} kg",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SetCard(
    set: SetEntity,
    type: ExerciseType,
    startingResistanceKg: Double?,
    bodyWeightKg: Double?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val paceLine = if (type == ExerciseType.DistanceTime) {
        formatSetPace(set)
    } else {
        null
    }
    AppCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = 96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Set ${set.position + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatSetSummary(set, type, startingResistanceKg, bodyWeightKg),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (paceLine != null) {
                    Text(
                        text = paceLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = set.difficulty.icon,
                contentDescription = set.difficulty.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit set",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Delete set",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSetDialog(
    set: SetEntity,
    exerciseType: ExerciseType,
    startingResistanceKg: Double?,
    bodyWeightKg: Double?,
    missingBodyWeight: Boolean,
    onDismiss: () -> Unit,
    onSave: (SetEntity) -> Unit,
) {
    var addedWeight by remember(set.id) { mutableStateOf(formatKg(set.addedWeightKg)) }
    var reps by remember(set.id) { mutableStateOf(set.reps.toString()) }
    var durationMinutes by remember(set.id) {
        mutableStateOf(((set.durationSeconds ?: 0) / 60).toString().takeIf { (set.durationSeconds ?: 0) > 0 }.orEmpty())
    }
    var durationSeconds by remember(set.id) {
        mutableStateOf(((set.durationSeconds ?: 0) % 60).toString().takeIf { (set.durationSeconds ?: 0) > 0 }.orEmpty())
    }
    var distanceKm by remember(set.id) {
        mutableStateOf(set.distanceMeters?.let { formatKg(it / 1000.0) }.orEmpty())
    }
    var difficulty by remember(set.id) { mutableStateOf(set.difficulty) }
    var showDifficultyPicker by remember { mutableStateOf(false) }

    fun parsedDurationSeconds(): Int? {
        val minutes = durationMinutes.toIntOrNull() ?: 0
        val seconds = durationSeconds.toIntOrNull() ?: 0
        val total = minutes * 60 + seconds
        return total.takeIf { it > 0 }
    }

    val canSave = when (exerciseType) {
        ExerciseType.Regular,
        ExerciseType.WeightedBodyweight,
        ExerciseType.Assisted,
        -> {
            addedWeight.toDoubleOrNull() != null &&
                reps.toIntOrNull() != null &&
                !missingBodyWeight
        }
        ExerciseType.TimeOnly -> parsedDurationSeconds() != null
        ExerciseType.DistanceTime -> {
            distanceKm.toDoubleOrNull()?.let { it > 0 } == true &&
                parsedDurationSeconds() != null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Edit set ${set.position + 1}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SetInputFields(
                        exerciseType = exerciseType,
                        addedWeight = addedWeight,
                        onAddedWeightChange = { addedWeight = it },
                        reps = reps,
                        onRepsChange = { reps = it },
                        durationMinutes = durationMinutes,
                        onDurationMinutesChange = { durationMinutes = it },
                        durationSeconds = durationSeconds,
                        onDurationSecondsChange = { durationSeconds = it },
                        distanceKm = distanceKm,
                        onDistanceKmChange = { distanceKm = it },
                        startingResistanceKg = startingResistanceKg,
                        bodyWeightKg = bodyWeightKg,
                        inlineDifficulty = false,
                        difficultyContent = {
                            DifficultyIconButton(
                                difficulty = difficulty,
                                onClick = { showDifficultyPicker = true },
                            )
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (!canSave) return@TextButton
                            val updated = when (exerciseType) {
                                ExerciseType.Regular,
                                ExerciseType.WeightedBodyweight,
                                ExerciseType.Assisted,
                                -> set.copy(
                                    addedWeightKg = addedWeight.toDoubleOrNull()
                                        ?: return@TextButton,
                                    reps = reps.toIntOrNull() ?: return@TextButton,
                                    difficulty = difficulty,
                                    durationSeconds = null,
                                    distanceMeters = null,
                                )
                                ExerciseType.TimeOnly -> set.copy(
                                    addedWeightKg = 0.0,
                                    reps = 0,
                                    difficulty = difficulty,
                                    durationSeconds = parsedDurationSeconds(),
                                    distanceMeters = null,
                                )
                                ExerciseType.DistanceTime -> set.copy(
                                    addedWeightKg = 0.0,
                                    reps = 0,
                                    difficulty = difficulty,
                                    durationSeconds = parsedDurationSeconds(),
                                    distanceMeters = (distanceKm.toDoubleOrNull()
                                        ?: return@TextButton) * 1000.0,
                                )
                            }
                            onSave(updated)
                        },
                        enabled = canSave,
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showDifficultyPicker) {
        DifficultyPickerDialog(
            selected = difficulty,
            onSelect = {
                difficulty = it
                showDifficultyPicker = false
            },
            onDismiss = { showDifficultyPicker = false },
        )
    }
}

@Composable
private fun HistoryWorkoutCard(group: HistoryWorkoutGroup) {
    AppCard {
        Text(
            text = formatDate(group.performedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = group.workoutName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            group.entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Set ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatHistoryLine(entry),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = entry.difficulty.icon,
                        contentDescription = entry.difficulty.label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyIconButton(
    difficulty: SetDifficulty,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(OutlinedTextFieldDefaults.MinHeight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = difficulty.icon,
            contentDescription = difficulty.label,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SetInputFields(
    exerciseType: ExerciseType,
    addedWeight: String,
    onAddedWeightChange: (String) -> Unit,
    reps: String,
    onRepsChange: (String) -> Unit,
    durationMinutes: String,
    onDurationMinutesChange: (String) -> Unit,
    durationSeconds: String,
    onDurationSecondsChange: (String) -> Unit,
    distanceKm: String,
    onDistanceKmChange: (String) -> Unit,
    startingResistanceKg: Double?,
    bodyWeightKg: Double?,
    inlineDifficulty: Boolean,
    difficultyContent: @Composable () -> Unit,
) {
    when (exerciseType) {
        ExerciseType.Regular,
        ExerciseType.WeightedBodyweight,
        ExerciseType.Assisted,
        -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = addedWeight,
                    onValueChange = onAddedWeightChange,
                    label = {
                        Text(
                            when (exerciseType) {
                                ExerciseType.Assisted -> "Assistance (kg)"
                                else -> "Added weight (kg)"
                            },
                        )
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = onRepsChange,
                    label = { Text("Reps") },
                    modifier = Modifier.weight(if (inlineDifficulty) 0.55f else 1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                if (inlineDifficulty) {
                    difficultyContent()
                }
            }
            val preview = addedWeight.toDoubleOrNull()?.let { weight ->
                effectiveLoadKg(
                    type = exerciseType,
                    startingResistanceKg = startingResistanceKg,
                    addedWeightKg = weight,
                    bodyWeightKg = bodyWeightKg,
                )
            }
            if (inlineDifficulty) {
                if (preview != null) {
                    Text(
                        "Effective load: ${formatKg(preview)} kg",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (preview != null) {
                        Text(
                            "Effective load: ${formatKg(preview)} kg",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    difficultyContent()
                }
            }
        }
        ExerciseType.TimeOnly -> {
            if (inlineDifficulty) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = onDurationMinutesChange,
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = durationSeconds,
                        onValueChange = onDurationSecondsChange,
                        label = { Text("Seconds") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    difficultyContent()
                }
            } else {
                DurationFields(
                    minutes = durationMinutes,
                    seconds = durationSeconds,
                    onMinutesChange = onDurationMinutesChange,
                    onSecondsChange = onDurationSecondsChange,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    difficultyContent()
                }
            }
        }
        ExerciseType.DistanceTime -> {
            if (inlineDifficulty) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = distanceKm,
                        onValueChange = onDistanceKmChange,
                        label = { Text("Distance (km)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = onDurationMinutesChange,
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = durationSeconds,
                        onValueChange = onDurationSecondsChange,
                        label = { Text("Seconds") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            } else {
                OutlinedTextField(
                    value = distanceKm,
                    onValueChange = onDistanceKmChange,
                    label = { Text("Distance (km)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                DurationFields(
                    minutes = durationMinutes,
                    seconds = durationSeconds,
                    onMinutesChange = onDurationMinutesChange,
                    onSecondsChange = onDurationSecondsChange,
                )
            }
            val meters = distanceKm.toDoubleOrNull()?.times(1000.0)
            val minutes = durationMinutes.toIntOrNull() ?: 0
            val seconds = durationSeconds.toIntOrNull() ?: 0
            val duration = (minutes * 60 + seconds).takeIf { it > 0 }
            val pace = if (meters != null && duration != null) {
                formatPace(meters, duration)
            } else {
                null
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (pace != null) {
                    Text(
                        "Pace: $pace",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                difficultyContent()
            }
        }
    }
}

@Composable
private fun DurationFields(
    minutes: String,
    seconds: String,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = minutes,
            onValueChange = onMinutesChange,
            label = { Text("Minutes") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        OutlinedTextField(
            value = seconds,
            onValueChange = onSecondsChange,
            label = { Text("Seconds") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
}

private fun formatSetSummary(
    set: SetEntity,
    type: ExerciseType,
    startingResistanceKg: Double?,
    bodyWeightKg: Double?,
): String =
    when (type) {
        ExerciseType.Regular,
        ExerciseType.WeightedBodyweight,
        ExerciseType.Assisted,
        -> {
            val effective = effectiveLoadKg(
                type,
                startingResistanceKg,
                set.addedWeightKg,
                bodyWeightKg,
            )
            if (effective != null) {
                "${formatKg(effective)} kg × ${set.reps}"
            } else {
                "${formatKg(set.addedWeightKg)} kg × ${set.reps}"
            }
        }
        ExerciseType.TimeOnly -> set.durationSeconds?.let { formatDuration(it) } ?: "—"
        ExerciseType.DistanceTime -> {
            val distance = set.distanceMeters?.let { formatDistanceKm(it) } ?: "—"
            val duration = set.durationSeconds?.let { formatDuration(it) } ?: "—"
            "$distance in $duration"
        }
    }

private fun formatSetPace(set: SetEntity): String? {
    val meters = set.distanceMeters ?: return null
    val duration = set.durationSeconds ?: return null
    return formatPace(meters, duration)
}

private fun formatHistoryLine(entry: ExerciseHistoryEntry): String =
    when (entry.exerciseType) {
        ExerciseType.Regular,
        ExerciseType.WeightedBodyweight,
        ExerciseType.Assisted,
        -> {
            val effective = entry.effectiveLoadOrNull()
            if (effective != null) {
                "${formatKg(effective)} kg × ${entry.reps}"
            } else {
                "${formatKg(entry.addedWeightKg)} kg × ${entry.reps}"
            }
        }
        ExerciseType.TimeOnly -> entry.durationSeconds?.let { formatDuration(it) } ?: "—"
        ExerciseType.DistanceTime -> {
            val distance = entry.distanceMeters?.let { formatDistanceKm(it) } ?: "—"
            val duration = entry.durationSeconds?.let { formatDuration(it) } ?: "—"
            "$distance in $duration"
        }
    }

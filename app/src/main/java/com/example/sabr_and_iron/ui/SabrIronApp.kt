package com.example.sabr_and_iron.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sabr_and_iron.ui.components.AppCardShape
import com.example.sabr_and_iron.ui.components.LocalAppSnackbar
import com.example.sabr_and_iron.ui.components.TabBottomChrome
import com.example.sabr_and_iron.ui.exercises.ExerciseEditorScreen
import com.example.sabr_and_iron.ui.exercises.ExercisesScreen
import com.example.sabr_and_iron.ui.exercises.ExercisesViewModel
import com.example.sabr_and_iron.ui.navigation.AppDestination
import com.example.sabr_and_iron.ui.navigation.Routes
import com.example.sabr_and_iron.ui.records.RecordDetailScreen
import com.example.sabr_and_iron.ui.records.RecordsSortMenu
import com.example.sabr_and_iron.ui.records.RecordsScreen
import com.example.sabr_and_iron.ui.routines.RoutineEditorScreen
import com.example.sabr_and_iron.ui.routines.RoutinesScreen
import com.example.sabr_and_iron.ui.settings.SettingsScreen
import com.example.sabr_and_iron.ui.workouts.ActiveExerciseScreen
import com.example.sabr_and_iron.ui.workouts.WorkoutDetailScreen
import com.example.sabr_and_iron.ui.workouts.WorkoutFormScreen
import com.example.sabr_and_iron.ui.workouts.WorkoutsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SabrIronApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = navBackStackEntry?.destination

    val tabRoutes = AppDestination.entries.map { it.route }.toSet()
    val isTabRoot = currentRoute in tabRoutes
    val selectedDestination = AppDestination.entries.find { it.route == currentRoute }
        ?: AppDestination.Workouts
    val showStartWorkoutFab = currentRoute == AppDestination.Workouts.route
    val showAddExerciseFab = currentRoute == AppDestination.Exercises.route
    val showAddRoutineFab = currentRoute == AppDestination.Routines.route
    val addFabShape = RoundedCornerShape(16.dp)
    val isWorkoutDetail = currentRoute?.startsWith("workout/") == true &&
        currentRoute?.contains("/exercise/") != true &&
        currentRoute != Routes.WorkoutNew
    val isExerciseEdit = currentRoute?.startsWith("exercise/") == true &&
        currentRoute != Routes.ExerciseNew
    val isRoutineEdit = currentRoute?.startsWith("routine/") == true &&
        currentRoute != Routes.RoutineNew

    var workoutEditTick by remember { mutableIntStateOf(0) }
    var workoutEditing by remember { mutableStateOf(false) }
    var workoutDeleteTick by remember { mutableIntStateOf(0) }
    var exerciseDeleteTick by remember { mutableIntStateOf(0) }
    var routineDeleteTick by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isWorkoutDetail) {
        if (!isWorkoutDetail) {
            workoutEditing = false
            workoutEditTick = 0
            workoutDeleteTick = 0
        }
    }
    LaunchedEffect(isExerciseEdit) {
        if (!isExerciseEdit) exerciseDeleteTick = 0
    }
    LaunchedEffect(isRoutineEdit) {
        if (!isRoutineEdit) routineDeleteTick = 0
    }

    val title = when {
        currentRoute == Routes.WorkoutNew -> "Start New Workout"
        currentRoute?.startsWith("workout/") == true &&
            currentRoute.contains("/exercise/") -> "Active Exercise"
        currentRoute?.startsWith("workout/") == true -> "Workout"
        currentRoute == Routes.RoutineNew -> "New Routine"
        currentRoute?.startsWith("routine/") == true -> "Edit Routine"
        currentRoute == Routes.ExerciseNew -> "New Exercise"
        currentRoute?.startsWith("exercise/") == true -> "Edit Exercise"
        currentRoute?.startsWith("records/") == true -> "Record"
        currentRoute == Routes.Settings -> "Settings"
        else -> selectedDestination.title
    }

    CompositionLocalProvider(LocalAppSnackbar provides snackbarHostState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (!isTabRoot) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute == AppDestination.Records.route) {
                        val recordsEntry = navController.getBackStackEntry(
                            AppDestination.Records.route,
                        )
                        RecordsSortMenu(
                            viewModel = viewModel(viewModelStoreOwner = recordsEntry),
                        )
                        IconButton(onClick = { navController.navigate(Routes.Settings) }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                            )
                        }
                    }
                    if (isExerciseEdit) {
                        TextButton(onClick = { exerciseDeleteTick++ }) {
                            Text("Delete exercise", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (isRoutineEdit) {
                        TextButton(onClick = { routineDeleteTick++ }) {
                            Text("Delete routine", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (isWorkoutDetail) {
                        if (workoutEditing) {
                            TextButton(onClick = { workoutEditTick++ }) {
                                Text("Done")
                            }
                        } else {
                            IconButton(onClick = { workoutEditTick++ }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit workout")
                            }
                            TextButton(onClick = { workoutDeleteTick++ }) {
                                Text("Delete workout", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        bottomBar = {
            if (isTabRoot) {
                TabBottomChrome(
                    navController = navController,
                    currentDestination = currentDestination,
                    actionRow = when {
                        showAddExerciseFab || showAddRoutineFab || showStartWorkoutFab -> {
                            {
                                when {
                                    showAddExerciseFab -> {
                                        val exercisesEntry = navController.getBackStackEntry(
                                            AppDestination.Exercises.route,
                                        )
                                        val exercisesVm: ExercisesViewModel =
                                            viewModel(viewModelStoreOwner = exercisesEntry)
                                        var query by remember { mutableStateOf("") }
                                        OutlinedTextField(
                                            value = query,
                                            onValueChange = {
                                                query = it
                                                exercisesVm.onQueryChange(it)
                                            },
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    "Search name or muscle",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Search,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            },
                                            singleLine = true,
                                            shape = AppCardShape,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.92f,
                                                ),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.82f,
                                                ),
                                                disabledContainerColor = Color.Transparent,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.7f,
                                                ),
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                cursorColor = MaterialTheme.colorScheme.primary,
                                            ),
                                        )
                                        FloatingActionButton(
                                            onClick = { navController.navigate(Routes.ExerciseNew) },
                                            shape = addFabShape,
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                                        }
                                    }
                                    showAddRoutineFab -> {
                                        Spacer(modifier = Modifier.weight(1f))
                                        ExtendedFloatingActionButton(
                                            onClick = { navController.navigate(Routes.RoutineNew) },
                                            icon = {
                                                Icon(Icons.Filled.Add, contentDescription = null)
                                            },
                                            text = { Text("Create New Routine") },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                    }
                                    showStartWorkoutFab -> {
                                        Spacer(modifier = Modifier.weight(1f))
                                        ExtendedFloatingActionButton(
                                            onClick = { navController.navigate(Routes.WorkoutNew) },
                                            icon = {
                                                Icon(Icons.Filled.Add, contentDescription = null)
                                            },
                                            text = { Text("Start New Workout") },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                        else -> null
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Workouts.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(AppDestination.Records.route) {
                RecordsScreen(
                    onOpenRecord = { id ->
                        navController.navigate(Routes.recordDetail(id))
                    },
                )
            }
            composable(Routes.Settings) {
                val recordsEntry = navController.getBackStackEntry(
                    AppDestination.Records.route,
                )
                SettingsScreen(
                    viewModel = viewModel(viewModelStoreOwner = recordsEntry),
                )
            }
            composable(
                route = Routes.RecordDetail,
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
            ) { entry ->
                val exerciseId = entry.arguments?.getLong("exerciseId") ?: return@composable
                RecordDetailScreen(exerciseId = exerciseId)
            }
            composable(AppDestination.Exercises.route) {
                ExercisesScreen(
                    onOpenExercise = { id -> navController.navigate(Routes.exerciseEdit(id)) },
                )
            }
            composable(AppDestination.Routines.route) {
                RoutinesScreen(
                    onOpenRoutine = { id -> navController.navigate(Routes.routineEdit(id)) },
                )
            }
            composable(AppDestination.Workouts.route) {
                WorkoutsScreen(
                    onOpenWorkout = { id -> navController.navigate(Routes.workoutDetail(id)) },
                )
            }
            composable(Routes.WorkoutNew) {
                WorkoutFormScreen(
                    onSaved = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.workoutDetail(id))
                    },
                )
            }
            composable(
                route = Routes.WorkoutDetail,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
            ) { entry ->
                val workoutId = entry.arguments?.getLong("workoutId") ?: return@composable
                WorkoutDetailScreen(
                    workoutId = workoutId,
                    editTick = workoutEditTick,
                    deleteTick = workoutDeleteTick,
                    onEditingChange = { workoutEditing = it },
                    onOpenExercise = { weId ->
                        navController.navigate(Routes.activeExercise(workoutId, weId))
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ActiveExercise,
                arguments = listOf(
                    navArgument("workoutId") { type = NavType.LongType },
                    navArgument("workoutExerciseId") { type = NavType.LongType },
                ),
            ) { entry ->
                val weId = entry.arguments?.getLong("workoutExerciseId") ?: return@composable
                ActiveExerciseScreen(workoutExerciseId = weId)
            }
            composable(Routes.RoutineNew) {
                RoutineEditorScreen(
                    routineId = null,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.RoutineEdit,
                arguments = listOf(navArgument("routineId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("routineId") ?: return@composable
                RoutineEditorScreen(
                    routineId = id,
                    deleteTick = routineDeleteTick,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.ExerciseNew) {
                ExerciseEditorScreen(
                    exerciseId = null,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ExerciseEdit,
                arguments = listOf(navArgument("exerciseId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("exerciseId") ?: return@composable
                ExerciseEditorScreen(
                    exerciseId = id,
                    deleteTick = exerciseDeleteTick,
                    onDone = { navController.popBackStack() },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )
        }
    }
    }
}

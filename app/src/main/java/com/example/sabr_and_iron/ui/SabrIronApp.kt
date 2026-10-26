package com.example.sabr_and_iron.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sabr_and_iron.ui.exercises.ExerciseEditorScreen
import com.example.sabr_and_iron.ui.exercises.ExercisesScreen
import com.example.sabr_and_iron.ui.navigation.AppDestination
import com.example.sabr_and_iron.ui.navigation.Routes
import com.example.sabr_and_iron.ui.records.RecordsScreen
import com.example.sabr_and_iron.ui.routines.RoutineEditorScreen
import com.example.sabr_and_iron.ui.routines.RoutinesScreen
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

    val title = when {
        currentRoute == Routes.WorkoutNew -> "Start New Workout"
        currentRoute?.startsWith("workout/") == true &&
            currentRoute.contains("/exercise/") -> "Active Exercise"
        currentRoute?.startsWith("workout/") == true -> "Workout"
        currentRoute == Routes.RoutineNew -> "New Routine"
        currentRoute?.startsWith("routine/") == true -> "Edit Routine"
        currentRoute == Routes.ExerciseNew -> "New Exercise"
        currentRoute?.startsWith("exercise/") == true -> "Edit Exercise"
        else -> selectedDestination.title
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            if (isTabRoot) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    AppDestination.entries.forEach { destination ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showStartWorkoutFab) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.WorkoutNew) },
                    icon = {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    },
                    text = { Text("Start New Workout") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Workouts.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Records.route) {
                RecordsScreen()
            }
            composable(AppDestination.Exercises.route) {
                ExercisesScreen(
                    onAddExercise = { navController.navigate(Routes.ExerciseNew) },
                    onOpenExercise = { id -> navController.navigate(Routes.exerciseEdit(id)) },
                )
            }
            composable(AppDestination.Routines.route) {
                RoutinesScreen(
                    onAddRoutine = { navController.navigate(Routes.RoutineNew) },
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
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

package com.guicarneirodev.gympro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.guicarneirodev.gympro.presentation.ui.auth.login.LoginScreen
import com.guicarneirodev.gympro.presentation.ui.auth.register.RegisterScreen
import com.guicarneirodev.gympro.presentation.ui.exercise.form.ExerciseFormScreen
import com.guicarneirodev.gympro.presentation.ui.exercise.list.ExerciseListScreen
import com.guicarneirodev.gympro.presentation.ui.settings.SettingsScreen
import com.guicarneirodev.gympro.presentation.ui.workout.form.WorkoutFormScreen
import com.guicarneirodev.gympro.presentation.ui.workout.list.WorkoutListScreen
import org.koin.compose.koinInject

@Composable
fun GymProNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val auth = koinInject<FirebaseAuth>()
    val startDestination = if (auth.currentUser != null) {
        Screen.WorkoutList
    } else {
        Screen.Login
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Login> {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.WorkoutList) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.WorkoutList) {
                        popUpTo(Screen.Register) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.WorkoutList> {
            WorkoutListScreen(
                onNavigateToWorkout = { workoutId ->
                    navController.navigate(Screen.ExerciseList(workoutId))
                },
                onNavigateToAddWorkout = {
                    navController.navigate(Screen.WorkoutForm())
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.WorkoutForm> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.WorkoutForm>()
            WorkoutFormScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToExercises = { workoutId ->
                    navController.navigate(Screen.ExerciseList(workoutId)) {
                        popUpTo(Screen.WorkoutList)
                    }
                }
            )
        }

        composable<Screen.ExerciseList> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.ExerciseList>()
            ExerciseListScreen(
                onNavigateToExercise = { exerciseId ->
                    navController.navigate(
                        Screen.ExerciseForm(
                            workoutId = screen.workoutId,
                            exerciseId = exerciseId
                        )
                    )
                },
                onNavigateToAddExercise = {
                    navController.navigate(
                        Screen.ExerciseForm(workoutId = screen.workoutId)
                    )
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable<Screen.ExerciseForm> { backStackEntry ->
            val screen = backStackEntry.toRoute<Screen.ExerciseForm>()
            ExerciseFormScreen(
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
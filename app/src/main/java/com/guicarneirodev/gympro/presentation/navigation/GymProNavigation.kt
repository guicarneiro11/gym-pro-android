package com.guicarneirodev.gympro.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Login : Screen

    @Serializable
    data object Register : Screen

    @Serializable
    data object WorkoutList : Screen

    @Serializable
    data class WorkoutForm(
        val workoutId: String? = null
    ) : Screen

    @Serializable
    data class ExerciseList(
        val workoutId: String
    ) : Screen

    @Serializable
    data class ExerciseForm(
        val workoutId: String,
        val exerciseId: String? = null
    ) : Screen

    @Serializable
    data object Settings : Screen
}
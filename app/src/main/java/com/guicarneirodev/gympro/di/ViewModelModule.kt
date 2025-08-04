package com.guicarneirodev.gympro.di

import com.guicarneirodev.gympro.presentation.ui.auth.login.LoginViewModel
import com.guicarneirodev.gympro.presentation.ui.auth.register.RegisterViewModel
import com.guicarneirodev.gympro.presentation.ui.workout.list.WorkoutListViewModel
import com.guicarneirodev.gympro.presentation.ui.workout.form.WorkoutFormViewModel
import com.guicarneirodev.gympro.presentation.ui.exercise.list.ExerciseListViewModel
import com.guicarneirodev.gympro.presentation.ui.exercise.form.ExerciseFormViewModel
import org.koin.core.module.dsl.*
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { LoginViewModel(get()) }
    viewModel { RegisterViewModel(get()) }
    viewModel { WorkoutListViewModel(get(), get()) }
    viewModel { WorkoutFormViewModel(get(), get(), get()) }
    viewModel { ExerciseListViewModel(get(), get()) }
    viewModel { ExerciseFormViewModel(get(), get()) }
}
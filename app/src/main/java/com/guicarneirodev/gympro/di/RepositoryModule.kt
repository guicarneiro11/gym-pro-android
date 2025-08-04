package com.guicarneirodev.gympro.di

import com.guicarneirodev.gympro.data.repository.AuthRepositoryImpl
import com.guicarneirodev.gympro.data.repository.ExerciseRepositoryImpl
import com.guicarneirodev.gympro.data.repository.WorkoutRepositoryImpl
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.domain.repository.ExerciseRepository
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> {
        AuthRepositoryImpl(
            auth = get(),
            firestore = get(),
            context = androidContext(),
            imageLoader = get(),
            userPreferencesManager = get()
        )
    }

    single<WorkoutRepository> {
        WorkoutRepositoryImpl(
            firestore = get(),
            workoutDao = get(),
            networkMonitor = get(),
            syncManager = get(),
            getCurrentUserId = { get<FirebaseAuth>().currentUser?.uid }
        )
    }

    single<ExerciseRepository> {
        ExerciseRepositoryImpl(
            context = androidContext(),
            firestore = get(),
            storage = get(),
            exerciseDao = get(),
            networkMonitor = get(),
            syncManager = get(),
            getCurrentUserId = { get<FirebaseAuth>().currentUser?.uid }
        )
    }
}
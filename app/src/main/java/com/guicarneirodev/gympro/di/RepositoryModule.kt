package com.guicarneirodev.gympro.di

import com.guicarneirodev.gympro.data.repository.AuthRepositoryImpl
import com.guicarneirodev.gympro.data.repository.ExerciseRepositoryImpl
import com.guicarneirodev.gympro.data.repository.WorkoutRepositoryImpl
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.domain.repository.ExerciseRepository
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.google.firebase.auth.FirebaseAuth
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<ExerciseRepository> {
        ExerciseRepositoryImpl(
            firestore = get(),
            storage = get(),
            getCurrentUserId = { get<FirebaseAuth>().currentUser?.uid }
        )
    }
}
package com.guicarneirodev.gympro.di

import androidx.room.Room
import com.guicarneirodev.gympro.data.local.database.GymProDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GymProDatabase::class.java,
            "gympro_database"
        ).build()
    }

    single { get<GymProDatabase>().workoutDao() }
    single { get<GymProDatabase>().exerciseDao() }
}
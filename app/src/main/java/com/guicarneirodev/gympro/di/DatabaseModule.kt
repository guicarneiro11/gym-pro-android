package com.guicarneirodev.gympro.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guicarneirodev.gympro.data.local.database.GymProDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE exercises ADD COLUMN position INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            GymProDatabase::class.java,
            "gympro_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    single { get<GymProDatabase>().workoutDao() }
    single { get<GymProDatabase>().exerciseDao() }
}
package com.guicarneirodev.gympro.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.guicarneirodev.gympro.data.local.dao.ExerciseDao
import com.guicarneirodev.gympro.data.local.dao.WorkoutDao
import com.guicarneirodev.gympro.data.local.entity.ExerciseEntity
import com.guicarneirodev.gympro.data.local.entity.WorkoutEntity

@Database(
    entities = [WorkoutEntity::class, ExerciseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GymProDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
}
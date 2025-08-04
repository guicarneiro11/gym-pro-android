package com.guicarneirodev.gympro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val date: Long,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
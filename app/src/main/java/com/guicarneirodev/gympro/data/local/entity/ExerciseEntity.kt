package com.guicarneirodev.gympro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val workoutId: String,
    val name: String,
    val observations: String,
    val imageUrl: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
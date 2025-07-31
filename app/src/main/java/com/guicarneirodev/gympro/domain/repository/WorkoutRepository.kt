package com.guicarneirodev.gympro.domain.repository

import com.guicarneirodev.gympro.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun createWorkout(workout: Workout): Result<String>
    suspend fun updateWorkout(workout: Workout): Result<Unit>
    suspend fun deleteWorkout(workoutId: String): Result<Unit>
    suspend fun getWorkout(workoutId: String): Result<Workout>
    fun getWorkoutsByUser(userId: String): Flow<List<Workout>>
}
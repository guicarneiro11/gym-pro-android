package com.guicarneirodev.gympro.domain.repository

import com.guicarneirodev.gympro.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    suspend fun createExercise(exercise: Exercise): Result<String>
    suspend fun updateExercise(exercise: Exercise): Result<Unit>
    suspend fun deleteExercise(exerciseId: String): Result<Unit>
    suspend fun getExercise(exerciseId: String): Result<Exercise>
    fun getExercisesByWorkout(workoutId: String): Flow<List<Exercise>>
    suspend fun uploadExerciseImage(exerciseId: String, imageUri: String): Result<String>
}
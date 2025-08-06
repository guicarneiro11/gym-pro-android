package com.guicarneirodev.gympro.data.local.dao

import androidx.room.*
import com.guicarneirodev.gympro.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY position ASC, id ASC")
    fun getExercisesByWorkout(workoutId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExercise(exerciseId: String): ExerciseEntity?

    @Query("SELECT MAX(position) FROM exercises WHERE workoutId = :workoutId")
    suspend fun getMaxPosition(workoutId: String): Int?

    @Query("UPDATE exercises SET position = :position WHERE id = :exerciseId")
    suspend fun updatePosition(exerciseId: String, position: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: String)
}
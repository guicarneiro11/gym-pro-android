package com.guicarneirodev.gympro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.guicarneirodev.gympro.data.local.preferences.UserPreferencesManager
import com.guicarneirodev.gympro.data.mapper.toDomain
import com.guicarneirodev.gympro.data.mapper.toDto
import com.guicarneirodev.gympro.data.remote.dto.WorkoutDto
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WorkoutRepositoryImpl(
    firestore: FirebaseFirestore,
    private val userPreferencesManager: UserPreferencesManager
) : WorkoutRepository {

    private val workoutsCollection = firestore.collection("workouts")

    override suspend fun createWorkout(workout: Workout): Result<String> {
        return try {
            val documentRef = workoutsCollection.document()
            val workoutWithId = workout.copy(id = documentRef.id)
            documentRef.set(workoutWithId.toDto()).await()

            userPreferencesManager.updateLastSync()

            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkout(workout: Workout): Result<Unit> {
        return try {
            workoutsCollection.document(workout.id)
                .set(workout.toDto())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutsCollection.document(workoutId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkout(workoutId: String): Result<Workout> {
        return try {
            val snapshot = workoutsCollection.document(workoutId).get().await()
            snapshot.toObject(WorkoutDto::class.java)?.let {
                Result.success(it.toDomain())
            } ?: Result.failure(Exception("Workout not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWorkoutsByUser(userId: String): Flow<List<Workout>> {
        return workoutsCollection
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(WorkoutDto::class.java)?.toDomain()
                }
            }
    }
}
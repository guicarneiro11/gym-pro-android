package com.guicarneirodev.gympro.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.gympro.data.local.dao.WorkoutDao
import com.guicarneirodev.gympro.data.mapper.toDomain
import com.guicarneirodev.gympro.data.mapper.toDto
import com.guicarneirodev.gympro.data.mapper.toEntity
import com.guicarneirodev.gympro.data.remote.dto.WorkoutDto
import com.guicarneirodev.gympro.data.util.NetworkMonitor
import com.guicarneirodev.gympro.data.util.SyncManager
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class WorkoutRepositoryImpl(
    firestore: FirebaseFirestore,
    private val workoutDao: WorkoutDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager,
    private val getCurrentUserId: () -> String?
) : WorkoutRepository {

    private val workoutsCollection = firestore.collection("workouts")

    override suspend fun createWorkout(workout: Workout): Result<String> {
        return try {
            val documentRef = workoutsCollection.document()
            val workoutWithId = workout.copy(id = documentRef.id)

            workoutDao.insertWorkout(workoutWithId.toEntity())

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                documentRef.set(workoutWithId.toDto()).await()
            }

            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkout(workout: Workout): Result<Unit> {
        return try {
            workoutDao.insertWorkout(workout.toEntity())

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                workoutsCollection.document(workout.id)
                    .set(workout.toDto())
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            workoutDao.deleteWorkoutById(workoutId)

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                workoutsCollection.document(workoutId)
                    .delete()
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkout(workoutId: String): Result<Workout> {
        return try {
            val localWorkout = workoutDao.getWorkout(workoutId)
            if (localWorkout != null) {
                Result.success(localWorkout.toDomain())
            } else {
                val isOnline = networkMonitor.isOnline.first()
                if (isOnline) {
                    val snapshot = workoutsCollection.document(workoutId).get().await()
                    snapshot.toObject(WorkoutDto::class.java)?.let { dto ->
                        val workout = dto.toDomain()
                        workoutDao.insertWorkout(workout.toEntity())
                        Result.success(workout)
                    } ?: Result.failure(Exception("Workout not found"))
                } else {
                    Result.failure(Exception("Workout not found offline"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWorkoutsByUser(userId: String): Flow<List<Workout>> {
        return flow {
            val localWorkouts = workoutDao.getWorkoutsByUser(userId)
                .first()
                .map { it.toDomain() }

            emit(localWorkouts)

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                try {
                    syncManager.startSync()

                    val remoteWorkouts = workoutsCollection
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { doc ->
                            doc.toObject(WorkoutDto::class.java)?.toDomain()
                        }

                    remoteWorkouts.forEach { workout ->
                        workoutDao.insertWorkout(workout.toEntity())
                    }

                    val updatedLocal = workoutDao.getWorkoutsByUser(userId)
                        .first()
                        .map { it.toDomain() }

                    if (updatedLocal != localWorkouts) {
                        emit(updatedLocal)
                    }
                } catch (_: Exception) {
                    // Continue com dados locais
                } finally {
                    syncManager.endSync()
                }
            }

            workoutDao.getWorkoutsByUser(userId)
                .map { workouts -> workouts.map { it.toDomain() } }
                .collect { workouts ->
                    emit(workouts)
                }
        }.distinctUntilChanged()
    }
}
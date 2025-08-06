package com.guicarneirodev.gympro.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.guicarneirodev.gympro.data.local.dao.ExerciseDao
import com.guicarneirodev.gympro.data.mapper.toDomain
import com.guicarneirodev.gympro.data.mapper.toDto
import com.guicarneirodev.gympro.data.mapper.toEntity
import com.guicarneirodev.gympro.data.remote.dto.ExerciseDto
import com.guicarneirodev.gympro.data.util.NetworkMonitor
import com.guicarneirodev.gympro.data.util.SyncManager
import com.guicarneirodev.gympro.domain.model.Exercise
import com.guicarneirodev.gympro.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ExerciseRepositoryImpl(
    private val context: Context,
    firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val exerciseDao: ExerciseDao,
    private val networkMonitor: NetworkMonitor,
    private val syncManager: SyncManager,
    private val getCurrentUserId: () -> String?
) : ExerciseRepository {

    private val exercisesCollection = firestore.collection("exercises")
    private val storageRef = storage.reference

    override suspend fun createExercise(exercise: Exercise): Result<String> {
        return try {
            val documentRef = exercisesCollection.document()

            val maxPosition = exerciseDao.getMaxPosition(exercise.workoutId) ?: -1
            val exerciseWithIdAndPosition = exercise.copy(
                id = documentRef.id,
                position = maxPosition + 1
            )

            exerciseDao.insertExercise(exerciseWithIdAndPosition.toEntity())

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                documentRef.set(exerciseWithIdAndPosition.toDto()).await()
            }

            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderExercises(exercises: List<Exercise>): Result<Unit> {
        return try {
            exercises.forEach { exercise ->
                exerciseDao.insertExercise(exercise.toEntity())
            }

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                exercises.forEach { exercise ->
                    exercisesCollection.document(exercise.id)
                        .update("position", exercise.position)
                        .await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExercise(exercise: Exercise): Result<Unit> {
        return try {
            exerciseDao.insertExercise(exercise.toEntity())

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                exercisesCollection.document(exercise.id)
                    .update(
                        mapOf(
                            "name" to exercise.name,
                            "observations" to exercise.observations,
                            "imageUrl" to exercise.imageUrl
                        )
                    )
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExercise(exerciseId: String): Result<Unit> {
        return try {
            val exercise = getExercise(exerciseId).getOrNull()

            exerciseDao.deleteExerciseById(exerciseId)

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                exercise?.imageUrl?.let { url ->
                    try {
                        storage.getReferenceFromUrl(url).delete().await()
                    } catch (_: Exception) {
                    }
                }

                exercisesCollection.document(exerciseId)
                    .delete()
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExercise(exerciseId: String): Result<Exercise> {
        return try {
            val localExercise = exerciseDao.getExercise(exerciseId)
            if (localExercise != null) {
                Result.success(localExercise.toDomain())
            } else {
                val isOnline = networkMonitor.isOnline.first()
                if (isOnline) {
                    val snapshot = exercisesCollection.document(exerciseId).get().await()
                    snapshot.toObject(ExerciseDto::class.java)?.let { dto ->
                        val exercise = dto.toDomain()
                        exerciseDao.insertExercise(exercise.toEntity())
                        Result.success(exercise)
                    } ?: Result.failure(Exception("Exercise not found"))
                } else {
                    Result.failure(Exception("Exercise not found offline"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getExercisesByWorkout(workoutId: String): Flow<List<Exercise>> {
        return flow {
            val localExercises = exerciseDao.getExercisesByWorkout(workoutId)
                .first()
                .map { it.toDomain() }

            emit(localExercises)

            val isOnline = networkMonitor.isOnline.first()
            if (isOnline) {
                try {
                    syncManager.startSync()

                    val remoteExercises = exercisesCollection
                        .whereEqualTo("workoutId", workoutId)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { doc ->
                            doc.toObject(ExerciseDto::class.java)?.toDomain()
                        }

                    remoteExercises.forEach { exercise ->
                        exerciseDao.insertExercise(exercise.toEntity())
                    }

                    val updatedLocal = exerciseDao.getExercisesByWorkout(workoutId)
                        .first()
                        .map { it.toDomain() }

                    if (updatedLocal != localExercises) {
                        emit(updatedLocal)
                    }
                } catch (_: Exception) {
                    // Continue com dados locais
                } finally {
                    syncManager.endSync()
                }
            }

            exerciseDao.getExercisesByWorkout(workoutId)
                .map { exercises -> exercises.map { it.toDomain() } }
                .collect { exercises ->
                    emit(exercises)
                }
        }.distinctUntilChanged()
    }

    override suspend fun uploadExerciseImage(exerciseId: String, imageUri: String): Result<String> {
        val isOnline = networkMonitor.isOnline.first()
        if (!isOnline) {
            return Result.failure(Exception("Cannot upload image offline"))
        }

        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            val uri = imageUri.toUri()
            val extension = getFileExtension(uri)

            val fileName = "${UUID.randomUUID()}$extension"
            val imageRef = storageRef.child("exercises/$userId/$fileName")

            val uploadTask = imageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileExtension(uri: Uri): String {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            when {
                mimeType?.contains("gif") == true -> ".gif"
                mimeType?.contains("png") == true -> ".png"
                mimeType?.contains("jpeg") == true -> ".jpg"
                mimeType?.contains("jpg") == true -> ".jpg"
                else -> {
                    val path = uri.path ?: ""
                    when {
                        path.endsWith(".gif", ignoreCase = true) -> ".gif"
                        path.endsWith(".png", ignoreCase = true) -> ".png"
                        path.endsWith(".jpeg", ignoreCase = true) -> ".jpg"
                        path.endsWith(".jpg", ignoreCase = true) -> ".jpg"
                        else -> ".jpg"
                    }
                }
            }
        } catch (_: Exception) {
            ".jpg"
        }
    }
}
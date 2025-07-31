package com.guicarneirodev.gympro.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.guicarneirodev.gympro.data.mapper.toDomain
import com.guicarneirodev.gympro.data.mapper.toDto
import com.guicarneirodev.gympro.data.remote.dto.ExerciseDto
import com.guicarneirodev.gympro.domain.model.Exercise
import com.guicarneirodev.gympro.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ExerciseRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val getCurrentUserId: () -> String?
) : ExerciseRepository {

    private val exercisesCollection = firestore.collection("exercises")
    private val storageRef = storage.reference

    override suspend fun createExercise(exercise: Exercise): Result<String> {
        return try {
            val documentRef = exercisesCollection.document()
            val exerciseWithId = exercise.copy(id = documentRef.id)
            documentRef.set(exerciseWithId.toDto()).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExercise(exercise: Exercise): Result<Unit> {
        return try {
            exercisesCollection.document(exercise.id)
                .set(exercise.toDto())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExercise(exerciseId: String): Result<Unit> {
        return try {
            val exercise = getExercise(exerciseId).getOrNull()
            exercise?.imageUrl?.let { url ->
                try {
                    storage.getReferenceFromUrl(url).delete().await()
                } catch (e: Exception) {

                }
            }

            exercisesCollection.document(exerciseId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExercise(exerciseId: String): Result<Exercise> {
        return try {
            val snapshot = exercisesCollection.document(exerciseId).get().await()
            snapshot.toObject(ExerciseDto::class.java)?.let {
                Result.success(it.toDomain())
            } ?: Result.failure(Exception("Exercise not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getExercisesByWorkout(workoutId: String): Flow<List<Exercise>> {
        return exercisesCollection
            .whereEqualTo("workoutId", workoutId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ExerciseDto::class.java)?.toDomain()
                }
            }
    }

    override suspend fun uploadExerciseImage(exerciseId: String, imageUri: String): Result<String> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            val fileName = "${UUID.randomUUID()}.jpg"
            val imageRef = storageRef.child("exercises/$userId/$fileName")

            val uploadTask = imageRef.putFile(Uri.parse(imageUri)).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
package com.guicarneirodev.gympro.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.gympro.domain.model.User
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.guicarneirodev.gympro.data.local.preferences.UserPreferencesManager

@OptIn(ExperimentalCoilApi::class)
class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val userPreferencesManager: UserPreferencesManager
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomainUser())
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { firebaseUser ->
                userPreferencesManager.setUserId(firebaseUser.uid)
                userPreferencesManager.updateLastSync()
                Result.success(firebaseUser.toDomainUser())
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                Result.success(it.toDomainUser())
            } ?: Result.failure(Exception("Failed to create user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                clearFirestoreCache()
                userPreferencesManager.clearAll()
                clearImageCache()
                auth.signOut()
                clearTempData()
            } catch (e: Exception) {
                e.printStackTrace()
                auth.signOut()
            }
        }
    }

    private suspend fun clearFirestoreCache() {
        try {
            firestore.clearPersistence().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clearImageCache() {
        imageLoader.memoryCache?.clear()

        imageLoader.diskCache?.clear()
    }

    private fun clearTempData() {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("temp_photo_") ||
                file.name.startsWith("workout_image_")) {
                file.delete()
            }
        }

        context.externalCacheDir?.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name == "image_cache") {
                file.deleteRecursively()
            }
        }
    }

    private fun FirebaseUser.toDomainUser(): User {
        return User(
            id = uid,
            email = email ?: "",
            displayName = displayName ?: "",
            photoUrl = photoUrl?.toString()
        )
    }
}
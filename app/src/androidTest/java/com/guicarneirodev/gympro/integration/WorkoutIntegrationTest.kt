package com.guicarneirodev.gympro.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.gympro.data.repository.AuthRepositoryImpl
import com.guicarneirodev.gympro.data.repository.WorkoutRepositoryImpl
import com.guicarneirodev.gympro.data.util.NetworkMonitor
import com.guicarneirodev.gympro.data.util.SyncManager
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WorkoutIntegrationTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val testEmail = "integration_test_${UUID.randomUUID()}@test.com"
    private val testPassword = "Test123456"
    private var testUserId: String? = null

    @Before
    fun setup() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        authRepository = AuthRepositoryImpl(
            auth = firebaseAuth,
            firestore = firestore,
            context = context,
            imageLoader = coil.ImageLoader(context),
            userPreferencesManager = com.guicarneirodev.gympro.data.local.preferences.UserPreferencesManager(context)
        )

        val result = authRepository.signUp(testEmail, testPassword)
        assertTrue("Failed to create test user", result.isSuccess)

        testUserId = firebaseAuth.currentUser?.uid
        assertNotNull("Test user ID should not be null", testUserId)

        val appDatabase = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.guicarneirodev.gympro.data.local.database.GymProDatabase::class.java
        ).build()

        workoutRepository = WorkoutRepositoryImpl(
            firestore = firestore,
            workoutDao = appDatabase.workoutDao(),
            networkMonitor = NetworkMonitor(context),
            syncManager = SyncManager()
        )
    }

    @After
    fun cleanup() = runBlocking {
        testUserId?.let { userId ->
            try {
                val workouts = firestore.collection("workouts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                workouts.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                firebaseAuth.currentUser?.delete()?.await()
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }

        firebaseAuth.signOut()
    }

    @Test
    fun testCompleteWorkoutFlow_CreateUpdateDelete() = runTest {
        // 1. CREATE
        val newWorkout = Workout(
            id = "",
            name = "Integration Test Workout",
            description = "Testing the complete flow",
            date = Timestamp(Date()),
            userId = testUserId!!
        )

        val createResult = workoutRepository.createWorkout(newWorkout)
        assertTrue("Workout creation should succeed", createResult.isSuccess)

        val workoutId = createResult.getOrNull()
        assertNotNull("Workout ID should not be null", workoutId)

        // 2. READ
        val workouts = workoutRepository.getWorkoutsByUser(testUserId!!).first()
        assertTrue("Should have at least one workout", workouts.isNotEmpty())

        val createdWorkout = workouts.find { it.id == workoutId }
        assertNotNull("Created workout should be in the list", createdWorkout)
        assertEquals("Integration Test Workout", createdWorkout?.name)
        assertEquals("Testing the complete flow", createdWorkout?.description)

        // 3. UPDATE
        val updatedWorkout = createdWorkout!!.copy(
            name = "Updated Integration Test",
            description = "Updated description"
        )

        val updateResult = workoutRepository.updateWorkout(updatedWorkout)
        assertTrue("Workout update should succeed", updateResult.isSuccess)

        val getResult = workoutRepository.getWorkout(workoutId!!)
        assertTrue("Should be able to get workout", getResult.isSuccess)

        val fetchedWorkout = getResult.getOrNull()
        assertEquals("Updated Integration Test", fetchedWorkout?.name)
        assertEquals("Updated description", fetchedWorkout?.description)

        // 4. DELETE
        val deleteResult = workoutRepository.deleteWorkout(workoutId)
        assertTrue("Workout deletion should succeed", deleteResult.isSuccess)

        val workoutsAfterDelete = workoutRepository.getWorkoutsByUser(testUserId!!).first()
        val deletedWorkout = workoutsAfterDelete.find { it.id == workoutId }
        assertNull("Deleted workout should not be in the list", deletedWorkout)
    }

    @Test
    fun testWorkoutListSynchronization() = runTest {
        val workouts = listOf(
            Workout(id = "", name = "Monday Workout", description = "Chest day", date = Timestamp.now(), userId = testUserId!!),
            Workout(id = "", name = "Wednesday Workout", description = "Back day", date = Timestamp.now(), userId = testUserId!!),
            Workout(id = "", name = "Friday Workout", description = "Leg day", date = Timestamp.now(), userId = testUserId!!)
        )

        val createdIds = mutableListOf<String>()

        workouts.forEach { workout ->
            val result = workoutRepository.createWorkout(workout)
            assertTrue("Workout creation should succeed", result.isSuccess)
            result.getOrNull()?.let { createdIds.add(it) }
        }

        val retrievedWorkouts = workoutRepository.getWorkoutsByUser(testUserId!!).first()
        assertEquals("Should have 3 workouts", 3, retrievedWorkouts.size)

        val workoutNames = retrievedWorkouts.map { it.name }.sorted()
        assertEquals(
            listOf("Friday Workout", "Monday Workout", "Wednesday Workout"),
            workoutNames
        )

        createdIds.forEach { id ->
            workoutRepository.deleteWorkout(id)
        }
    }

    @Test
    fun testAuthenticationFlow() = runTest {
        authRepository.signOut()

        val currentUserAfterSignOut = authRepository.getCurrentUser().first()
        assertNull("User should be null after sign out", currentUserAfterSignOut)

        val signInResult = authRepository.signIn(testEmail, testPassword)
        assertTrue("Sign in should succeed", signInResult.isSuccess)

        val currentUserAfterSignIn = authRepository.getCurrentUser().first()
        assertNotNull("User should not be null after sign in", currentUserAfterSignIn)
        assertEquals(testEmail, currentUserAfterSignIn?.email)
    }
}


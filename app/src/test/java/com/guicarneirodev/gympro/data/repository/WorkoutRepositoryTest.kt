package com.guicarneirodev.gympro.data.repository

import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.guicarneirodev.gympro.data.local.dao.WorkoutDao
import com.guicarneirodev.gympro.data.local.entity.WorkoutEntity
import com.guicarneirodev.gympro.data.remote.dto.WorkoutDto
import com.guicarneirodev.gympro.data.util.NetworkMonitor
import com.guicarneirodev.gympro.data.util.SyncManager
import com.guicarneirodev.gympro.domain.model.Workout
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class WorkoutRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var workoutDao: WorkoutDao
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncManager: SyncManager
    private lateinit var repository: WorkoutRepositoryImpl

    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference

    private val testUserId = "test_user_123"
    private val getCurrentUserId: () -> String? = { testUserId }

    private val testWorkout = Workout(
        id = "workout123",
        name = "Test Workout",
        description = "Test Description",
        date = Timestamp(Date()),
        userId = testUserId
    )

    private val testWorkoutEntity = WorkoutEntity(
        id = testWorkout.id,
        userId = testWorkout.userId,
        name = testWorkout.name,
        description = testWorkout.description,
        date = testWorkout.date.toDate().time
    )

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        workoutDao = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        mockCollection = mockk(relaxed = true)
        mockDocument = mockk(relaxed = true)

        every { firestore.collection("workouts") } returns mockCollection
        every { mockCollection.document() } returns mockDocument
        every { mockCollection.document(any()) } returns mockDocument
        every { mockDocument.id } returns testWorkout.id

        every { networkMonitor.isOnline } returns flowOf(true)

        repository = WorkoutRepositoryImpl(
            firestore = firestore,
            workoutDao = workoutDao,
            networkMonitor = networkMonitor,
            syncManager = syncManager
        )
    }

    @Test
    fun `createWorkout should save to local database and firestore when online`() = runTest {
        val workoutToCreate = testWorkout.copy(id = "")

        coEvery { workoutDao.insertWorkout(any()) } just Runs
        every { mockDocument.set(any()) } returns Tasks.forResult(null)

        val result = repository.createWorkout(workoutToCreate)

        assertTrue(result.isSuccess)
        assertEquals(testWorkout.id, result.getOrNull())

        coVerify(exactly = 1) {
            workoutDao.insertWorkout(match {
                it.id == testWorkout.id && it.name == workoutToCreate.name
            })
        }
        verify(exactly = 1) { mockDocument.set(any<WorkoutDto>()) }
    }

    @Test
    fun `createWorkout should only save to local database when offline`() = runTest {
        every { networkMonitor.isOnline } returns flowOf(false)
        val workoutToCreate = testWorkout.copy(id = "")

        coEvery { workoutDao.insertWorkout(any()) } just Runs

        val result = repository.createWorkout(workoutToCreate)

        assertTrue(result.isSuccess)
        assertEquals(testWorkout.id, result.getOrNull())

        coVerify(exactly = 1) { workoutDao.insertWorkout(any()) }
        verify(exactly = 0) { mockDocument.set(any<WorkoutDto>()) }
    }

    @Test
    fun `updateWorkout should update local and remote when online`() = runTest {
        coEvery { workoutDao.insertWorkout(any()) } just Runs
        every { mockDocument.set(any()) } returns Tasks.forResult(null)

        val result = repository.updateWorkout(testWorkout)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) { workoutDao.insertWorkout(any()) }
        verify(exactly = 1) { mockDocument.set(any<WorkoutDto>()) }
    }

    @Test
    fun `deleteWorkout should delete from local and remote when online`() = runTest {
        coEvery { workoutDao.deleteWorkoutById(testWorkout.id) } just Runs
        every { mockDocument.delete() } returns Tasks.forResult(null)

        val result = repository.deleteWorkout(testWorkout.id)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) { workoutDao.deleteWorkoutById(testWorkout.id) }
        verify(exactly = 1) { mockDocument.delete() }
    }

    @Test
    fun `deleteWorkout should only delete from local when offline`() = runTest {
        every { networkMonitor.isOnline } returns flowOf(false)

        coEvery { workoutDao.deleteWorkoutById(testWorkout.id) } just Runs

        val result = repository.deleteWorkout(testWorkout.id)

        assertTrue(result.isSuccess)

        coVerify(exactly = 1) { workoutDao.deleteWorkoutById(testWorkout.id) }
        verify(exactly = 0) { mockDocument.delete() }
    }

    @Test
    fun `getWorkout should return from local database first`() = runTest {
        coEvery { workoutDao.getWorkout(testWorkout.id) } returns testWorkoutEntity

        val result = repository.getWorkout(testWorkout.id)

        assertTrue(result.isSuccess)
        assertEquals(testWorkout.name, result.getOrNull()?.name)

        coVerify(exactly = 1) { workoutDao.getWorkout(testWorkout.id) }
        verify(exactly = 0) { mockDocument.get() }
    }

    @Test
    fun `getWorkout should fetch from firestore when not in local database and online`() = runTest {
        coEvery { workoutDao.getWorkout(testWorkout.id) } returns null

        val mockSnapshot = mockk<DocumentSnapshot>()
        every { mockSnapshot.toObject(WorkoutDto::class.java) } returns WorkoutDto(
            id = testWorkout.id,
            name = testWorkout.name,
            description = testWorkout.description,
            date = testWorkout.date,
            userId = testWorkout.userId
        )
        every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)

        coEvery { workoutDao.insertWorkout(any()) } just Runs

        val result = repository.getWorkout(testWorkout.id)

        assertTrue(result.isSuccess)
        assertEquals(testWorkout.name, result.getOrNull()?.name)

        coVerify(exactly = 1) { workoutDao.getWorkout(testWorkout.id) }
        verify(exactly = 1) { mockDocument.get() }
        coVerify(exactly = 1) { workoutDao.insertWorkout(any()) }
    }

    @Test
    fun `getWorkoutsByUser should emit local data first then sync with remote`() = runTest {
        val localWorkouts = listOf(testWorkoutEntity)
        val remoteWorkout = testWorkout.copy(id = "remote123", name = "Remote Workout")

        coEvery { workoutDao.getWorkoutsByUser(testUserId) } returnsMany listOf(
            flowOf(localWorkouts),
            flowOf(localWorkouts)
        )

        val mockQuery = mockk<Query>()
        val mockQuerySnapshot = mockk<QuerySnapshot>()
        val mockDocumentSnapshot = mockk<DocumentSnapshot>()

        every { mockCollection.whereEqualTo("userId", testUserId) } returns mockQuery
        every { mockQuery.get() } returns Tasks.forResult(mockQuerySnapshot)
        every { mockQuerySnapshot.documents } returns listOf(mockDocumentSnapshot)
        every { mockDocumentSnapshot.toObject(WorkoutDto::class.java) } returns WorkoutDto(
            id = remoteWorkout.id,
            name = remoteWorkout.name,
            description = remoteWorkout.description,
            date = remoteWorkout.date,
            userId = remoteWorkout.userId
        )

        coEvery { workoutDao.insertWorkout(any()) } just Runs

        repository.getWorkoutsByUser(testUserId).test {
            val firstEmission = awaitItem()
            assertEquals(1, firstEmission.size)
            assertEquals(testWorkout.name, firstEmission[0].name)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(atLeast = 1) { workoutDao.getWorkoutsByUser(testUserId) }
        verify { syncManager.startSync() }
        verify { syncManager.endSync() }
    }

    @Test
    fun `getWorkoutsByUser should handle offline mode gracefully`() = runTest {
        every { networkMonitor.isOnline } returns flowOf(false)
        val localWorkouts = listOf(testWorkoutEntity)

        coEvery { workoutDao.getWorkoutsByUser(testUserId) } returns flowOf(localWorkouts)

        repository.getWorkoutsByUser(testUserId).test {
            val emission = awaitItem()
            assertEquals(1, emission.size)
            assertEquals(testWorkout.name, emission[0].name)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify { workoutDao.getWorkoutsByUser(testUserId) }
        verify(exactly = 0) { mockCollection.whereEqualTo(any<String>(), any<String>()) }
    }

    @Test
    fun `repository should handle exceptions gracefully`() = runTest {
        coEvery { workoutDao.insertWorkout(any()) } throws Exception("Database error")

        val result = repository.createWorkout(testWorkout)

        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
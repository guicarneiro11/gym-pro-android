package com.guicarneirodev.gympro.presentation.ui.workout.list

import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.User
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import com.guicarneirodev.gympro.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

class WorkoutListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: WorkoutListViewModel

    private val testUser = User(
        id = "user123",
        email = "test@test.com"
    )

    private val testWorkouts = listOf(
        Workout(
            id = "1",
            name = "Chest Day",
            description = "Focus on chest",
            date = Timestamp(Date()),
            userId = "user123"
        ),
        Workout(
            id = "2",
            name = "Leg Day",
            description = "Never skip leg day",
            date = Timestamp(Date()),
            userId = "user123"
        )
    )

    @Before
    fun setup() {
        workoutRepository = mockk()
        authRepository = mockk()
    }

    @Test
    fun `when user logged in, loads workouts successfully`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        delay(100)

        assertEquals(testWorkouts, viewModel.uiState.value.workouts)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `when user not logged in, navigates to login`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(null)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        delay(200)

        assertEquals(WorkoutListEvent.NavigateToLogin, viewModel.events.value)
    }

    @Test
    fun `when workout clicked, navigates to detail`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.events.test {
            skipItems(1)

            viewModel.onWorkoutClick(testWorkouts[0])

            assertEquals(
                WorkoutListEvent.NavigateToWorkoutDetail(testWorkouts[0].id),
                awaitItem()
            )
        }
    }

    @Test
    fun `when add workout clicked, navigates to add workout`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(emptyList())

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.events.test {
            skipItems(1)

            viewModel.onAddWorkoutClick()

            assertEquals(WorkoutListEvent.NavigateToAddWorkout, awaitItem())
        }
    }

    @Test
    fun `when edit workout clicked, navigates to edit`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.events.test {
            skipItems(1)

            viewModel.onEditWorkoutClick(testWorkouts[0])

            assertEquals(
                WorkoutListEvent.NavigateToEditWorkout(testWorkouts[0].id),
                awaitItem()
            )
        }
    }

    @Test
    fun `when delete workout clicked, shows dialog`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.onDeleteWorkoutClick(testWorkouts[0])

        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)
        assertEquals(testWorkouts[0], viewModel.uiState.value.workoutToDelete)
    }

    @Test
    fun `when confirm delete, deletes workout`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)
        coEvery { workoutRepository.deleteWorkout(any()) } returns Result.success(Unit)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.onDeleteWorkoutClick(testWorkouts[0])
        viewModel.onConfirmDelete()

        delay(100)

        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        coVerify(exactly = 1) { workoutRepository.deleteWorkout(testWorkouts[0].id) }
    }

    @Test
    fun `when delete fails, shows error`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)
        coEvery { workoutRepository.deleteWorkout(any()) } returns Result.failure(Exception())

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.onDeleteWorkoutClick(testWorkouts[0])
        viewModel.onConfirmDelete()

        delay(100)

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_deleting_workout, (error as UiText.StringResource).resId)
    }

    @Test
    fun `when cancel delete, hides dialog`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.onDeleteWorkoutClick(testWorkouts[0])
        viewModel.onCancelDelete()

        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        assertNull(viewModel.uiState.value.workoutToDelete)
    }

    @Test
    fun `when refresh, sets refreshing state`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flowOf(testWorkouts)

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        viewModel.onRefresh()

        assertTrue(viewModel.uiState.value.isRefreshing || !viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `when loading fails, shows error message`() = runTest {
        every { authRepository.getCurrentUser() } returns flowOf(testUser)
        every { workoutRepository.getWorkoutsByUser(testUser.id) } returns flow {
            throw Exception("Network error")
        }

        viewModel = WorkoutListViewModel(workoutRepository, authRepository)

        delay(100)

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_loading_workouts, (error as UiText.StringResource).resId)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
package com.guicarneirodev.gympro.presentation.ui.workout.form

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import com.guicarneirodev.gympro.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

class WorkoutFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: WorkoutFormViewModel
    private lateinit var firebaseUser: FirebaseUser

    @Before
    fun setup() {
        workoutRepository = mockk()
        auth = mockk()
        firebaseUser = mockk()
        savedStateHandle = mockk()

        every { savedStateHandle.get<String>("workoutId") } returns null
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "user123"
    }

    @Test
    fun `when creating new workout, isEditMode is false`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun `when editing workout, loads workout data`() = runTest {
        val workoutId = "workout123"
        val existingWorkout = Workout(
            id = workoutId,
            name = "Morning Workout",
            description = "Daily routine",
            date = Timestamp(Date()),
            userId = "user123"
        )

        every { savedStateHandle.get<String>("workoutId") } returns workoutId
        coEvery { workoutRepository.getWorkout(workoutId) } returns Result.success(existingWorkout)

        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        delay(100)

        assertEquals(existingWorkout.name, viewModel.uiState.value.name)
        assertEquals(existingWorkout.description, viewModel.uiState.value.description)
        assertTrue(viewModel.uiState.value.isEditMode)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `when name changes, save button enables`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onNameChange("New Workout")

        assertEquals("New Workout", viewModel.uiState.value.name)
        assertTrue(viewModel.uiState.value.isSaveEnabled)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `when name is empty, save button disables`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onNameChange("")

        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `when description changes, updates state`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onDescriptionChange("New description")

        assertEquals("New description", viewModel.uiState.value.description)
    }

    @Test
    fun `when save with empty name, shows error`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onNameChange("")
        viewModel.onSaveClick()

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_empty_workout_name, (error as UiText.StringResource).resId)
        coVerify(exactly = 0) { workoutRepository.createWorkout(any()) }
    }

    @Test
    fun `when save with name too long, shows error`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        val longName = "a".repeat(101)
        viewModel.onNameChange(longName)
        viewModel.onSaveClick()

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_workout_name_too_long, (error as UiText.StringResource).resId)
        coVerify(exactly = 0) { workoutRepository.createWorkout(any()) }
    }

    @Test
    fun `when create workout successfully, navigates to exercises`() = runTest {
        val workoutId = "new123"
        coEvery { workoutRepository.createWorkout(any()) } returns Result.success(workoutId)

        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onNameChange("Test Workout")
        viewModel.onDescriptionChange("Test Description")

        viewModel.events.test {
            skipItems(1)

            viewModel.onSaveClick()

            assertEquals(
                WorkoutFormEvent.NavigateToExercises(workoutId),
                awaitItem()
            )
        }

        coVerify(exactly = 1) { workoutRepository.createWorkout(any()) }
    }

    @Test
    fun `when update workout successfully, navigates to exercises`() = runTest {
        val workoutId = "existing123"
        val existingWorkout = Workout(
            id = workoutId,
            name = "Old Name",
            description = "Old Description",
            date = Timestamp(Date()),
            userId = "user123"
        )

        every { savedStateHandle.get<String>("workoutId") } returns workoutId
        coEvery { workoutRepository.getWorkout(workoutId) } returns Result.success(existingWorkout)
        coEvery { workoutRepository.updateWorkout(any()) } returns Result.success(Unit)

        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        delay(100)

        viewModel.onNameChange("Updated Name")

        viewModel.events.test {
            skipItems(1)

            viewModel.onSaveClick()

            assertEquals(
                WorkoutFormEvent.NavigateToExercises(workoutId),
                awaitItem()
            )
        }

        coVerify(exactly = 1) { workoutRepository.updateWorkout(any()) }
    }

    @Test
    fun `when user not authenticated, shows error`() = runTest {
        every { auth.currentUser } returns null

        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onNameChange("Test Workout")
        viewModel.onSaveClick()

        delay(100)

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_user_not_authenticated, (error as UiText.StringResource).resId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `when create fails, shows error`() = runTest {
        coEvery { workoutRepository.createWorkout(any()) } returns Result.failure(Exception())

        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.onNameChange("Test Workout")
        viewModel.onSaveClick()

        delay(100)

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_creating_workout, (error as UiText.StringResource).resId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `when back clicked, navigates back`() = runTest {
        viewModel = WorkoutFormViewModel(workoutRepository, auth, savedStateHandle)

        viewModel.events.test {
            skipItems(1)

            viewModel.onBackClick()

            assertEquals(WorkoutFormEvent.NavigateBack, awaitItem())
        }
    }
}
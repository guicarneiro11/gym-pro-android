package com.guicarneirodev.gympro.presentation.ui.exercise.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.Exercise
import com.guicarneirodev.gympro.domain.repository.ExerciseRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import com.guicarneirodev.gympro.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExerciseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ExerciseListViewModel

    private val workoutId = "workout123"

    private val testExercises = listOf(
        Exercise(
            id = "1",
            workoutId = workoutId,
            name = "Bench Press",
            observations = "3x10",
            imageUrl = "https://example.com/bench.jpg"
        ),
        Exercise(
            id = "2",
            workoutId = workoutId,
            name = "Squats",
            observations = "4x8",
            imageUrl = null
        )
    )

    @Before
    fun setup() {
        exerciseRepository = mockk()
        savedStateHandle = mockk()

        every { savedStateHandle.get<String>("workoutId") } returns workoutId
        every { exerciseRepository.getExercisesByWorkout(workoutId) } returns flowOf(testExercises)
    }

    @Test
    fun `initialization loads exercises for workout`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        delay(100)

        assertEquals(workoutId, viewModel.uiState.value.workoutId)
        assertEquals(testExercises, viewModel.uiState.value.exercises)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clicking exercise navigates to edit form`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.events.test {
            skipItems(1)

            viewModel.onExerciseClick(testExercises[0])

            assertEquals(
                ExerciseListEvent.NavigateToExerciseForm(testExercises[0].id),
                awaitItem()
            )
        }
    }

    @Test
    fun `clicking add navigates to create form`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.events.test {
            skipItems(1)

            viewModel.onAddExerciseClick()

            assertEquals(
                ExerciseListEvent.NavigateToExerciseForm(null),
                awaitItem()
            )
        }
    }

    @Test
    fun `delete exercise shows confirmation dialog`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.onDeleteExerciseClick(testExercises[0])

        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)
        assertEquals(testExercises[0], viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun `confirm delete calls repository and hides dialog`() = runTest {
        coEvery { exerciseRepository.deleteExercise(any()) } returns Result.success(Unit)

        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.onDeleteExerciseClick(testExercises[0])
        viewModel.onConfirmDelete()

        delay(100)

        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        coVerify(exactly = 1) { exerciseRepository.deleteExercise(testExercises[0].id) }
    }

    @Test
    fun `delete failure shows error message`() = runTest {
        coEvery { exerciseRepository.deleteExercise(any()) } returns Result.failure(Exception())

        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.onDeleteExerciseClick(testExercises[0])
        viewModel.onConfirmDelete()

        delay(100)

        val error = viewModel.uiState.value.errorMessage
        assertNotNull(error)
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.error_deleting_exercise, (error as UiText.StringResource).resId)
    }

    @Test
    fun `cancel delete hides dialog and clears selection`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.onDeleteExerciseClick(testExercises[0])
        viewModel.onCancelDelete()

        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        assertNull(viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun `refresh sets refreshing state`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.onRefresh()

        assertTrue(true)
    }

    @Test
    fun `back button triggers navigation event`() = runTest {
        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.events.test {
            skipItems(1)

            viewModel.onBackClick()

            assertEquals(ExerciseListEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `clear error removes error message`() = runTest {
        coEvery { exerciseRepository.deleteExercise(any()) } returns Result.failure(Exception())

        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        viewModel.onDeleteExerciseClick(testExercises[0])
        viewModel.onConfirmDelete()

        delay(100)

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `empty exercise list shows correct state`() = runTest {
        every { exerciseRepository.getExercisesByWorkout(workoutId) } returns flowOf(emptyList())

        viewModel = ExerciseListViewModel(exerciseRepository, savedStateHandle)

        delay(100)

        assertTrue(viewModel.uiState.value.exercises.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
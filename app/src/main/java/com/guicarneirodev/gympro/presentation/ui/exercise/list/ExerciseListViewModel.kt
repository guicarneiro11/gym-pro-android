package com.guicarneirodev.gympro.presentation.ui.exercise.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.Exercise
import com.guicarneirodev.gympro.domain.repository.ExerciseRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isDeleteDialogVisible: Boolean = false,
    val exerciseToDelete: Exercise? = null,
    val workoutId: String = ""
)

sealed class ExerciseListEvent {
    data class NavigateToExerciseForm(val exerciseId: String? = null) : ExerciseListEvent()
    data object NavigateBack : ExerciseListEvent()
}

class ExerciseListViewModel(
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle["workoutId"]) {
        "Workout ID is required"
    }

    private val _uiState = MutableStateFlow(ExerciseListUiState(workoutId = workoutId))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ExerciseListEvent?>(null)
    val events = _events.asStateFlow()

    init {
        observeExercises()
    }

    private fun observeExercises() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            exerciseRepository.getExercisesByWorkout(workoutId)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.error_loading_exercises)
                        )
                    }
                }
                .collect { exercises ->
                    _uiState.update {
                        it.copy(
                            exercises = exercises,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onExerciseClick(exercise: Exercise) {
        _events.value = ExerciseListEvent.NavigateToExerciseForm(exercise.id)
    }

    fun onAddExerciseClick() {
        _events.value = ExerciseListEvent.NavigateToExerciseForm(null)
    }

    fun onDeleteExerciseClick(exercise: Exercise) {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = true,
                exerciseToDelete = exercise
            )
        }
    }

    fun onConfirmDelete() {
        val exercise = _uiState.value.exerciseToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleteDialogVisible = false) }

            exerciseRepository.deleteExercise(exercise.id)
                .onFailure {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.StringResource(R.string.error_deleting_exercise)
                        )
                    }
                }
        }
    }

    fun onCancelDelete() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                exerciseToDelete = null
            )
        }
    }

    fun onBackClick() {
        _events.value = ExerciseListEvent.NavigateBack
    }

    fun clearEvent() {
        _events.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
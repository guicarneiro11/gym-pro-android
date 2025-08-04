package com.guicarneirodev.gympro.presentation.ui.workout.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutFormUiState(
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val errorMessage: UiText? = null,
    val isEditMode: Boolean = false
)

sealed class WorkoutFormEvent {
    data object NavigateBack : WorkoutFormEvent()
    data class NavigateToExercises(val workoutId: String) : WorkoutFormEvent()
}

class WorkoutFormViewModel(
    private val workoutRepository: WorkoutRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String? = savedStateHandle["workoutId"]

    private val _uiState = MutableStateFlow(WorkoutFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableStateFlow<WorkoutFormEvent?>(null)
    val events = _events.asStateFlow()

    init {
        workoutId?.let { loadWorkout(it) }
    }

    private fun loadWorkout(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }

            workoutRepository.getWorkout(id)
                .onSuccess { workout ->
                    _uiState.update {
                        it.copy(
                            name = workout.name,
                            description = workout.description,
                            isLoading = false,
                            isSaveEnabled = workout.name.isNotBlank()
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.error_loading_workout)
                        )
                    }
                    _events.value = WorkoutFormEvent.NavigateBack
                }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                isSaveEnabled = name.isNotBlank(),
                errorMessage = null
            )
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update {
            it.copy(
                description = description,
                errorMessage = null
            )
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (!validateInputs(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = auth.currentUser?.uid ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.StringResource(R.string.error_user_not_authenticated)
                    )
                }
                return@launch
            }

            val workout = Workout(
                id = workoutId ?: "",
                name = state.name.trim(),
                description = state.description.trim(),
                date = Timestamp.now(),
                userId = userId
            )

            val result = if (workoutId != null) {
                workoutRepository.updateWorkout(workout)
                    .map { workoutId }
            } else {
                workoutRepository.createWorkout(workout)
            }

            result
                .onSuccess { id ->
                    _events.value = WorkoutFormEvent.NavigateToExercises(id)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(
                                if (workoutId != null) R.string.error_updating_workout
                                else R.string.error_creating_workout
                            )
                        )
                    }
                }
        }
    }

    fun onBackClick() {
        _events.value = WorkoutFormEvent.NavigateBack
    }

    fun clearEvent() {
        _events.value = null
    }

    private fun validateInputs(state: WorkoutFormUiState): Boolean {
        return when {
            state.name.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_empty_workout_name))
                }
                false
            }
            state.name.length > 100 -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_workout_name_too_long))
                }
                false
            }
            else -> true
        }
    }
}
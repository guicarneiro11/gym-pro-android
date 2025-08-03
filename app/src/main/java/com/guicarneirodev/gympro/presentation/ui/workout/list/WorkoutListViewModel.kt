package com.guicarneirodev.gympro.presentation.ui.workout.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.repository.AuthRepository
import com.guicarneirodev.gympro.domain.repository.WorkoutRepository
import com.guicarneirodev.gympro.presentation.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class WorkoutListUiState(
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isDeleteDialogVisible: Boolean = false,
    val workoutToDelete: Workout? = null,
    val isLogoutDialogVisible: Boolean = false
)

sealed class WorkoutListEvent {
    data class NavigateToWorkoutDetail(val workoutId: String) : WorkoutListEvent()
    data object NavigateToAddWorkout : WorkoutListEvent()
    data object NavigateToLogin : WorkoutListEvent()
}

class WorkoutListViewModel(
    private val workoutRepository: WorkoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutListUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableStateFlow<WorkoutListEvent?>(null)
    val events = _events.asStateFlow()

    init {
        observeWorkouts()
    }

    private fun observeWorkouts() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collectLatest { user ->
                if (user == null) {
                    _events.value = WorkoutListEvent.NavigateToLogin
                    return@collectLatest
                }

                _uiState.update { it.copy(isLoading = true) }

                workoutRepository.getWorkoutsByUser(user.id)
                    .catch { exception ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = UiText.StringResource(R.string.error_loading_workouts)
                            )
                        }
                    }
                    .collect { workouts ->
                        _uiState.update {
                            it.copy(
                                workouts = workouts,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
            }
        }
    }

    fun onWorkoutClick(workout: Workout) {
        _events.value = WorkoutListEvent.NavigateToWorkoutDetail(workout.id)
    }

    fun onAddWorkoutClick() {
        _events.value = WorkoutListEvent.NavigateToAddWorkout
    }

    fun onDeleteWorkoutClick(workout: Workout) {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = true,
                workoutToDelete = workout
            )
        }
    }

    fun onConfirmDelete() {
        val workout = _uiState.value.workoutToDelete ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleteDialogVisible = false) }

            workoutRepository.deleteWorkout(workout.id)
                .onFailure {
                    _uiState.update {
                        it.copy(
                            errorMessage = UiText.StringResource(R.string.error_deleting_workout)
                        )
                    }
                }
        }
    }

    fun onCancelDelete() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                workoutToDelete = null
            )
        }
    }

    fun clearEvent() {
        _events.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onLogoutClick() {
        _uiState.update { it.copy(isLogoutDialogVisible = true) }
    }

    fun onConfirmLogout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLogoutDialogVisible = false) }
            authRepository.signOut()
        }
    }

    fun onCancelLogout() {
        _uiState.update { it.copy(isLogoutDialogVisible = false) }
    }
}
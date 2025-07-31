package com.guicarneirodev.gympro.presentation.ui.exercise.form

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
import kotlinx.coroutines.launch

data class ExerciseFormUiState(
    val name: String = "",
    val observations: String = "",
    val imageUrl: String? = null,
    val localImageUri: String? = null,
    val isLoading: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isUploadingImage: Boolean = false,
    val errorMessage: UiText? = null,
    val isEditMode: Boolean = false,
    val showImageOptions: Boolean = false
)

sealed class ExerciseFormEvent {
    data object NavigateBack : ExerciseFormEvent()
    data object OpenCamera : ExerciseFormEvent()
    data object OpenGallery : ExerciseFormEvent()
    data object RequestCameraPermission : ExerciseFormEvent()
    data object RequestGalleryPermission : ExerciseFormEvent()
}

class ExerciseFormViewModel(
    private val exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle["workoutId"]) {
        "Workout ID is required"
    }
    private val exerciseId: String? = savedStateHandle["exerciseId"]

    private val _uiState = MutableStateFlow(ExerciseFormUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableStateFlow<ExerciseFormEvent?>(null)
    val events = _events.asStateFlow()

    init {
        exerciseId?.let { loadExercise(it) }
    }

    private fun loadExercise(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }

            exerciseRepository.getExercise(id)
                .onSuccess { exercise ->
                    _uiState.update {
                        it.copy(
                            name = exercise.name,
                            observations = exercise.observations,
                            imageUrl = exercise.imageUrl,
                            isLoading = false,
                            isSaveEnabled = exercise.name.isNotBlank()
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(R.string.error_loading_exercise)
                        )
                    }
                    _events.value = ExerciseFormEvent.NavigateBack
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

    fun onObservationsChange(observations: String) {
        _uiState.update {
            it.copy(
                observations = observations,
                errorMessage = null
            )
        }
    }

    fun onAddImageClick() {
        _uiState.update { it.copy(showImageOptions = true) }
    }

    fun onCameraOptionClick() {
        _uiState.update { it.copy(showImageOptions = false) }
        _events.value = ExerciseFormEvent.RequestCameraPermission
    }

    fun onGalleryOptionClick() {
        _uiState.update { it.copy(showImageOptions = false) }
        _events.value = ExerciseFormEvent.RequestGalleryPermission
    }

    fun onImageCaptured(uri: String) {
        _uiState.update {
            it.copy(
                localImageUri = uri,
                imageUrl = null
            )
        }
    }

    fun onRemoveImageClick() {
        _uiState.update {
            it.copy(
                localImageUri = null,
                imageUrl = null
            )
        }
    }

    fun onDismissImageOptions() {
        _uiState.update { it.copy(showImageOptions = false) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (!validateInputs(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val imageUrl = if (state.localImageUri != null) {
                uploadImage(state.localImageUri)
            } else {
                state.imageUrl
            }

            if (state.localImageUri != null && imageUrl == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UiText.StringResource(R.string.error_uploading_image)
                    )
                }
                return@launch
            }

            val exercise = Exercise(
                id = exerciseId ?: "",
                workoutId = workoutId,
                name = state.name.trim(),
                observations = state.observations.trim(),
                imageUrl = imageUrl
            )

            val result = if (exerciseId != null) {
                exerciseRepository.updateExercise(exercise)
            } else {
                exerciseRepository.createExercise(exercise).map { Unit }
            }

            result
                .onSuccess {
                    _events.value = ExerciseFormEvent.NavigateBack
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResource(
                                if (exerciseId != null) R.string.error_updating_exercise
                                else R.string.error_creating_exercise
                            )
                        )
                    }
                }
        }
    }

    private suspend fun uploadImage(uri: String): String? {
        _uiState.update { it.copy(isUploadingImage = true) }

        return exerciseRepository.uploadExerciseImage(
            exerciseId = exerciseId ?: "",
            imageUri = uri
        ).fold(
            onSuccess = { url ->
                _uiState.update { it.copy(isUploadingImage = false) }
                url
            },
            onFailure = {
                _uiState.update { it.copy(isUploadingImage = false) }
                null
            }
        )
    }

    fun onBackClick() {
        _events.value = ExerciseFormEvent.NavigateBack
    }

    fun clearEvent() {
        _events.value = null
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun validateInputs(state: ExerciseFormUiState): Boolean {
        return when {
            state.name.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_empty_exercise_name))
                }
                false
            }
            state.name.length > 100 -> {
                _uiState.update {
                    it.copy(errorMessage = UiText.StringResource(R.string.error_exercise_name_too_long))
                }
                false
            }
            else -> true
        }
    }
}
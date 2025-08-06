package com.guicarneirodev.gympro.presentation.ui.workout.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guicarneirodev.gympro.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExercises: (String) -> Unit,
    viewModel: WorkoutFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val maxNameLength = 100
    val maxDescriptionLength = 200

    LaunchedEffect(events) {
        when (val event = events) {
            WorkoutFormEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.clearEvent()
            }
            is WorkoutFormEvent.NavigateToExercises -> {
                onNavigateToExercises(event.workoutId)
                viewModel.clearEvent()
            }
            null -> Unit
        }
    }

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isEditMode) R.string.workout_edit_title
                            else R.string.workout_add_title
                        ),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::onBackClick,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::onSaveClick,
                        enabled = uiState.isSaveEnabled && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { newValue ->
                    if (newValue.length <= maxNameLength) {
                        viewModel.onNameChange(newValue)
                    }
                },
                label = { Text(stringResource(R.string.workout_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = uiState.errorMessage != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_workout),
                        contentDescription = null
                    )
                },
                supportingText = {
                    Text(
                        text = "${uiState.name.length}/$maxNameLength",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { newValue ->
                    if (newValue.length <= maxDescriptionLength) {
                        viewModel.onDescriptionChange(newValue)
                    }
                },
                label = { Text(stringResource(R.string.workout_description_hint)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (uiState.isSaveEnabled) {
                            viewModel.onSaveClick()
                        }
                    }
                ),
                minLines = 3,
                maxLines = 5,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_description),
                        contentDescription = null
                    )
                },
                supportingText = {
                    Text(
                        text = "${uiState.description.length}/$maxDescriptionLength",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            if (uiState.isEditMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.workout_edit_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_error),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error.asString(context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
package com.guicarneirodev.gympro.presentation.ui.exercise.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guicarneirodev.gympro.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(events) {
        when (val event = events) {
            ExerciseFormEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.clearEvent()
            }
            ExerciseFormEvent.OpenCamera -> {
                // TODO: Implement camera
                viewModel.clearEvent()
            }
            ExerciseFormEvent.OpenGallery -> {
                // TODO: Implement gallery
                viewModel.clearEvent()
            }
            ExerciseFormEvent.RequestCameraPermission -> {
                // TODO: Request camera permission
                viewModel.clearEvent()
            }
            ExerciseFormEvent.RequestGalleryPermission -> {
                // TODO: Request gallery permission
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
                            if (uiState.isEditMode) R.string.exercise_edit_title
                            else R.string.exercise_add_title
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !uiState.isLoading) { viewModel.onAddImageClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.localImageUri != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uiState.localImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    uiState.imageUrl != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uiState.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add_photo),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.exercise_add_image),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (uiState.isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                if ((uiState.localImageUri != null || uiState.imageUrl != null) && !uiState.isLoading) {
                    IconButton(
                        onClick = viewModel::onRemoveImageClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.exercise_name_hint)) },
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
                        painter = painterResource(R.drawable.ic_exercise),
                        contentDescription = null
                    )
                }
            )

            OutlinedTextField(
                value = uiState.observations,
                onValueChange = viewModel::onObservationsChange,
                label = { Text(stringResource(R.string.exercise_observations_hint)) },
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
                        painter = painterResource(R.drawable.ic_notes),
                        contentDescription = null
                    )
                }
            )

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

    if (uiState.showImageOptions) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissImageOptions
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.image_option_camera)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { viewModel.onCameraOptionClick() }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.image_option_gallery)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_photo_library),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { viewModel.onGalleryOptionClick() }
                )
                if (uiState.imageUrl != null || uiState.localImageUri != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.image_option_remove)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = ListItemDefaults.colors(
                            headlineColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.clickable {
                            viewModel.onRemoveImageClick()
                            viewModel.onDismissImageOptions()
                        }
                    )
                }
            }
        }
    }
}
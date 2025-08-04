package com.guicarneirodev.gympro.presentation.ui.exercise.form

import android.Manifest
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.presentation.ui.camera.CameraScreen
import com.guicarneirodev.gympro.presentation.ui.components.ImageWithLoading
import com.guicarneirodev.gympro.presentation.ui.gallery.GalleryPicker
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ExerciseFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val maxObservationsLength = 200

    var showCamera by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(events) {
        when (events) {
            ExerciseFormEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.clearEvent()
            }
            ExerciseFormEvent.OpenCamera -> {
                showCamera = true
                viewModel.clearEvent()
            }
            ExerciseFormEvent.OpenGallery -> {
                showGallery = true
                viewModel.clearEvent()
            }
            ExerciseFormEvent.RequestCameraPermission -> {
                if (cameraPermission.status.isGranted) {
                    showCamera = true
                } else {
                    cameraPermission.launchPermissionRequest()
                }
                viewModel.clearEvent()
            }
            ExerciseFormEvent.RequestGalleryPermission -> {
                showGallery = true
                viewModel.clearEvent()
            }
            null -> Unit
        }
    }

    LaunchedEffect(cameraPermission.status) {
        if (cameraPermission.status.isGranted &&
            events == ExerciseFormEvent.RequestCameraPermission) {
            showCamera = true
        }
    }

    if (showCamera) {
        CameraScreen(
            onImageCaptured = { path ->
                viewModel.onImageCaptured(path)
                showCamera = false
            },
            onError = {
                fileError = it.message ?: "Camera error"
                showCamera = false
            },
            onBack = { showCamera = false }
        )
        return
    }

    if (showGallery) {
        GalleryPicker(
            onImageSelected = { uri ->
                viewModel.onImageCaptured(uri.toString())
                showGallery = false
            },
            onDismiss = { showGallery = false },
            onError = { error ->
                fileError = error
                showGallery = false
            }
        )
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
        },
        snackbarHost = {
            fileError?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { fileError = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }

                LaunchedEffect(error) {
                    delay(3000)
                    fileError = null
                }
            }
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
                var isImageLoading by remember { mutableStateOf(false) }

                when {
                    uiState.localImageUri != null -> {
                        ImageWithLoading(
                            imageUrl = uiState.localImageUri,
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.large,
                            contentScale = ContentScale.Crop
                        )
                    }
                    uiState.imageUrl != null -> {
                        ImageWithLoading(
                            imageUrl = uiState.imageUrl,
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.large,
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

                if (uiState.isUploadingImage || isImageLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            if (uiState.isUploadingImage) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.uploading_image),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
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
                onValueChange = { newValue ->
                    if (newValue.length <= maxObservationsLength) {
                        viewModel.onObservationsChange(newValue)
                    }
                },
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
                },
                supportingText = {
                    Text(
                        text = "${uiState.observations.length}/$maxObservationsLength",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
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
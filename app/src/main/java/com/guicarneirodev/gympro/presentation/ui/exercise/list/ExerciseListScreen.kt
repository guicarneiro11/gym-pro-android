package com.guicarneirodev.gympro.presentation.ui.exercise.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.domain.model.Exercise
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.graphics.Color
import com.guicarneirodev.gympro.data.util.SyncManager
import com.guicarneirodev.gympro.presentation.ui.components.ExerciseListSkeleton
import com.guicarneirodev.gympro.presentation.ui.components.ImageWithLoading
import com.guicarneirodev.gympro.presentation.ui.components.SyncIndicator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onNavigateToExercise: (String) -> Unit,
    onNavigateToAddExercise: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val syncManager: SyncManager = koinInject()
    val isSyncing by syncManager.isSyncing.collectAsStateWithLifecycle()

    val hasInitialLoadCompleted = remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !hasInitialLoadCompleted.value) {
            hasInitialLoadCompleted.value = true
        }
    }

    LaunchedEffect(events) {
        when (val event = events) {
            is ExerciseListEvent.NavigateToExerciseForm -> {
                if (event.exerciseId != null) {
                    onNavigateToExercise(event.exerciseId)
                } else {
                    onNavigateToAddExercise()
                }
                viewModel.clearEvent()
            }
            ExerciseListEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.clearEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.exercises_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::onBackClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = "Navigate back"
                            )
                        }
                    }
                )
                SyncIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    isSyncing = isSyncing
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddExerciseClick() },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.exercise_add_button)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && !hasInitialLoadCompleted.value -> {
                        ExerciseListSkeleton()
                    }
                    uiState.exercises.isEmpty() && hasInitialLoadCompleted.value -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        ExerciseList(
                            exercises = uiState.exercises,
                            onExerciseClick = viewModel::onExerciseClick,
                            onDeleteClick = viewModel::onDeleteExerciseClick,
                            onMoveUp = viewModel::onMoveExerciseUp,
                            onMoveDown = viewModel::onMoveExerciseDown
                        )
                    }
                }

                uiState.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = viewModel::clearError) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text(error.asString(context))
                    }
                }
            }
        }
    }

    if (uiState.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::onCancelDelete,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = stringResource(R.string.exercise_delete_confirmation))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.exercise_delete_message,
                        uiState.exerciseToDelete?.name ?: ""
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCancelDelete) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_exercise),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.exercise_empty_state),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExerciseList(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    onDeleteClick: (Exercise) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = exercises,
            key = { _, item -> item.id }
        ) { index, exercise ->
            ExerciseItem(
                exercise = exercise,
                position = index,
                totalItems = exercises.size,
                onClick = { onExerciseClick(exercise) },
                onDeleteClick = { onDeleteClick(exercise) },
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseItem(
    exercise: Exercise,
    position: Int,
    totalItems: Int,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteClick()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val alignment = Alignment.CenterEnd
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        content = {
            Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (exercise.imageUrl != null) {
                        ImageWithLoading(
                            imageUrl = exercise.imageUrl,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_exercise),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (exercise.observations.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exercise.observations,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column {
                        IconButton(
                            onClick = onMoveUp,
                            enabled = position > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_up),
                                contentDescription = "Move up",
                                tint = if (position > 0)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }

                        IconButton(
                            onClick = onMoveDown,
                            enabled = position < totalItems - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_down),
                                contentDescription = "Move down",
                                tint = if (position < totalItems - 1)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit),
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false
    )
}
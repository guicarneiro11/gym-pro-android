package com.guicarneirodev.gympro.presentation.ui.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.guicarneirodev.gympro.R

@Composable
fun GalleryPicker(
    onImageSelected: (Uri) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (validateFile(context, it, onError)) {
                onImageSelected(it)
            }
        } ?: onDismiss()
    }

    LaunchedEffect(Unit) {
        launcher.launch("image/*")
    }
}

@SuppressLint("DefaultLocale")
private fun validateFile(
    context: Context,
    uri: Uri,
    onError: (String) -> Unit
): Boolean {
    val maxSizeBytes = 3 * 1024 * 1024

    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val size = inputStream.available()

            if (size > maxSizeBytes) {
                val sizeMB = size / (1024.0 * 1024.0)
                onError(context.getString(R.string.error_file_too_large, String.format("%.1f", sizeMB)))
                false
            } else {
                true
            }
        } ?: false
    } catch (_: Exception) {
        onError(context.getString(R.string.error_reading_file))
        false
    }
}
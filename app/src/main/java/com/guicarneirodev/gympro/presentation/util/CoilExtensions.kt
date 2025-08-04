package com.guicarneirodev.gympro.presentation.util

import coil.ImageLoader
import androidx.compose.runtime.Composable
import org.koin.compose.koinInject

@Composable
fun rememberImageLoader(): ImageLoader = koinInject()
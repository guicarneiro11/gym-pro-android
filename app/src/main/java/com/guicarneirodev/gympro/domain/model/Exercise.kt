package com.guicarneirodev.gympro.domain.model

data class Exercise(
    val id: String = "",
    val workoutId: String = "",
    val name: String = "",
    val imageUrl: String? = null,
    val observations: String = ""
)
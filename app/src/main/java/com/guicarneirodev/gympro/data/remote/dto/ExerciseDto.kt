package com.guicarneirodev.gympro.data.remote.dto

import com.google.firebase.firestore.PropertyName

data class ExerciseDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("workoutId") val workoutId: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("imageUrl") val imageUrl: String? = null,
    @PropertyName("observations") val observations: String = ""
)
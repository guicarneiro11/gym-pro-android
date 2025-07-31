package com.guicarneirodev.gympro.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class WorkoutDto(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("date") val date: Timestamp = Timestamp.now(),
    @PropertyName("userId") val userId: String = ""
)
package com.guicarneirodev.gympro.domain.model

import com.google.firebase.Timestamp

data class Workout(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val date: Timestamp = Timestamp.now(),
    val userId: String = ""
)
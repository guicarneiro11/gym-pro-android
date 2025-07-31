package com.guicarneirodev.gympro.data.mapper

import com.guicarneirodev.gympro.data.remote.dto.ExerciseDto
import com.guicarneirodev.gympro.domain.model.Exercise

fun ExerciseDto.toDomain(): Exercise {
    return Exercise(
        id = id,
        workoutId = workoutId,
        name = name,
        imageUrl = imageUrl,
        observations = observations
    )
}

fun Exercise.toDto(): ExerciseDto {
    return ExerciseDto(
        id = id,
        workoutId = workoutId,
        name = name,
        imageUrl = imageUrl,
        observations = observations
    )
}
package com.guicarneirodev.gympro.data.mapper

import com.guicarneirodev.gympro.data.remote.dto.WorkoutDto
import com.guicarneirodev.gympro.domain.model.Workout

fun WorkoutDto.toDomain(): Workout {
    return Workout(
        id = id,
        name = name,
        description = description,
        date = date,
        userId = userId
    )
}

fun Workout.toDto(): WorkoutDto {
    return WorkoutDto(
        id = id,
        name = name,
        description = description,
        date = date,
        userId = userId
    )
}
package com.guicarneirodev.gympro.data.mapper

import com.guicarneirodev.gympro.data.local.entity.WorkoutEntity
import com.guicarneirodev.gympro.data.local.entity.ExerciseEntity
import com.guicarneirodev.gympro.domain.model.Workout
import com.guicarneirodev.gympro.domain.model.Exercise
import com.google.firebase.Timestamp
import java.util.Date

fun WorkoutEntity.toDomain(): Workout {
    return Workout(
        id = id,
        userId = userId,
        name = name,
        description = description,
        date = Timestamp(Date(date))
    )
}

fun Workout.toEntity(): WorkoutEntity {
    return WorkoutEntity(
        id = id,
        userId = userId,
        name = name,
        description = description,
        date = date.toDate().time
    )
}

fun ExerciseEntity.toDomain(): Exercise {
    return Exercise(
        id = id,
        workoutId = workoutId,
        name = name,
        observations = observations,
        imageUrl = imageUrl,
        position = position
    )
}

fun Exercise.toEntity(): ExerciseEntity {
    return ExerciseEntity(
        id = id,
        workoutId = workoutId,
        name = name,
        observations = observations,
        imageUrl = imageUrl,
        position = position
    )
}
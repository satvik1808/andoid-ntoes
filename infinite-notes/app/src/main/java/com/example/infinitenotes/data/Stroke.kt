package com.example.infinitenotes.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Float, val y: Float)

@Serializable
data class Stroke(
    val points: List<Point>,
    val color: Long = Color.Black.value.toLong(),
    val width: Float = 5f
)

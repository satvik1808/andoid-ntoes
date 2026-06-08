package com.example.infinitenotes.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class Point(val x: Float, val y: Float, val pressure: Float = 1.0f)

@Serializable
data class Stroke(
    val points: List<Point>,
    val color: Long = Color.Black.value.toLong(),
    val width: Float = 5f
)

fun catmullRom(p0: Point, p1: Point, p2: Point, p3: Point, t: Float): Point {
    val t2 = t * t
    val t3 = t2 * t
    
    val x = 0.5f * (
        (2f * p1.x) +
        (-p0.x + p2.x) * t +
        (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
        (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3
    )
    
    val y = 0.5f * (
        (2f * p1.y) +
        (-p0.y + p2.y) * t +
        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
    )
    
    val pressure = 0.5f * (
        (2f * p1.pressure) +
        (-p0.pressure + p2.pressure) * t +
        (2f * p0.pressure - 5f * p1.pressure + 4f * p2.pressure - p3.pressure) * t2 +
        (-p0.pressure + 3f * p1.pressure - 3f * p2.pressure + p3.pressure) * t3
    )

    return Point(x, y, pressure)
}

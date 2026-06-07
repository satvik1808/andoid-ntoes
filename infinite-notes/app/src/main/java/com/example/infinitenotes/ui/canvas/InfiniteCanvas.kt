package com.example.infinitenotes.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.infinitenotes.data.Point
import com.example.infinitenotes.data.Stroke as DataStroke

@Composable
fun InfiniteCanvas(
    strokes: List<DataStroke>,
    onStrokesChanged: (List<DataStroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.1f, 10f)
        offset += offsetChange
    }

    var currentStrokePoints by remember { mutableStateOf<List<Point>>(emptyList()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val canvasX = (startOffset.x - offset.x) / scale
                        val canvasY = (startOffset.y - offset.y) / scale
                        currentStrokePoints = listOf(Point(canvasX, canvasY))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val canvasX = (change.position.x - offset.x) / scale
                        val canvasY = (change.position.y - offset.y) / scale
                        currentStrokePoints = currentStrokePoints + Point(canvasX, canvasY)
                    },
                    onDragEnd = {
                        if (currentStrokePoints.isNotEmpty()) {
                            val newStroke = DataStroke(points = currentStrokePoints)
                            onStrokesChanged(strokes + newStroke)
                            currentStrokePoints = emptyList()
                        }
                    },
                    onDragCancel = {
                        currentStrokePoints = emptyList()
                    }
                )
            }
    ) {
        val allStrokes = if (currentStrokePoints.isNotEmpty()) {
            strokes + DataStroke(points = currentStrokePoints)
        } else {
            strokes
        }

        allStrokes.forEach { dataStroke ->
            if (dataStroke.points.isEmpty()) return@forEach
            
            val path = Path().apply {
                val start = dataStroke.points.first()
                moveTo(
                    start.x * scale + offset.x,
                    start.y * scale + offset.y
                )
                for (i in 1 until dataStroke.points.size) {
                    val point = dataStroke.points[i]
                    lineTo(
                        point.x * scale + offset.x,
                        point.y * scale + offset.y
                    )
                }
            }

            drawPath(
                path = path,
                color = Color(dataStroke.color.toULong()),
                style = Stroke(
                    width = dataStroke.width * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

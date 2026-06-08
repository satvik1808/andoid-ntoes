package com.example.infinitenotes.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.example.infinitenotes.data.Point
import com.example.infinitenotes.data.Stroke as DataStroke

@Composable
fun InfiniteCanvas(
    strokes: List<DataStroke>,
    onStrokesChanged: (List<DataStroke>) -> Unit,
    currentPenColor: Color = Color.Black,
    currentPenWidth: Float = 5f,
    allowFingerDrawing: Boolean = true,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }


    var currentStrokePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    
    val currentStrokes by rememberUpdatedState(strokes)
    val currentColor by rememberUpdatedState(currentPenColor)
    val currentWidth by rememberUpdatedState(currentPenWidth)
    val currentAllowFinger by rememberUpdatedState(allowFingerDrawing)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(0.1f, 10f)
                    val scaleFactor = newScale / oldScale
                    offset = centroid - (centroid - offset) * scaleFactor + pan
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDrawingGesture = false
                    
                    if (down.type == PointerType.Stylus || currentAllowFinger) {
                        isDrawingGesture = true
                        val canvasX = (down.position.x - offset.x) / scale
                        val canvasY = (down.position.y - offset.y) / scale
                        currentStrokePoints = listOf(Point(canvasX, canvasY, down.pressure))
                    } else {
                        currentStrokePoints = emptyList()
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        
                        // If 2 or more fingers touch the screen, it's a pan/zoom gesture, not drawing.
                        if (pressedChanges.size > 1) {
                            isDrawingGesture = false
                            currentStrokePoints = emptyList()
                        }

                        if (isDrawingGesture) {
                            val ptr = event.changes.firstOrNull { it.id == down.id }
                            if (ptr != null && ptr.pressed) {
                                if (ptr.positionChanged()) {
                                    ptr.consume()
                                }
                                
                                val newPoints = mutableListOf<Point>()
                                // Capture all high-frequency hardware points that were batched
                                ptr.historical.forEach { hist ->
                                    val hX = (hist.position.x - offset.x) / scale
                                    val hY = (hist.position.y - offset.y) / scale
                                    newPoints.add(Point(hX, hY, ptr.pressure))
                                }
                                
                                val canvasX = (ptr.position.x - offset.x) / scale
                                val canvasY = (ptr.position.y - offset.y) / scale
                                newPoints.add(Point(canvasX, canvasY, ptr.pressure))
                                
                                currentStrokePoints = currentStrokePoints + newPoints
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (currentStrokePoints.isNotEmpty()) {
                        val newStroke = DataStroke(
                            points = currentStrokePoints,
                            color = currentColor.value.toLong(),
                            width = currentWidth
                        )
                        onStrokesChanged(currentStrokes + newStroke)
                        currentStrokePoints = emptyList()
                    }
                }
            }
    ) {
        val allStrokes = if (currentStrokePoints.isNotEmpty()) {
            strokes + DataStroke(
                points = currentStrokePoints,
                color = currentPenColor.value.toLong(),
                width = currentPenWidth
            )
        } else {
            strokes
        }

        allStrokes.forEach { dataStroke ->
            if (dataStroke.points.isEmpty()) return@forEach
            
            if (dataStroke.points.size == 1) {
                val p = dataStroke.points.first()
                drawCircle(
                    color = Color(dataStroke.color.toULong()),
                    radius = (dataStroke.width * p.pressure * scale) / 2f,
                    center = Offset(p.x * scale + offset.x, p.y * scale + offset.y)
                )
                return@forEach
            }
            
            for (i in 0 until dataStroke.points.size - 1) {
                val p1 = dataStroke.points[i]
                val p2 = dataStroke.points[i + 1]
                val avgPressure = (p1.pressure + p2.pressure) / 2f
                
                drawLine(
                    color = Color(dataStroke.color.toULong()),
                    start = Offset(p1.x * scale + offset.x, p1.y * scale + offset.y),
                    end = Offset(p2.x * scale + offset.x, p2.y * scale + offset.y),
                    strokeWidth = dataStroke.width * avgPressure * scale,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

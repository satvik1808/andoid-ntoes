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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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

    val currentStrokePoints = remember { mutableStateListOf<Point>() }
    
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
                        currentStrokePoints.clear()
                        currentStrokePoints.add(Point(canvasX, canvasY, down.pressure))
                    } else {
                        currentStrokePoints.clear()
                    }

                    do {
                        val event = awaitPointerEvent()
                        val pressedChanges = event.changes.filter { it.pressed }
                        
                        // If 2 or more fingers touch the screen, it's a pan/zoom gesture, not drawing.
                        if (pressedChanges.size > 1) {
                            isDrawingGesture = false
                            currentStrokePoints.clear()
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
                                
                                currentStrokePoints.addAll(newPoints)
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (currentStrokePoints.isNotEmpty()) {
                        val newStroke = DataStroke(
                            points = currentStrokePoints.toList(),
                            color = currentColor.value.toLong(),
                            width = currentWidth
                        )
                        onStrokesChanged(currentStrokes + newStroke)
                        currentStrokePoints.clear()
                    }
                }
            }
    ) {
        val nativePaint = android.graphics.Paint().apply {
            strokeCap = android.graphics.Paint.Cap.ROUND
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
        }

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            
            val visibleLeft = -offset.x / scale
            val visibleTop = -offset.y / scale
            val visibleRight = (size.width - offset.x) / scale
            val visibleBottom = (size.height - offset.y) / scale
            
            nativeCanvas.save()
            nativeCanvas.translate(offset.x, offset.y)
            nativeCanvas.scale(scale, scale)

            strokes.forEach { dataStroke ->
                if (dataStroke.points.isEmpty()) return@forEach
                
                val bounds = dataStroke.bounds
                if (bounds[2] < visibleLeft || bounds[0] > visibleRight ||
                    bounds[3] < visibleTop || bounds[1] > visibleBottom) {
                    return@forEach
                }
                
                nativePaint.color = Color(dataStroke.color.toULong()).toArgb()
                
                if (dataStroke.points.size == 1) {
                    val p = dataStroke.points.first()
                    nativePaint.style = android.graphics.Paint.Style.FILL
                    nativeCanvas.drawCircle(p.x, p.y, (dataStroke.width * p.pressure) / 2f, nativePaint)
                    nativePaint.style = android.graphics.Paint.Style.STROKE
                    return@forEach
                }
                
                for (i in 0 until dataStroke.points.size - 1) {
                    val p1 = dataStroke.points[i]
                    val p2 = dataStroke.points[i + 1]
                    val avgPressure = (p1.pressure + p2.pressure) / 2f
                    nativePaint.strokeWidth = dataStroke.width * avgPressure
                    nativeCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, nativePaint)
                }
            }

            if (currentStrokePoints.isNotEmpty()) {
                nativePaint.color = Color(currentPenColor.value.toLong().toULong()).toArgb()
                if (currentStrokePoints.size == 1) {
                    val p = currentStrokePoints.first()
                    nativePaint.style = android.graphics.Paint.Style.FILL
                    nativeCanvas.drawCircle(p.x, p.y, (currentPenWidth * p.pressure) / 2f, nativePaint)
                    nativePaint.style = android.graphics.Paint.Style.STROKE
                } else {
                    for (i in 0 until currentStrokePoints.size - 1) {
                        val p1 = currentStrokePoints[i]
                        val p2 = currentStrokePoints[i + 1]
                        val avgPressure = (p1.pressure + p2.pressure) / 2f
                        nativePaint.strokeWidth = currentPenWidth * avgPressure
                        nativeCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, nativePaint)
                    }
                }
            }
            
            nativeCanvas.restore()
        }
    }
}

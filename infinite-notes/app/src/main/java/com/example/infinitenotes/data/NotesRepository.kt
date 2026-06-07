package com.example.infinitenotes.data

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class NotesRepository(private val context: Context) {
    private val notesDir: File
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "InfiniteNotes"
            )
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun saveNote(fileName: String, strokes: List<Stroke>) = withContext(Dispatchers.IO) {
        try {
            val jsonFile = File(notesDir, "$fileName.json")
            val jsonString = Json.encodeToString(strokes)
            jsonFile.writeText(jsonString)

            exportToPdf(fileName, strokes)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to save note", e)
        }
    }

    suspend fun loadNote(fileName: String): List<Stroke> = withContext(Dispatchers.IO) {
        try {
            val jsonFile = File(notesDir, "$fileName.json")
            if (jsonFile.exists()) {
                val jsonString = jsonFile.readText()
                Json.decodeFromString<List<Stroke>>(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to load note", e)
            emptyList()
        }
    }

    suspend fun listNotes(): List<String> = withContext(Dispatchers.IO) {
        val files = notesDir.listFiles { _, name -> name.endsWith(".json") }
        files?.map { it.nameWithoutExtension } ?: emptyList()
    }

    private fun exportToPdf(fileName: String, strokes: List<Stroke>) {
        if (strokes.isEmpty()) return

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { stroke ->
            stroke.points.forEach { point ->
                minX = min(minX, point.x)
                minY = min(minY, point.y)
                maxX = max(maxX, point.x)
                maxY = max(maxY, point.y)
            }
        }

        // Add padding
        minX -= 50f
        minY -= 50f
        maxX += 50f
        maxY += 50f

        val width = (maxX - minX).toInt().coerceAtLeast(100)
        val height = (maxY - minY).toInt().coerceAtLeast(100)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        canvas.translate(-minX, -minY)

        val paint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }

        strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            paint.color = stroke.color.toInt()
            paint.strokeWidth = stroke.width

            val path = android.graphics.Path()
            val start = stroke.points.first()
            path.moveTo(start.x, start.y)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x, stroke.points[i].y)
            }
            canvas.drawPath(path, paint)
        }

        document.finishPage(page)

        val pdfFile = File(notesDir, "$fileName.pdf")
        try {
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to write PDF", e)
        } finally {
            document.close()
        }
    }
}

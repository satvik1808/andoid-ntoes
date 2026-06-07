package com.example.infinitenotes.ui.main

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinitenotes.data.NotesRepository
import com.example.infinitenotes.data.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotesRepository(application)
    private var currentNoteName: String = ""

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    private val _currentPenColor = MutableStateFlow(Color.Black)
    val currentPenColor: StateFlow<Color> = _currentPenColor.asStateFlow()

    private val _currentPenWidth = MutableStateFlow(5f)
    val currentPenWidth: StateFlow<Float> = _currentPenWidth.asStateFlow()

    fun initialize(fileName: String) {
        if (currentNoteName == fileName) return
        currentNoteName = fileName
        loadNote()
    }

    fun updateStrokes(newStrokes: List<Stroke>) {
        _strokes.value = newStrokes
    }

    fun setPenColor(color: Color) {
        _currentPenColor.value = color
    }

    fun setPenWidth(width: Float) {
        _currentPenWidth.value = width
    }

    fun undo() {
        val current = _strokes.value
        if (current.isNotEmpty()) {
            _strokes.value = current.dropLast(1)
        }
    }

    fun clear() {
        _strokes.value = emptyList()
    }

    fun saveNote() {
        if (currentNoteName.isEmpty()) return
        viewModelScope.launch {
            repository.saveNote(currentNoteName, _strokes.value)
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            val loadedStrokes = repository.loadNote(currentNoteName)
            _strokes.value = loadedStrokes
        }
    }
}

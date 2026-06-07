package com.example.infinitenotes.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.infinitenotes.data.NotesRepository
import com.example.infinitenotes.data.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotesRepository(application)
    private val currentNoteName = "MyInfiniteNote"

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

    init {
        loadNote()
    }

    fun updateStrokes(newStrokes: List<Stroke>) {
        _strokes.value = newStrokes
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

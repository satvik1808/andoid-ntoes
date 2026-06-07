package com.example.infinitenotes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object FileManager : NavKey
@Serializable data class NoteEditor(val fileName: String) : NavKey

package com.example.infinitenotes.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("infinite_notes_prefs", Context.MODE_PRIVATE)
    
    private val _allowFingerDrawing = MutableStateFlow(prefs.getBoolean("allow_finger_drawing", true))
    val allowFingerDrawing: StateFlow<Boolean> = _allowFingerDrawing.asStateFlow()

    fun setAllowFingerDrawing(allow: Boolean) {
        prefs.edit().putBoolean("allow_finger_drawing", allow).apply()
        _allowFingerDrawing.value = allow
    }
}

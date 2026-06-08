package com.example.infinitenotes.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.infinitenotes.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val allowFingerDrawing: StateFlow<Boolean> = repository.allowFingerDrawing

    fun setAllowFingerDrawing(allow: Boolean) {
        repository.setAllowFingerDrawing(allow)
    }
}

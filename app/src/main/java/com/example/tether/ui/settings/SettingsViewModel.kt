package com.example.tether.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tether.ui.data.AppSettings
import com.example.tether.data.TetherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val settings: AppSettings = AppSettings()
)

class SettingsViewModel : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = TetherRepository.settings
        .map { SettingsUiState(settings = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun updateDebounce(seconds: Int) {
        TetherRepository.updateDebounce(seconds)
    }

    fun toggleBackgroundMonitoring(enabled: Boolean) {
        TetherRepository.toggleBackgroundMonitoring(enabled)
    }
}
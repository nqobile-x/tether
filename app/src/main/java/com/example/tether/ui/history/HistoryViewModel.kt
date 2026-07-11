package com.example.tether.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tether.ui.data.AlertEvent
import com.example.tether.data.TetherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val alerts: List<AlertEvent> = emptyList()
)

class HistoryViewModel : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = TetherRepository.alerts
        .map { HistoryUiState(alerts = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState()
        )

    fun toggleFalsePositive(id: String) {
        TetherRepository.toggleFalsePositive(id)
    }
}
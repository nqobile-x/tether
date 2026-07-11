package com.example.tether.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tether.data.TetherRepository
import com.example.tether.ui.data.PairedDevice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val pairedDevice: PairedDevice? = null,
    val isConnected: Boolean = false,
    val testAlertSent: Boolean = false
)

class HomeViewModel : ViewModel() {
    val uiState: StateFlow<HomeUiState> = TetherRepository.pairedDevice
        .map { device ->
            HomeUiState(
                pairedDevice = device,
                isConnected = device?.isConnected == true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    fun sendTestAlert() {
        TetherRepository.sendTestAlert()
    }
}
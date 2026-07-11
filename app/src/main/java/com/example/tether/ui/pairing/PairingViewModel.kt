package com.example.tether.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tether.data.TetherRepository
import com.example.tether.ui.data.PairedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PairingUiState(
    val isScanning: Boolean = false,
    val devices: List<PairedDevice> = emptyList()
)

class PairingViewModel : ViewModel() {
    private val _isScanning = MutableStateFlow(false)

    val uiState: StateFlow<PairingUiState> = combine(
        _isScanning,
        TetherRepository.availableDevices
    ) { scanning, devices ->
        PairingUiState(isScanning = scanning, devices = devices)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PairingUiState()
    )

    init {
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _isScanning.value = true
            TetherRepository.scanForDevices()
            _isScanning.value = false
        }
    }

    fun pairDevice(device: PairedDevice) {
        TetherRepository.pairWithDevice(device)
    }
}
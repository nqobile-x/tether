package com.example.tether.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tether.ui.data.Contact
import com.example.tether.data.TetherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val isLoading: Boolean = false
)

class ContactsViewModel : ViewModel() {
    val uiState: StateFlow<ContactsUiState> = TetherRepository.contacts
        .map { ContactsUiState(contacts = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContactsUiState(isLoading = true)
        )

    fun addContact(name: String, phone: String) {
        if (name.isNotBlank() && phone.isNotBlank()) {
            TetherRepository.addContact(name, phone)
        }
    }

    fun deleteContact(id: String) {
        TetherRepository.deleteContact(id)
    }

    fun setPrimary(id: String) {
        TetherRepository.setPrimaryContact(id)
    }
}
package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.LibraryRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(val games: List<Game>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

class LibraryViewModel(
    private val repository: LibraryRepositoryImpl = LibraryRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            repository.getUserLibrary()
                .onSuccess { games ->
                    _uiState.value = LibraryUiState.Success(games)
                }
                .onFailure { error ->
                    _uiState.value = LibraryUiState.Error(
                        error.message ?: "Error al cargar la biblioteca"
                    )
                }
        }
    }
}

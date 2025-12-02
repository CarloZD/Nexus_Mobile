package com.tecsup.nexusmobile.presentation.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.GameRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val games: List<Game>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel : ViewModel() {
    private val repository = GameRepositoryImpl()

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState

    init {
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading
            repository.getAllGames()
                .onSuccess { games ->
                    _uiState.value = CatalogUiState.Success(games)
                }
                .onFailure { error ->
                    _uiState.value = CatalogUiState.Error(
                        error.message ?: "Error desconocido"
                    )
                }
        }
    }
}
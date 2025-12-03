package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.GameRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.repository.GameRepository
import com.tecsup.nexusmobile.domain.usecase.GetGameByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GameDetailUiState {
    object Loading : GameDetailUiState()
    data class Success(val game: Game) : GameDetailUiState()
    data class Error(val message: String) : GameDetailUiState()
}

class GameDetailViewModel(
    private val getGameByIdUseCase: GetGameByIdUseCase = GetGameByIdUseCase(GameRepositoryImpl())
) : ViewModel() {
    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            _uiState.value = GameDetailUiState.Loading
            getGameByIdUseCase(gameId)
                .onSuccess { game ->
                    if (game != null) {
                        _uiState.value = GameDetailUiState.Success(game)
                    } else {
                        _uiState.value = GameDetailUiState.Error("Juego no encontrado")
                    }
                }
                .onFailure { error ->
                    _uiState.value = GameDetailUiState.Error(
                        error.message ?: "Error al cargar el juego"
                    )
                }
        }
    }
}

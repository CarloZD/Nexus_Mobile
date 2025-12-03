package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.GameRepositoryImpl
import com.tecsup.nexusmobile.data.repository.LibraryRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.repository.GameRepository
import com.tecsup.nexusmobile.domain.usecase.GetGameByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GameDetailUiState {
    object Loading : GameDetailUiState()
    data class Success(val game: Game, val isInLibrary: Boolean = false) : GameDetailUiState()
    data class Error(val message: String) : GameDetailUiState()
}

class GameDetailViewModel(
    private val getGameByIdUseCase: GetGameByIdUseCase = GetGameByIdUseCase(GameRepositoryImpl()),
    private val libraryRepository: LibraryRepositoryImpl = LibraryRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState

    fun loadGame(gameId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = GameDetailUiState.Loading
                getGameByIdUseCase(gameId)
                    .onSuccess { game ->
                        if (game != null) {
                            // Verificar si el juego está en la biblioteca de forma segura
                            val isInLibrary = try {
                                libraryRepository.isGameInLibrary(gameId)
                                    .getOrElse { false }
                            } catch (e: Exception) {
                                // Si hay error al verificar biblioteca, asumir que no está
                                false
                            }
                            _uiState.value = GameDetailUiState.Success(game, isInLibrary)
                        } else {
                            _uiState.value = GameDetailUiState.Error("Juego no encontrado")
                        }
                    }
                    .onFailure { error ->
                        _uiState.value = GameDetailUiState.Error(
                            error.message ?: "Error al cargar el juego"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = GameDetailUiState.Error(
                    e.message ?: "Error inesperado al cargar el juego"
                )
            }
        }
    }
}

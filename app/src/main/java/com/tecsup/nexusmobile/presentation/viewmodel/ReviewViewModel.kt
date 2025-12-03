package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.ReviewRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ReviewUiState {
    object Loading : ReviewUiState()
    data class Success(val reviews: List<Review>) : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}

sealed class AddReviewUiState {
    object Idle : AddReviewUiState()
    object Loading : AddReviewUiState()
    object Success : AddReviewUiState()
    data class Error(val message: String) : AddReviewUiState()
}

class ReviewViewModel(
    private val repository: ReviewRepositoryImpl = ReviewRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState

    private val _addReviewState = MutableStateFlow<AddReviewUiState>(AddReviewUiState.Idle)
    val addReviewState: StateFlow<AddReviewUiState> = _addReviewState

    fun loadReviews(gameId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ReviewUiState.Loading
                if (gameId.isEmpty()) {
                    _uiState.value = ReviewUiState.Success(emptyList())
                    return@launch
                }
                repository.getReviewsByGameId(gameId)
                    .onSuccess { reviews ->
                        _uiState.value = ReviewUiState.Success(reviews)
                    }
                    .onFailure { error ->
                        // En caso de error, mostrar lista vacía en lugar de error para no bloquear la pantalla
                        _uiState.value = ReviewUiState.Success(emptyList())
                        // Log del error para debug
                        android.util.Log.e("ReviewViewModel", "Error al cargar reseñas: ${error.message}", error)
                    }
            } catch (e: Exception) {
                // En caso de excepción, mostrar lista vacía en lugar de error
                _uiState.value = ReviewUiState.Success(emptyList())
                android.util.Log.e("ReviewViewModel", "Excepción al cargar reseñas: ${e.message}", e)
            }
        }
    }

    fun addReview(gameId: String, comment: String, rating: Int) {
        viewModelScope.launch {
            try {
                _addReviewState.value = AddReviewUiState.Loading
                val review = Review(
                    gameId = gameId,
                    comment = comment,
                    rating = rating
                )
                repository.addReview(review)
                    .onSuccess {
                        _addReviewState.value = AddReviewUiState.Success
                        // Recargar reseñas después de agregar una nueva
                        loadReviews(gameId)
                    }
                    .onFailure { error ->
                        _addReviewState.value = AddReviewUiState.Error(
                            error.message ?: "Error al agregar la reseña"
                        )
                    }
            } catch (e: Exception) {
                _addReviewState.value = AddReviewUiState.Error(
                    e.message ?: "Error inesperado al agregar la reseña"
                )
            }
        }
    }

    fun resetAddReviewState() {
        _addReviewState.value = AddReviewUiState.Idle
    }
}


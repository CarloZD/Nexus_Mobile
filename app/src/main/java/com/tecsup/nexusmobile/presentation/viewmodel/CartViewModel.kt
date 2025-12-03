package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tecsup.nexusmobile.data.repository.CartRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Cart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CartUiState {
    object Loading : CartUiState()
    data class Success(val cart: Cart) : CartUiState()
    data class Error(val message: String) : CartUiState()
}

class CartViewModel(
    private val repository: CartRepositoryImpl = CartRepositoryImpl()
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState

    init {
        loadCart()
    }

    fun loadCart() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = CartUiState.Error("Usuario no autenticado")
                return@launch
            }

            _uiState.value = CartUiState.Loading
            repository.getCart(userId)
                .onSuccess { cart ->
                    _uiState.value = CartUiState.Success(cart)
                }
                .onFailure { error ->
                    _uiState.value = CartUiState.Error(
                        error.message ?: "Error al cargar el carrito"
                    )
                }
        }
    }

    fun addToCart(gameId: String, gameTitle: String, gameImage: String?, price: Double) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.addToCart(userId, gameId, gameTitle, gameImage, price)
                .onSuccess {
                    loadCart() // Recargar carrito
                }
                .onFailure {
                    // Error silencioso o mostrar mensaje
                }
        }
    }

    fun removeFromCart(gameId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.removeFromCart(userId, gameId)
                .onSuccess {
                    loadCart()
                }
                .onFailure {
                    // Error silencioso
                }
        }
    }

    fun updateQuantity(gameId: String, quantity: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.updateQuantity(userId, gameId, quantity)
                .onSuccess {
                    loadCart()
                }
                .onFailure {
                    // Error silencioso
                }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.clearCart(userId)
                .onSuccess {
                    loadCart()
                }
                .onFailure {
                    // Error silencioso
                }
        }
    }
}
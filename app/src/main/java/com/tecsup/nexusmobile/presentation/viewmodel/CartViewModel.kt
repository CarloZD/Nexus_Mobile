package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState

    private var cartListener: ListenerRegistration? = null

    init {
        setupRealtimeCartListener()
    }

    // Escuchar cambios en tiempo real del carrito
    private fun setupRealtimeCartListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = CartUiState.Error("Usuario no autenticado")
            return
        }

        _uiState.value = CartUiState.Loading

        // Listener en tiempo real de Firestore
        cartListener = firestore.collection("carts")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = CartUiState.Error(
                        error.message ?: "Error al cargar el carrito"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val cart = snapshot.toObject(Cart::class.java)?.copy(userId = userId)
                        ?: Cart(userId = userId, items = emptyList(), total = 0.0)
                    _uiState.value = CartUiState.Success(cart)
                } else {
                    // Si no existe el carrito, crear uno vacío
                    val emptyCart = Cart(userId = userId, items = emptyList(), total = 0.0)
                    _uiState.value = CartUiState.Success(emptyCart)
                }
            }
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
                    // No necesitamos recargar manualmente, el listener se encarga
                }
                .onFailure { error ->
                    _uiState.value = CartUiState.Error(
                        error.message ?: "Error al agregar al carrito"
                    )
                }
        }
    }

    fun removeFromCart(gameId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.removeFromCart(userId, gameId)
                .onSuccess {
                    // El listener actualizará automáticamente
                }
                .onFailure { error ->
                    _uiState.value = CartUiState.Error(
                        error.message ?: "Error al eliminar del carrito"
                    )
                }
        }
    }

    fun updateQuantity(gameId: String, quantity: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.updateQuantity(userId, gameId, quantity)
                .onSuccess {
                    // El listener actualizará automáticamente
                }
                .onFailure { error ->
                    _uiState.value = CartUiState.Error(
                        error.message ?: "Error al actualizar cantidad"
                    )
                }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            repository.clearCart(userId)
                .onSuccess {
                    // El listener actualizará automáticamente
                }
                .onFailure { error ->
                    _uiState.value = CartUiState.Error(
                        error.message ?: "Error al limpiar carrito"
                    )
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Limpiar el listener cuando el ViewModel se destruye
        cartListener?.remove()
    }
}
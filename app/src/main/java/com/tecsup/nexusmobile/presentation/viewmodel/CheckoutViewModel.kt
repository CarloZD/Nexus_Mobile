package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.data.repository.CartRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Cart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CheckoutUiState {
    object Idle : CheckoutUiState()
    object Loading : CheckoutUiState()
    object Success : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "COMPLETED",
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderItem(
    val gameId: String = "",
    val gameTitle: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)

class CheckoutViewModel(
    private val cartRepository: CartRepositoryImpl = CartRepositoryImpl()
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val uiState: StateFlow<CheckoutUiState> = _uiState

    fun processPayment(
        cart: Cart,
        paymentMethod: String,
        paymentDetails: Map<String, String>
    ) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = CheckoutUiState.Error("Usuario no autenticado")
                return@launch
            }

            _uiState.value = CheckoutUiState.Loading

            try {
                // Simular procesamiento de pago (2 segundos)
                delay(2000)

                // Validar metodo de pago
                val isPaymentValid = when (paymentMethod) {
                    "CREDIT_CARD" -> validateCreditCard(paymentDetails)
                    "YAPE" -> validateYape(paymentDetails)
                    else -> false
                }

                if (!isPaymentValid) {
                    _uiState.value = CheckoutUiState.Error("Datos de pago inválidos")
                    return@launch
                }

                // Crear orden
                val orderId = createOrder(userId, cart, paymentMethod)

                // Agregar juegos a la biblioteca del usuario (con orderId actualizado)
                addGamesToLibrary(userId, cart, orderId)

                // Limpiar carrito
                cartRepository.clearCart(userId)

                _uiState.value = CheckoutUiState.Success
            } catch (e: Exception) {
                _uiState.value = CheckoutUiState.Error(
                    e.message ?: "Error al procesar el pago"
                )
            }
        }
    }

    private fun validateCreditCard(details: Map<String, String>): Boolean {
        val cardNumber = details["cardNumber"] ?: return false
        val cvv = details["cvv"] ?: return false
        val expiryDate = details["expiryDate"] ?: return false
        val cardHolder = details["cardHolder"] ?: return false

        return cardNumber.length >= 13 &&
                cvv.length == 3 &&
                expiryDate.length >= 4 &&
                cardHolder.isNotBlank()
    }

    private fun validateYape(details: Map<String, String>): Boolean {
        val phoneNumber = details["phoneNumber"] ?: return false
        return phoneNumber.length == 9
    }

    private suspend fun createOrder(
        userId: String,
        cart: Cart,
        paymentMethod: String
    ): String {
        val orderId = firestore.collection("orders").document().id

        val orderItems = cart.items.map { cartItem ->
            OrderItem(
                gameId = cartItem.gameId,
                gameTitle = cartItem.gameTitle,
                price = cartItem.price,
                quantity = cartItem.quantity
            )
        }

        val order = Order(
            orderId = orderId,
            userId = userId,
            items = orderItems,
            total = cart.total,
            paymentMethod = paymentMethod,
            status = "COMPLETED",
            createdAt = System.currentTimeMillis()
        )

        firestore.collection("orders")
            .document(orderId)
            .set(order)
            .await()

        return orderId
    }

    private suspend fun addGamesToLibrary(userId: String, cart: Cart, orderId: String) {
        try {
            val userLibraryRef = firestore.collection("users")
                .document(userId)
                .collection("library")

            cart.items.forEach { cartItem ->
                val libraryItem = hashMapOf(
                    "gameId" to cartItem.gameId,
                    "orderId" to orderId,
                    "purchasePrice" to cartItem.price,
                    "playTimeMinutes" to 0,
                    "lastPlayed" to null,
                    "isInstalled" to false,
                    "acquiredAt" to System.currentTimeMillis()
                )

                // Usar set() para crear o actualizar el documento
                userLibraryRef.document(cartItem.gameId).set(libraryItem).await()
            }
        } catch (e: Exception) {
            // Log error pero no fallar el pago
            e.printStackTrace()
        }
    }

    fun resetState() {
        _uiState.value = CheckoutUiState.Idle
    }
}
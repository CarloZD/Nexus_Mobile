package com.tecsup.nexusmobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.Cart
import com.tecsup.nexusmobile.domain.model.CartItem
import com.tecsup.nexusmobile.domain.repository.CartRepository
import kotlinx.coroutines.tasks.await

class CartRepositoryImpl : CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val cartsCollection = db.collection("carts")

    override suspend fun getCart(userId: String): Result<Cart> {
        return try {
            val doc = cartsCollection.document(userId).get().await()

            if (!doc.exists()) {
                // Si no existe el carrito, crear uno vacío
                val emptyCart = Cart(userId = userId, items = emptyList(), total = 0.0)
                cartsCollection.document(userId).set(emptyCart).await()
                return Result.success(emptyCart)
            }

            val cart = doc.toObject(Cart::class.java)?.copy(userId = userId)
                ?: Cart(userId = userId, items = emptyList(), total = 0.0)

            Result.success(cart)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addToCart(
        userId: String,
        gameId: String,
        gameTitle: String,
        gameImage: String?,
        price: Double
    ): Result<Unit> {
        return try {
            val currentCart = getCart(userId).getOrThrow()

            // Verificar si el juego ya está en el carrito
            val existingItemIndex = currentCart.items.indexOfFirst { it.gameId == gameId }

            val updatedItems = if (existingItemIndex != -1) {
                // Si ya existe, incrementar cantidad
                currentCart.items.toMutableList().apply {
                    val item = this[existingItemIndex]
                    this[existingItemIndex] = item.copy(
                        quantity = item.quantity + 1,
                        subtotal = (item.quantity + 1) * item.price
                    )
                }
            } else {
                // Si no existe, agregar nuevo item
                currentCart.items + CartItem(
                    gameId = gameId,
                    gameTitle = gameTitle,
                    gameImage = gameImage,
                    quantity = 1,
                    price = price,
                    subtotal = price
                )
            }

            val newTotal = updatedItems.sumOf { it.subtotal }
            val updatedCart = currentCart.copy(items = updatedItems, total = newTotal)

            cartsCollection.document(userId).set(updatedCart).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromCart(userId: String, gameId: String): Result<Unit> {
        return try {
            val currentCart = getCart(userId).getOrThrow()
            val updatedItems = currentCart.items.filter { it.gameId != gameId }
            val newTotal = updatedItems.sumOf { it.subtotal }
            val updatedCart = currentCart.copy(items = updatedItems, total = newTotal)

            cartsCollection.document(userId).set(updatedCart).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearCart(userId: String): Result<Unit> {
        return try {
            val emptyCart = Cart(userId = userId, items = emptyList(), total = 0.0)
            cartsCollection.document(userId).set(emptyCart).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateQuantity(userId: String, gameId: String, quantity: Int): Result<Unit> {
        return try {
            if (quantity <= 0) {
                return removeFromCart(userId, gameId)
            }

            val currentCart = getCart(userId).getOrThrow()
            val updatedItems = currentCart.items.map { item ->
                if (item.gameId == gameId) {
                    item.copy(quantity = quantity, subtotal = quantity * item.price)
                } else {
                    item
                }
            }

            val newTotal = updatedItems.sumOf { it.subtotal }
            val updatedCart = currentCart.copy(items = updatedItems, total = newTotal)

            cartsCollection.document(userId).set(updatedCart).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
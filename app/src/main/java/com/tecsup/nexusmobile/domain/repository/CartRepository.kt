package com.tecsup.nexusmobile.domain.repository

import com.tecsup.nexusmobile.domain.model.Cart
import com.tecsup.nexusmobile.domain.model.CartItem

interface CartRepository {
    suspend fun getCart(userId: String): Result<Cart>
    suspend fun addToCart(userId: String, gameId: String, gameTitle: String, gameImage: String?, price: Double): Result<Unit>
    suspend fun removeFromCart(userId: String, gameId: String): Result<Unit>
    suspend fun clearCart(userId: String): Result<Unit>
    suspend fun updateQuantity(userId: String, gameId: String, quantity: Int): Result<Unit>
}
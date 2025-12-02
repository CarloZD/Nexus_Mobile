package com.tecsup.nexus_mobile.domain.model

data class CartItem(
    val gameId: String = "",
    val gameTitle: String = "",
    val gameImage: String? = null,
    val quantity: Int = 1,
    val price: Double = 0.0,
    val subtotal: Double = 0.0
)

data class Cart(
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0
)
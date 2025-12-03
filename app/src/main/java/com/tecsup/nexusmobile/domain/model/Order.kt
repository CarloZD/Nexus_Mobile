package com.tecsup.nexusmobile.domain.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, completed, cancelled
    val paymentMethod: String = "card",
    val paymentDetails: PaymentDetails? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class OrderItem(
    val gameId: String = "",
    val gameTitle: String = "",
    val gameImage: String? = null,
    val quantity: Int = 1,
    val priceAtPurchase: Double = 0.0
)

data class PaymentDetails(
    val cardLastFour: String = "",
    val cardType: String = "",
    val transactionId: String = ""
)
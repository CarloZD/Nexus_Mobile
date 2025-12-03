package com.tecsup.nexusmobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.Order
import com.tecsup.nexusmobile.domain.model.OrderItem
import com.tecsup.nexusmobile.domain.model.PaymentDetails
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface OrderRepository {
    suspend fun createOrder(
        items: List<OrderItem>,
        totalAmount: Double,
        paymentDetails: PaymentDetails
    ): Result<Order>

    suspend fun getUserOrders(): Result<List<Order>>
    suspend fun getOrderById(orderId: String): Result<Order?>
}

class OrderRepositoryImpl : OrderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val ordersCollection = firestore.collection("orders")

    override suspend fun createOrder(
        items: List<OrderItem>,
        totalAmount: Double,
        paymentDetails: PaymentDetails
    ): Result<Order> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Generar número de orden único
            val orderNumber = "ORD-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8).uppercase()}"

            val order = Order(
                orderNumber = orderNumber,
                userId = currentUser.uid,
                userEmail = currentUser.email ?: "",
                items = items,
                totalAmount = totalAmount,
                status = "completed", // Simulamos que se completó
                paymentMethod = "card",
                paymentDetails = paymentDetails,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Guardar en Firestore
            val docRef = ordersCollection.add(order).await()
            val savedOrder = order.copy(id = docRef.id)

            // Agregar juegos a la biblioteca del usuario
            addGamesToUserLibrary(currentUser.uid, items, docRef.id)

            Result.success(savedOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserOrders(): Result<List<Order>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = ordersCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }

            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderById(orderId: String): Result<Order?> {
        return try {
            val doc = ordersCollection.document(orderId).get().await()
            val order = doc.toObject(Order::class.java)?.copy(id = doc.id)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun addGamesToUserLibrary(
        userId: String,
        items: List<OrderItem>,
        orderId: String
    ) {
        try {
            val userLibraryRef = firestore.collection("users")
                .document(userId)
                .collection("library")

            items.forEach { item ->
                val libraryItem = hashMapOf(
                    "gameId" to item.gameId,
                    "orderId" to orderId,
                    "purchasePrice" to item.priceAtPurchase,
                    "playTimeMinutes" to 0,
                    "lastPlayed" to null,
                    "isInstalled" to false,
                    "acquiredAt" to System.currentTimeMillis()
                )

                userLibraryRef.document(item.gameId).set(libraryItem).await()
            }
        } catch (e: Exception) {
            // Log error pero no fallar la orden
            e.printStackTrace()
        }
    }
}
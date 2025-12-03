package com.tecsup.nexusmobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.Review
import com.tecsup.nexusmobile.domain.repository.ReviewRepository
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ReviewRepositoryImpl : ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val reviewsCollection = firestore.collection("reviews")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override suspend fun getReviewsByGameId(gameId: String): Result<List<Review>> {
        return try {
            if (gameId.isEmpty()) {
                return Result.success(emptyList())
            }
            // Primero filtramos por gameId, luego ordenamos en memoria para evitar necesidad de índice
            val snapshot = try {
                reviewsCollection
                    .whereEqualTo("gameId", gameId)
                    .get()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error al obtener snapshot: ${e.message}", e)
                return Result.success(emptyList())
            }

            val reviewsWithTimestamp = snapshot.documents.mapNotNull { doc ->
                try {
                    // Mapeo manual para evitar problemas con tipos o campos faltantes
                    val docGameId = doc.getString("gameId") ?: ""
                    val userId = doc.getString("userId") ?: ""
                    val userName = doc.getString("userName") ?: "Usuario"
                    
                    // Manejar rating que puede ser Long o Int
                    val ratingValue = when {
                        doc.get("rating") is Long -> (doc.get("rating") as Long).toInt()
                        doc.get("rating") is Number -> (doc.get("rating") as Number).toInt()
                        else -> 0
                    }
                    
                    val comment = doc.getString("comment") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val dateString = doc.getString("date") ?: ""
                    
                    val formattedDate = if (dateString.isEmpty()) {
                        dateFormat.format(Date(timestamp))
                    } else {
                        dateString
                    }
                    
                    val review = Review(
                        id = doc.id,
                        gameId = docGameId,
                        userId = userId,
                        userName = userName,
                        rating = ratingValue,
                        comment = comment,
                        date = formattedDate
                    )
                    
                    Pair(review, timestamp)
                } catch (e: Exception) {
                    android.util.Log.e("ReviewRepository", "Error al mapear documento ${doc.id}: ${e.message}", e)
                    null
                }
            }
            // Ordenar por timestamp descendente (más recientes primero)
            val sortedReviews = reviewsWithTimestamp
                .sortedByDescending { it.second }
                .map { it.first }
            Result.success(sortedReviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addReview(review: Review): Result<Review> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener nombre de usuario desde Firestore
            val userDoc = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val username = userDoc.getString("username") ?: userDoc.getString("fullName") ?: "Usuario"

            val currentTime = System.currentTimeMillis()
            val reviewToSave = review.copy(
                userId = currentUser.uid,
                userName = username,
                date = dateFormat.format(Date(currentTime))
            )

            // Guardar con timestamp para ordenamiento
            val reviewMap = hashMapOf(
                "gameId" to reviewToSave.gameId,
                "userId" to reviewToSave.userId,
                "userName" to reviewToSave.userName,
                "rating" to reviewToSave.rating,
                "comment" to reviewToSave.comment,
                "date" to reviewToSave.date,
                "timestamp" to currentTime
            )

            val docRef = reviewsCollection.add(reviewMap).await()
            val savedReview = reviewToSave.copy(id = docRef.id)

            Result.success(savedReview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


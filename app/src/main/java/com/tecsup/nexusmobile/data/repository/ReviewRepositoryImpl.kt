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

            android.util.Log.d("ReviewRepository", "Buscando reviews para gameId: $gameId")

            val snapshot = try {
                reviewsCollection
                    .whereEqualTo("gameId", gameId)
                    .get()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error al obtener snapshot: ${e.message}", e)
                return Result.success(emptyList())
            }

            android.util.Log.d("ReviewRepository", "Reviews encontradas: ${snapshot.documents.size}")

            val reviewsWithTimestamp = snapshot.documents.mapNotNull { doc ->
                try {
                    // Obtener valores con manejo seguro de nulls y tipos
                    val docGameId = doc.getString("gameId") ?: ""
                    val userId = doc.getString("userId") ?: ""
                    val userName = doc.getString("userName") ?: "Usuario"

                    // Manejar rating que puede ser Long, Int, o Double
                    val ratingValue = try {
                        when (val rating = doc.get("rating")) {
                            is Long -> rating.toInt()
                            is Int -> rating
                            is Double -> rating.toInt()
                            is Number -> rating.toInt()
                            else -> {
                                android.util.Log.w("ReviewRepository", "Rating desconocido para doc ${doc.id}: $rating")
                                0
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ReviewRepository", "Error al convertir rating: ${e.message}")
                        0
                    }

                    val comment = doc.getString("comment") ?: ""

                    // Obtener timestamp - puede venir como Long o como Timestamp
                    val timestamp = try {
                        when (val ts = doc.get("timestamp")) {
                            is Long -> ts
                            is Number -> ts.toLong()
                            else -> {
                                // Si no hay timestamp, intentar obtener de createdAt
                                when (val createdAt = doc.get("createdAt")) {
                                    is com.google.firebase.Timestamp -> createdAt.toDate().time
                                    is Long -> createdAt
                                    is Number -> createdAt.toLong()
                                    else -> {
                                        android.util.Log.w("ReviewRepository", "Timestamp no encontrado para doc ${doc.id}")
                                        System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ReviewRepository", "Error al obtener timestamp: ${e.message}")
                        System.currentTimeMillis()
                    }

                    // Obtener fecha formateada
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

                    android.util.Log.d("ReviewRepository", "Review mapeada: ${review.userName} - Rating: ${review.rating}")

                    Pair(review, timestamp)
                } catch (e: Exception) {
                    android.util.Log.e("ReviewRepository", "Error al mapear documento ${doc.id}: ${e.message}", e)
                    android.util.Log.e("ReviewRepository", "Datos del documento: ${doc.data}")
                    null
                }
            }

            // Ordenar por timestamp descendente (más recientes primero)
            val sortedReviews = reviewsWithTimestamp
                .sortedByDescending { it.second }
                .map { it.first }

            android.util.Log.d("ReviewRepository", "Reviews finales ordenadas: ${sortedReviews.size}")
            Result.success(sortedReviews)
        } catch (e: Exception) {
            android.util.Log.e("ReviewRepository", "Error general: ${e.message}", e)
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

            val username = userDoc.getString("username")?.takeIf { it.isNotEmpty() }
                ?: userDoc.getString("fullName")?.takeIf { it.isNotEmpty() }
                ?: "Usuario"

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
                "timestamp" to currentTime,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            android.util.Log.d("ReviewRepository", "Guardando review: $reviewMap")

            val docRef = reviewsCollection.add(reviewMap).await()
            val savedReview = reviewToSave.copy(id = docRef.id)

            android.util.Log.d("ReviewRepository", "Review guardada exitosamente con ID: ${docRef.id}")

            Result.success(savedReview)
        } catch (e: Exception) {
            android.util.Log.e("ReviewRepository", "Error al guardar review: ${e.message}", e)
            Result.failure(e)
        }
    }
}
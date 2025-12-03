package com.tecsup.nexusmobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.repository.LibraryRepository
import kotlinx.coroutines.tasks.await

class LibraryRepositoryImpl : LibraryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gamesCollection = firestore.collection("games")

    override suspend fun getUserLibrary(): Result<List<Game>> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // Obtener los gameIds de la biblioteca del usuario
            val librarySnapshot = firestore.collection("users")
                .document(currentUser.uid)
                .collection("library")
                .get()
                .await()

            val gameIds = librarySnapshot.documents.mapNotNull { doc ->
                doc.getString("gameId")
            }

            if (gameIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Obtener los detalles de los juegos
            val games = mutableListOf<Game>()
            gameIds.forEach { gameId ->
                val gameDoc = gamesCollection.document(gameId).get().await()
                val game = gameDoc.toObject(Game::class.java)?.copy(id = gameDoc.id)
                if (game != null) {
                    games.add(game)
                }
            }

            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isGameInLibrary(gameId: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val libraryDoc = firestore.collection("users")
                .document(currentUser.uid)
                .collection("library")
                .document(gameId)
                .get()
                .await()

            Result.success(libraryDoc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.tecsup.nexusmobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.repository.GameRepository
import kotlinx.coroutines.tasks.await

class GameRepositoryImpl : GameRepository {
    private val db = FirebaseFirestore.getInstance()
    private val gamesCollection = db.collection("games")

    override suspend fun getAllGames(): Result<List<Game>> {
        return try {
            val snapshot = gamesCollection
                .whereEqualTo("active", true)
                .get()
                .await()

            val games = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Game::class.java)?.copy(id = doc.id)
            }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGameById(id: String): Result<Game?> {
        return try {
            val doc = gamesCollection.document(id).get().await()
            val game = doc.toObject(Game::class.java)?.copy(id = doc.id)
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchGames(query: String): Result<List<Game>> {
        return try {
            val snapshot = gamesCollection
                .whereEqualTo("active", true)
                .get()
                .await()

            val games = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Game::class.java)?.copy(id = doc.id)
            }.filter {
                it.title.contains(query, ignoreCase = true)
            }
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
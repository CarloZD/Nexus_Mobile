package com.tecsup.nexusmobile.domain.repository

import com.tecsup.nexusmobile.domain.model.Game

interface GameRepository {
    suspend fun getAllGames(): Result<List<Game>>
    suspend fun getGameById(id: String): Result<Game?>
    suspend fun searchGames(query: String): Result<List<Game>>
}
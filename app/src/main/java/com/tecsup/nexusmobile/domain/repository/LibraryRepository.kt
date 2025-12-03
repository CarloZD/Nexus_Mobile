package com.tecsup.nexusmobile.domain.repository

import com.tecsup.nexusmobile.domain.model.Game

interface LibraryRepository {
    suspend fun getUserLibrary(): Result<List<Game>>
    suspend fun isGameInLibrary(gameId: String): Result<Boolean>
}

package com.tecsup.nexusmobile.domain.usecase

import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.repository.GameRepository

class GetGameByIdUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(gameId: String): Result<Game?> {
        return repository.getGameById(gameId)
    }
}

package com.tecsup.nexusmobile.domain.usecase

import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.repository.CartRepository
import com.tecsup.nexusmobile.domain.repository.LibraryRepository

/**
 * Use Case para agregar juegos al carrito con validaciones
 */
class AddToCartUseCase(
    private val cartRepository: CartRepository,
    private val libraryRepository: LibraryRepository
) {

    suspend operator fun invoke(
        userId: String,
        game: Game
    ): Result<Unit> {
        // Validación 1: Verificar que el juego tenga stock
        if (game.stock <= 0 && !game.isFree) {
            return Result.failure(Exception("Este juego no tiene stock disponible"))
        }

        // Validación 2: Verificar que el juego esté activo
        if (!game.active) {
            return Result.failure(Exception("Este juego ya no está disponible"))
        }

        // Validación 3: Verificar si el usuario ya tiene el juego
        val isInLibrary = libraryRepository.isGameInLibrary(game.id)
            .getOrElse { false }

        if (isInLibrary) {
            return Result.failure(Exception("Ya tienes este juego en tu biblioteca"))
        }

        // Validación 4: Los juegos gratis no se agregan al carrito
        if (game.isFree) {
            return Result.failure(Exception("Los juegos gratis no se agregan al carrito"))
        }

        // Si pasa todas las validaciones, agregar al carrito
        return cartRepository.addToCart(
            userId = userId,
            gameId = game.id,
            gameTitle = game.title,
            gameImage = game.headerImage,
            price = game.price
        )
    }

    /**
     * Validar si un juego puede agregarse al carrito (sin agregarlo)
     */
    suspend fun canAddToCart(userId: String, game: Game): ValidationResult {
        if (game.stock <= 0 && !game.isFree) {
            return ValidationResult.Error("Sin stock disponible")
        }

        if (!game.active) {
            return ValidationResult.Error("Juego no disponible")
        }

        val isInLibrary = libraryRepository.isGameInLibrary(game.id)
            .getOrElse { false }

        if (isInLibrary) {
            return ValidationResult.Error("Ya lo tienes en tu biblioteca")
        }

        if (game.isFree) {
            return ValidationResult.Error("Los juegos gratis no van al carrito")
        }

        return ValidationResult.Success
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
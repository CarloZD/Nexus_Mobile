package com.tecsup.nexusmobile.domain.usecase

import com.tecsup.nexusmobile.domain.model.Game

/**
 * Use Case para filtrar y ordenar juegos
 * (SIN cache local - solo lógica de negocio)
 */
class FilterGamesUseCase {

    operator fun invoke(
        games: List<Game>,
        searchQuery: String? = null,
        category: String? = null,
        onlyFree: Boolean = false,
        onlyFeatured: Boolean = false,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        sortBy: SortOption = SortOption.TITLE_ASC
    ): List<Game> {
        var filteredGames = games

        // Aplicar búsqueda
        searchQuery?.let { query ->
            if (query.isNotBlank()) {
                filteredGames = filteredGames.filter { game ->
                    game.title.contains(query, ignoreCase = true) ||
                            game.description.contains(query, ignoreCase = true) ||
                            game.developer.contains(query, ignoreCase = true) ||
                            game.category.contains(query, ignoreCase = true)
                }
            }
        }

        // Filtrar por categoría
        category?.let { cat ->
            filteredGames = filteredGames.filter {
                it.category.equals(cat, ignoreCase = true)
            }
        }

        // Filtrar solo gratis
        if (onlyFree) {
            filteredGames = filteredGames.filter { it.isFree }
        }

        // Filtrar solo destacados
        if (onlyFeatured) {
            filteredGames = filteredGames.filter { it.featured }
        }

        // Filtrar por rango de precio
        minPrice?.let { min ->
            filteredGames = filteredGames.filter { it.price >= min }
        }
        maxPrice?.let { max ->
            filteredGames = filteredGames.filter { it.price <= max }
        }

        // Ordenar
        return when (sortBy) {
            SortOption.TITLE_ASC -> filteredGames.sortedBy { it.title }
            SortOption.TITLE_DESC -> filteredGames.sortedByDescending { it.title }
            SortOption.PRICE_ASC -> filteredGames.sortedBy { it.price }
            SortOption.PRICE_DESC -> filteredGames.sortedByDescending { it.price }
            SortOption.RATING_DESC -> filteredGames.sortedByDescending { it.rating }
            SortOption.RELEASE_DATE_DESC -> filteredGames.sortedByDescending { it.releaseDate }
            SortOption.FEATURED_FIRST -> filteredGames.sortedByDescending { it.featured }
        }
    }

    // Métodos de conveniencia
    fun searchGames(games: List<Game>, query: String): List<Game> {
        return invoke(games, searchQuery = query)
    }

    fun getFeatured(games: List<Game>): List<Game> {
        return invoke(games, onlyFeatured = true, sortBy = SortOption.RATING_DESC)
    }

    fun getFreeGames(games: List<Game>): List<Game> {
        return invoke(games, onlyFree = true)
    }

    fun getByCategory(games: List<Game>, category: String): List<Game> {
        return invoke(games, category = category)
    }
}

enum class SortOption {
    TITLE_ASC,
    TITLE_DESC,
    PRICE_ASC,
    PRICE_DESC,
    RATING_DESC,
    RELEASE_DATE_DESC,
    FEATURED_FIRST
}
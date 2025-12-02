package com.tecsup.nexusmobile.domain.model

data class Game(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val platform: String = "",
    val rating: Double = 0.0,
    val releaseDate: String = "",
    val developer: String = "",
    val publisher: String = "",
    val headerImage: String? = null,
    val backgroundImage: String? = null,
    val stock: Int = 0,
    val featured: Boolean = false,
    val active: Boolean = true,
    val isFree: Boolean = false,
    val shortDescription: String = "",
    val steamAppId: String = "",
    val screenshots: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val genres: List<String> = emptyList()
)
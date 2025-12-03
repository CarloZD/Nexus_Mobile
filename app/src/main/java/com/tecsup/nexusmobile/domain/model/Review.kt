package com.tecsup.nexusmobile.domain.model

data class Review(
    val id: String = "",
    val gameId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val date: String = ""
)

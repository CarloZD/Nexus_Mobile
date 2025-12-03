package com.tecsup.nexusmobile.domain.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val title: String = "",
    val content: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList() // Lista de IDs de usuarios que dieron like
)

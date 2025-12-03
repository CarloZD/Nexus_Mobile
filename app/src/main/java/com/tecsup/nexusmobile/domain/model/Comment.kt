package com.tecsup.nexusmobile.domain.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

